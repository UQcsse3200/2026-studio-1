package com.csse3200.game.components.pet;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/** Keeps a companion pet hovering above and behind its owner. */
public class PetMovementComponent extends Component {
  private static final float HORIZONTAL_GAP = 0.25f;
  private static final float VERTICAL_GAP = 0.25f;
  private static final float FOLLOW_SHARPNESS = 6f;
  private static final float TELEPORT_DISTANCE = 6f;
  private static final float DIRECTION_EPSILON = 0.001f;

  private final GameTime timeSource;
  private Entity owner;
  private float previousOwnerX;
  private float facingDirection = 1f;

  /** Creates pet movement using the game's time source. */
  public PetMovementComponent() {
    this(ServiceLocator.getTimeSource());
  }

  PetMovementComponent(GameTime timeSource) {
    if (timeSource == null) {
      throw new IllegalArgumentException("Pet movement requires a time source");
    }
    this.timeSource = timeSource;
  }

  @Override
  public void create() {
    PetComponent petComponent = entity.getComponent(PetComponent.class);
    if (petComponent == null) {
      throw new IllegalStateException("PetMovementComponent requires a PetComponent");
    }

    owner = petComponent.getOwner();
    previousOwnerX = owner.getPosition().x;
    entity.setPosition(getFollowPosition());
  }

  @Override
  public void update() {
    updateFacingDirection();

    Vector2 currentPosition = entity.getPosition();
    Vector2 targetPosition = getFollowPosition();

    if (currentPosition.dst2(targetPosition) > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
      entity.setPosition(targetPosition);
      return;
    }

    float deltaTime = Math.max(0f, timeSource.getDeltaTime());
    float interpolation = 1f - (float) Math.exp(-FOLLOW_SHARPNESS * deltaTime);
    entity.setPosition(currentPosition.lerp(targetPosition, interpolation));
  }

  private void updateFacingDirection() {
    float ownerX = owner.getPosition().x;
    float horizontalMovement = ownerX - previousOwnerX;

    if (Math.abs(horizontalMovement) > DIRECTION_EPSILON) {
      facingDirection = Math.signum(horizontalMovement);
    }
    previousOwnerX = ownerX;
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
