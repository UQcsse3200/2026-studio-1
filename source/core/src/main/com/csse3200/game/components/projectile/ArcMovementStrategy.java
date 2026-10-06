package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Projectile movement along a ballistic arc: at launch the body is given the velocity that makes it
 * reach a chosen point under the world's gravity, and gravity stays on for the whole flight.
 *
 * <p>Worked out once at launch:
 *
 * <ul>
 *   <li>flight time = horizontal distance divided by horizontal speed
 *   <li>sideways velocity = horizontal speed, in the direction of the target
 *   <li>vertical velocity = height difference divided by flight time, plus half of gravity times
 *       the flight time (gravity as a positive number)
 * </ul>
 *
 * <p>If the horizontal distance is almost zero (target level with or directly above the launch
 * point) there is no sensible flight time, so the rock is thrown straight up with a fixed small
 * flight time and lands back down. No division by zero may occur.
 *
 * <p><b>Limitations:</b> it aims at a point, not at a moving entity. The world gravity is read from
 * the physics engine's world, so a change to gravity changes the arc automatically. Existing
 * projectile bodies switch gravity OFF; this is the one strategy that switches it back ON.
 *
 * <p><b>Style reference:</b> {@code AimedLineMovementStrategy} (constructor validation, start).
 */
public class ArcMovementStrategy implements ProjectileMovementStrategy {

  /** Flight time used when the target is (almost) straight above or below the launch point. */
  static final float FALLBACK_FLIGHT_TIME = 0.5f;

  /** Horizontal distances below this count as "no sideways distance". */
  static final float MIN_HORIZONTAL_DISTANCE = 1e-3f;

  private final float horizontalSpeed;
  private final Vector2 targetPosition;

  /**
   * @param horizontalSpeed sideways speed in world units per second; greater than zero and finite
   * @param targetPosition the point to reach; not null, not non-finite; a copy is stored
   * @throws IllegalArgumentException if either argument is invalid
   */
  public ArcMovementStrategy(float horizontalSpeed, Vector2 targetPosition)
      throws IllegalArgumentException {
    if (!(horizontalSpeed > 0f) || Float.isInfinite(horizontalSpeed)) {
      throw new IllegalArgumentException("horizontalSpeed must be positive and finite");
    }
    if (targetPosition == null
        || !Float.isFinite(targetPosition.x)
        || !Float.isFinite(targetPosition.y)) {
      throw new IllegalArgumentException("targetPosition must be a finite point");
    }
    this.horizontalSpeed = horizontalSpeed;
    this.targetPosition = targetPosition.cpy();
  }

  /**
   * Switches gravity on for this body, removes damping, marks it as a bullet, then gives it the
   * launch velocity described above.
   *
   * @param projectile the projectile being moved
   */
  @Override
  public void start(Entity projectile) {
    PhysicsComponent physics = projectile.getComponent(PhysicsComponent.class);
    if (physics == null || physics.getBody() == null) {
      return;
    }
    Body body = physics.getBody();
    // The launch point is the entity's own position, as in the spec, not its centre.
    Vector2 from = projectile.getPosition();

    body.setGravityScale(1f);
    body.setLinearDamping(0f);
    body.setBullet(true);

    float gravity = -ServiceLocator.getPhysicsService().getPhysics().getWorld().getGravity().y;
    body.setLinearVelocity(computeLaunchVelocity(from, targetPosition, horizontalSpeed, gravity));
  }

  /** Does nothing: gravity and the launch velocity do all the work. */
  @Override
  public void update(Entity projectile, float delta) {
    // Intentionally empty.
  }

  /**
   * Works out the launch velocity that carries a body from one point to another under gravity. Kept
   * separate from {@link #start(Entity)} so the maths can be tested without a physics world.
   *
   * @param from launch point
   * @param to point to reach
   * @param horizontalSpeed sideways speed, greater than zero
   * @param gravity gravity as a positive number (world units per second squared)
   * @return the velocity to give the body
   */
  static Vector2 computeLaunchVelocity(
      Vector2 from, Vector2 to, float horizontalSpeed, float gravity) {
    float dx = to.x - from.x;
    float dy = to.y - from.y;
    float distance = Math.abs(dx);

    float flightTime;
    float vx;
    if (distance < MIN_HORIZONTAL_DISTANCE) {
      flightTime = FALLBACK_FLIGHT_TIME;
      vx = 0f;
    } else {
      flightTime = distance / horizontalSpeed;
      vx = Math.signum(dx) * horizontalSpeed;
    }
    float vy = dy / flightTime + 0.5f * gravity * flightTime;
    return new Vector2(vx, vy);
  }

  /**
   * @return the sideways speed in world units per second
   */
  public float getHorizontalSpeed() {
    return horizontalSpeed;
  }

  /**
   * @return a copy of the point the arc aims at
   */
  public Vector2 getTargetPosition() {
    return targetPosition.cpy();
  }
}
