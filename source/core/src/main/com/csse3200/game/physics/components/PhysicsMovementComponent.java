package com.csse3200.game.physics.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.movement.MovementController;
import com.csse3200.game.components.Component;
import com.csse3200.game.utils.math.Vector2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Movement controller for a physics-based entity. */
public class PhysicsMovementComponent extends Component implements MovementController {
  private static final Logger logger = LoggerFactory.getLogger(PhysicsMovementComponent.class);
  private static final Vector2 maxSpeed = Vector2Utils.ONE;

  private PhysicsComponent physicsComponent;
  private Vector2 targetPosition;
  private boolean movementEnabled = true;
  private boolean groundedMovement = false;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
  }

  @Override
  public void update() {
    if (movementEnabled && targetPosition != null) {
      Body body = physicsComponent.getBody();
      updateDirection(body);
    }
  }

  /**
   * Enable/disable movement for the controller. Disabling will immediately set velocity to 0.
   *
   * @param movementEnabled true to enable movement, false otherwise
   */
  @Override
  public void setMoving(boolean movementEnabled) {
    this.movementEnabled = movementEnabled;
    if (!movementEnabled) {
      Body body = physicsComponent.getBody();
      setToVelocity(body, Vector2.Zero);
    }
  }

  @Override
  public boolean getMoving() {
    return movementEnabled;
  }

  /**
   * @return Target position in the world
   */
  @Override
  public Vector2 getTarget() {
    return targetPosition;
  }

  /**
   * Set a target to move towards. The entity will be steered towards it in a straight line, not
   * using pathfinding or avoiding other entities.
   *
   * @param target target position
   */
  @Override
  public void setTarget(Vector2 target) {
    logger.trace("Setting target to {}", target);
    this.targetPosition = target;
  }

  /**
   * Restrict this movement controller to horizontal movement only, leaving vertical motion (e.g.
   * falling under gravity) entirely to the physics engine. Useful for grounded entities that should
   * be pulled down by gravity instead of being steered directly toward a target's height.
   *
   * @param groundedMovement true to ignore the vertical component of the movement direction
   */
  public void setGroundedMovement(boolean groundedMovement) {
    this.groundedMovement = groundedMovement;
  }

  /**
   * @return true if this controller is currently restricted to horizontal-only movement
   */
  public boolean isGroundedMovement() {
    return groundedMovement;
  }

  private void updateDirection(Body body) {
    Vector2 desiredVelocity = getDirection().scl(maxSpeed);
    setToVelocity(body, desiredVelocity);
  }

  private void setToVelocity(Body body, Vector2 desiredVelocity) {
    // impulse force = (desired velocity - current velocity) * mass
    Vector2 velocity = body.getLinearVelocity();
    Vector2 clampedVelocity = desiredVelocity.cpy();
    if (groundedMovement) {
      // Only steer horizontally.
      clampedVelocity.y = velocity.y;
    }
    Vector2 impulse = clampedVelocity.sub(velocity).scl(body.getMass());
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  private Vector2 getDirection() {
    // Move towards targetPosition based on our current position
    Vector2 direction = targetPosition.cpy().sub(entity.getPosition());
    if (groundedMovement) {
      // Ignore vertical distance to the target
      direction.y = 0f;
    }
    return direction.nor();
  }
}
