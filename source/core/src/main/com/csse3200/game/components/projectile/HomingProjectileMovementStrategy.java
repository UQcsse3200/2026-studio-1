package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Follows the original target at constant speed with a limited turning rate. */
public class HomingProjectileMovementStrategy implements ProjectileMovementStrategy {
  private final Entity target;
  private final float speed;
  private final float turnRate;

  /**
   * @param target enemy to follow for this projectile's flight
   * @param speed positive travel speed in world units per second
   * @param turnRate maximum turn in degrees per second
   */
  public HomingProjectileMovementStrategy(Entity target, float speed, float turnRate) {
    if (target == null || speed <= 0f || turnRate <= 0f) {
      throw new IllegalArgumentException("Homing movement requires a target, speed and turn rate");
    }
    this.target = target;
    this.speed = speed;
    this.turnRate = turnRate;
  }

  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    Vector2 direction = target.getCenterPosition().sub(projectile.getCenterPosition());
    if (direction.isZero()) {
      direction.set(1f, 0f);
    }
    body.setGravityScale(0f);
    body.setLinearDamping(0f);
    body.setLinearVelocity(direction.nor().scl(speed));
  }

  @Override
  public void update(Entity projectile, float delta) {
    if (delta <= 0f || target.isDisposed()) {
      return;
    }
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    Vector2 direction = target.getCenterPosition().sub(projectile.getCenterPosition());
    if (direction.isZero()) {
      return;
    }
    float currentAngle = body.getLinearVelocity().angleDeg();
    // Wrap to the shortest turn so crossing 0/360 degrees does not produce a full circle.
    float angleDifference = (direction.angleDeg() - currentAngle + 540f) % 360f - 180f;
    float turn = MathUtils.clamp(angleDifference, -turnRate * delta, turnRate * delta);
    body.setLinearVelocity(new Vector2(speed, 0f).setAngleDeg(currentAngle + turn));
  }
}
