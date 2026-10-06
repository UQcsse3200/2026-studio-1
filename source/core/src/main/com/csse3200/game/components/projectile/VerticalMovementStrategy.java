package com.csse3200.game.components.projectile;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Creates a projectile moving straight down.
 *
 * <p>Requires the projectile entity to already have a {@link PhysicsComponent} when {@link
 * #start(Entity)} runs.
 */
public class VerticalMovementStrategy implements ProjectileMovementStrategy {
  private final float speed;

  public VerticalMovementStrategy(float speed) {
    if (speed <= 0) {
      throw new IllegalArgumentException("speed must be positive");
    }
    this.speed = speed;
  }

  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    if (body != null) {
      body.setLinearVelocity(0f, -speed); // straight down
    }
  }

  @Override
  public void update(Entity projectile, float delta) {
    // Constant velocity on a kinematic body, nothing to do per frame
  }
}
