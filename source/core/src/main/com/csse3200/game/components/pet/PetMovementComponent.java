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
 * Moving again resets the idle timer and interrupts the turn. Idle timing uses game time, so it
 * pauses with the game.
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
    updateIdleAnimation(ownerMoved, deltaTime);

    Vector2 currentPosition = entity.getPosition();
    Vector2 targetPosition = getFollowPosition();

    if (currentPosition.dst2(targetPosition) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
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

  private void updateIdleAnimation(boolean ownerMoved, float deltaTime) {
    CombatStatsComponent ownerStats = owner.getComponent(CombatStatsComponent.class);
    if (ownerMoved || (ownerStats != null && ownerStats.isDead())) {
      idleTime = 0;
    } else {
      // Cap the timer after one turn; only owner movement starts a new idle period.
      idleTime = Math.min(idleTime + deltaTime, (double) IDLE_SPIN_DELAY + SPIN_DURATION);
    }

    int facingIndex = facingDirection > 0f ? 0 : 2;
    String direction = SPIN_DIRECTIONS[facingIndex];
    double spinTime = idleTime - IDLE_SPIN_DELAY;
    if (spinTime >= 0 && spinTime < SPIN_DURATION) {
      int turnStep = 1 + (int) (spinTime / SPIN_DIRECTION_DURATION);
      direction = SPIN_DIRECTIONS[(facingIndex + turnStep) % SPIN_DIRECTIONS.length];
    }

    // Turning the sprite must not change the side of the owner that the pet follows.
    String animation = animationPrefix + "_" + direction;
    if (animator != null && !animation.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(animation);
    }
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
