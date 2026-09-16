package com.csse3200.game.components.projectile;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Default projectile movement: flies in a straight line at a constant speed, restricted entirely to
 * the x-axis (no vertical movement at all) - e.g. a skeleton ranger's arrow.
 *
 * <p>Movement is driven through the projectile's {@link PhysicsComponent} body, not by setting the
 * entity's position directly: the body must be a {@code KinematicBody} (see {@link
 * com.csse3200.game.entities.factories.ArrowFactory#createRangedArrow}), which means it is
 * unaffected by gravity or forces but still takes part in Box2D collision detection. Setting a
 * constant horizontal linear velocity once is enough - Box2D integrates the body's position every
 * physics step from then on, and {@link PhysicsComponent#earlyUpdate()} copies that position back
 * onto the entity every frame, exactly like any other physics-driven entity (the player, NPCs via
 * {@code PhysicsMovementComponent}, ...). This is why {@link #update} below is a no-op.
 *
 * <p>Requires the projectile entity to already have a {@link PhysicsComponent} when {@link
 * #start(Entity)} runs.
 */
public class StraightLineMovementStrategy implements ProjectileMovementStrategy {
  private final float speed;
  private final boolean movingRight;

  /**
   * @param speed travel speed in world units/second. Must be positive.
   * @param movingRight true to fly in the +x direction, false to fly in the -x direction.
   * @throws IllegalArgumentException if speed is not positive.
   */
  public StraightLineMovementStrategy(float speed, boolean movingRight) {
    if (speed <= 0) {
      throw new IllegalArgumentException("speed must be positive");
    }
    this.speed = speed;
    this.movingRight = movingRight;
  }

  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    float velocityX = movingRight ? speed : -speed;
    // y is intentionally left at 0 and never touched again - this is the x-axis restriction.
    body.setLinearVelocity(velocityX, 0f);
  }

  @Override
  public void update(Entity projectile, float delta) {
    // Nothing to do per-frame: the constant velocity set in start() persists on the kinematic
    // body by itself. Left as a hook for movement types that DO need continuous per-frame
    // updates - e.g. a slow "follow" arrow re-aiming at a moving target each frame, or a
    // lobbed/bow-arc shot adjusting its vertical velocity over time.
  }
}
