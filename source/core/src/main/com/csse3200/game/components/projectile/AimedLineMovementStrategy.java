package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Projectile movement along a fixed straight line in ANY direction at constant speed. The direction
 * is chosen once, when the projectile is created, and never changes (no homing).
 *
 * <p>Sibling of {@link StraightLineMovementStrategy}, which is restricted to the x-axis. Movement
 * is driven through the projectile's physics body: a linear velocity is set once in {@link
 * #start(Entity)} and Box2D integrates the position from then on, so {@link #update(Entity, float)}
 * has nothing to do. The body is made gravity-free, undamped and a bullet so a dynamic projectile
 * flies like the kinematic one used to (see {@code ArrowFactory}).
 *
 * <p><b>Limitation:</b> requires a {@link PhysicsComponent} on the projectile when {@code start}
 * runs; the range limit is enforced elsewhere (by {@code ProjectileComponent}).
 */
public class AimedLineMovementStrategy implements ProjectileMovementStrategy {
  private final float speed;
  private final Vector2 direction;

  /**
   * @param speed travel speed in world units per second; must be greater than zero and finite
   * @param direction direction of travel; need not be unit length, but must not be the zero vector
   *     and must be finite. A normalised copy is stored, so the caller may reuse the object.
   * @throws IllegalArgumentException if speed is invalid, or direction is null, zero length, or
   *     contains a non-finite component
   */
  public AimedLineMovementStrategy(float speed, Vector2 direction) throws IllegalArgumentException {
    if (!(speed > 0) || Float.isInfinite(speed)) {
      throw new IllegalArgumentException("speed must be positive");
    }
    if (direction == null) {
      throw new IllegalArgumentException("direction must not be null");
    }
    if (!Float.isFinite(direction.x) || !Float.isFinite(direction.y)) {
      throw new IllegalArgumentException("direction must be finite");
    }
    if (direction.isZero()) {
      throw new IllegalArgumentException("direction must not be zero");
    }
    this.speed = speed;
    this.direction = direction.cpy().nor();
  }

  /**
   * Gives the projectile's body a constant velocity of {@code speed} along the direction.
   *
   * @param projectile the projectile being moved
   */
  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    body.setGravityScale(0f);
    body.setLinearDamping(0f);
    body.setBullet(true);
    body.setLinearVelocity(direction.cpy().scl(speed));
  }

  /** Intentionally does nothing: the constant velocity set at start persists on its own. */
  @Override
  public void update(Entity projectile, float delta) {
    // Intentionally empty.
  }

  /**
   * @return the travel speed in world units per second
   */
  public float getSpeed() {
    return speed;
  }

  /**
   * @return a copy of the unit direction of travel (changing it does not affect this strategy)
   */
  public Vector2 getDirection() {
    return direction.cpy();
  }
}
