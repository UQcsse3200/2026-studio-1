package com.csse3200.game.components.pet;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Keeps a companion pet hovering above and behind its owner.
 *
 * <p>After three seconds without owner movement, the pet turns through all four directions once.
 * Two seconds after the turn, it flies to the other side of the owner and back. Each trip takes two
 * seconds and starts every four seconds while the owner stays still. Moving again resets the idle
 * sequence. Idle timing uses game time, so it pauses with the game.
 */
public class PetMovementComponent extends Component {
  private static final float HORIZONTAL_GAP = 0.25f;
  private static final float VERTICAL_GAP = 0.25f;
  private static final float FOLLOW_SHARPNESS = 6f;
  private static final float TELEPORT_DISTANCE = 6f;
  private static final float MOVEMENT_EPSILON = 0.001f;
  private static final float IDLE_SPIN_DELAY = 3f;
  private static final float SPIN_DIRECTION_DURATION = 0.3f;
  private static final String[] SPIN_DIRECTIONS = {"right", "back", "left", "front"};
  private static final float SPIN_DURATION = SPIN_DIRECTIONS.length * SPIN_DIRECTION_DURATION;
  private static final float POST_SPIN_DELAY = 2f;
  private static final float IDLE_FLIGHT_DURATION = 2f;
  private static final float IDLE_FLIGHT_INTERVAL = 4f;
  private static final float IDLE_FLIGHT_HEIGHT = 0.35f;
  private static final double FIRST_FLIGHT_TIME =
      (double) IDLE_SPIN_DELAY + SPIN_DURATION + POST_SPIN_DELAY;

  private final GameTime timeSource;
  private final Vector2 previousOwnerPosition = new Vector2();
  private Entity owner;
  private float facingDirection = 1f;
  private double idleTime;
  private AnimationRenderComponent animator;
  private final String animationPrefix;

  /** Creates pet movement using the game's time source. */
  public PetMovementComponent(String animationPrefix) {
    this(ServiceLocator.getTimeSource(), animationPrefix);
  }

  PetMovementComponent(GameTime timeSource, String animationPrefix) {
    if (timeSource == null) {
      throw new IllegalArgumentException("Pet movement requires a time source");
    }
    if (animationPrefix == null || animationPrefix.isBlank()) {
      throw new IllegalArgumentException("Animation prefix cannot be null or blank");
    }

    this.timeSource = timeSource;
    this.animationPrefix = animationPrefix;
  }

  @Override
  public void create() {
    PetComponent petComponent = entity.getComponent(PetComponent.class);
    if (petComponent == null) {
      throw new IllegalStateException("PetMovementComponent requires a PetComponent");
    }

    owner = petComponent.getOwner();
    animator = entity.getComponent(AnimationRenderComponent.class);
    previousOwnerPosition.set(owner.getPosition());
    entity.setPosition(getFollowPosition());
  }

  @Override
  public void update() {
    if (!enabled || entity.isDisposed() || owner.isDisposed()) {
      return;
    }
    float deltaTime = Math.max(0f, timeSource.getDeltaTime());
    if (deltaTime == 0f) {
      return;
    }

    boolean ownerMoved = updateFacingDirection();
    updateIdleTime(ownerMoved, deltaTime);

    Vector2 currentPosition = entity.getPosition();
    Vector2 targetPosition = getFollowPosition();

    if (currentPosition.dst2(targetPosition) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
      idleTime = 0;
      updateIdleAnimation();
      entity.setPosition(targetPosition);
      return;
    }

    updateIdleAnimation();
    double flightTime = idleTime - FIRST_FLIGHT_TIME;
    if (flightTime >= 0) {
      // Evaluate the idle path directly so its duration does not depend on the frame rate.
      applyIdleFlightOffset(targetPosition, flightTime);
      entity.setPosition(targetPosition);
      return;
    }

    float interpolation = 1f - (float) Math.exp(-FOLLOW_SHARPNESS * deltaTime);
    entity.setPosition(currentPosition.lerp(targetPosition, interpolation));
  }

  private boolean updateFacingDirection() {
    Vector2 ownerPosition = owner.getPosition();
    float horizontalMovement = ownerPosition.x - previousOwnerPosition.x;
    boolean ownerMoved = !ownerPosition.epsilonEquals(previousOwnerPosition, MOVEMENT_EPSILON);

    if (Math.abs(horizontalMovement) > MOVEMENT_EPSILON) {
      facingDirection = Math.signum(horizontalMovement);
    }

    previousOwnerPosition.set(ownerPosition);
    return ownerMoved;
  }

  private void updateIdleTime(boolean ownerMoved, float deltaTime) {
    CombatStatsComponent ownerStats = owner.getComponent(CombatStatsComponent.class);
    if (ownerMoved || (ownerStats != null && ownerStats.isDead())) {
      idleTime = 0;
    } else {
      idleTime += deltaTime;
      if (idleTime >= FIRST_FLIGHT_TIME) {
        // Repeat only the flight phase, retaining precision during long idle periods.
        idleTime = FIRST_FLIGHT_TIME + (idleTime - FIRST_FLIGHT_TIME) % IDLE_FLIGHT_INTERVAL;
      }
    }
  }

  private void updateIdleAnimation() {
    int facingIndex = facingDirection > 0f ? 0 : 2;
    String direction = SPIN_DIRECTIONS[facingIndex];
    double spinTime = idleTime - IDLE_SPIN_DELAY;
    if (spinTime >= 0 && spinTime < SPIN_DURATION) {
      int turnStep = 1 + (int) (spinTime / SPIN_DIRECTION_DURATION);
      direction = SPIN_DIRECTIONS[(facingIndex + turnStep) % SPIN_DIRECTIONS.length];
    }
    double flightTime = idleTime - FIRST_FLIGHT_TIME;
    if (flightTime >= IDLE_FLIGHT_DURATION / 2f && flightTime < IDLE_FLIGHT_DURATION) {
      direction = SPIN_DIRECTIONS[(facingIndex + 2) % SPIN_DIRECTIONS.length];
    }

    // Turning the sprite must not change the side of the owner that the pet follows.
    String animation = animationPrefix + "_" + direction;
    if (animator != null && !animation.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(animation);
    }
  }

  private void applyIdleFlightOffset(Vector2 position, double flightTime) {
    if (flightTime >= IDLE_FLIGHT_DURATION) {
      return;
    }
    double angle = 2 * Math.PI * flightTime / IDLE_FLIGHT_DURATION;
    float distanceAcross = entity.getScale().x + owner.getScale().x + 2 * HORIZONTAL_GAP;
    float horizontalOffset = (float) ((1 - Math.cos(angle)) / 2) * distanceAcross;
    float arc = (float) Math.sin(angle);

    // Both legs stay above the resting height and slow down at either end of the trip.
    position.x += facingDirection * horizontalOffset;
    position.y += IDLE_FLIGHT_HEIGHT * arc * arc;
  }

  private Vector2 getFollowPosition() {
    Vector2 ownerPosition = owner.getPosition();
    Vector2 ownerScale = owner.getScale();
    Vector2 petScale = entity.getScale();

    float centerSeparation = ownerScale.x / 2f + petScale.x / 2f + HORIZONTAL_GAP;
    float petCenterX = ownerPosition.x + ownerScale.x / 2f - facingDirection * centerSeparation;
    float petX = petCenterX - petScale.x / 2f;
    float petY = ownerPosition.y + ownerScale.y + VERTICAL_GAP;
    return new Vector2(petX, petY);
  }
}
