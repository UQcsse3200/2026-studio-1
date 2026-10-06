package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Flies along the firing direction while oscillating perpendicular to it. */
public class WaveProjectileMovementStrategy implements ProjectileMovementStrategy {
  private static final float POSITION_CORRECTION_RATE = 6f;
  private final Vector2 forward;
  private final Vector2 sideways;
  private final float speed;
  private final float amplitude;
  private final float angularFrequency;
  private Vector2 startPosition;

  /**
   * @param direction non-zero firing direction
   * @param speed forward speed in world units per second
   * @param amplitude maximum sideways displacement in world units
   * @param frequency number of wave cycles per second
   */
  public WaveProjectileMovementStrategy(
      Vector2 direction, float speed, float amplitude, float frequency) {
    if (direction == null
        || direction.isZero()
        || speed <= 0f
        || amplitude < 0f
        || frequency <= 0f) {
      throw new IllegalArgumentException(
          "Wave movement requires a direction and valid wave settings");
    }
    forward = direction.cpy().nor();
    sideways = new Vector2(-forward.y, forward.x);
    this.speed = speed;
    this.amplitude = amplitude;
    angularFrequency = MathUtils.PI2 * frequency;
  }

  @Override
  public void start(Entity projectile) {
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    startPosition = body.getPosition().cpy();
    body.setGravityScale(0f);
    body.setLinearDamping(0f);
    body.setLinearVelocity(forward.cpy().scl(speed).mulAdd(sideways, amplitude * angularFrequency));
  }

  @Override
  public void update(Entity projectile, float delta) {
    if (delta <= 0f) {
      return;
    }
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    Vector2 travelled = body.getPosition().cpy().sub(startPosition);
    float phase = angularFrequency * travelled.dot(forward) / speed;
    float desiredOffset = amplitude * MathUtils.sin(phase);
    // Follow the wave's tangent and gently correct accumulated integration error. A bounded
    // correction rate stays stable when the next physics step differs from this frame's delta.
    float sidewaysSpeed =
        amplitude * angularFrequency * MathUtils.cos(phase)
            + POSITION_CORRECTION_RATE * (desiredOffset - travelled.dot(sideways));
    body.setLinearVelocity(forward.cpy().scl(speed).mulAdd(sideways, sidewaysSpeed));
  }
}
