package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Moves a projectile at constant speed in any direction, including angled shots from a pet. */
public class DirectedProjectileMovementStrategy implements ProjectileMovementStrategy {
  private final Vector2 velocity;

  /**
   * @param direction non-zero firing direction; copied and normalised
   * @param speed positive travel speed in world units per second
   */
  public DirectedProjectileMovementStrategy(Vector2 direction, float speed) {
    if (direction == null || direction.isZero() || speed <= 0f) {
      throw new IllegalArgumentException("Projectile direction and speed must be valid");
    }
    velocity = direction.cpy().nor().scl(speed);
  }

  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    body.setGravityScale(0f);
    body.setLinearDamping(0f);
    body.setLinearVelocity(velocity);
  }

  @Override
  public void update(Entity projectile, float delta) {
    // Box2D advances the body using the velocity set at launch.
  }
}
