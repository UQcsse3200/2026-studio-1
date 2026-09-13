package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/** Moves an arrow projectile in a fixed direction. */
public class ArrowMovementComponent extends Component {
  private static final float SPEED = 8f;
  private static final float LIFETIME = 4f;
  private static final float GRAVITY_SCALE = 0.2f;

  private final Vector2 direction;
  private float timeAlive;
  private Body body;

  public ArrowMovementComponent(Vector2 direction) {
    if (direction == null || direction.isZero()) {
      throw new IllegalArgumentException("Arrow direction must not be null or zero.");
    }

    this.direction = direction.cpy().nor();
  }

  @Override
  public void create() {
    PhysicsComponent physicsComponent = entity.getComponent(PhysicsComponent.class);

    if (physicsComponent != null) {
      body = physicsComponent.getBody();

      // Apply reduced gravity so the arrow follows a gentle arc.
      body.setGravityScale(GRAVITY_SCALE);

      // Prevent damping from slowing the arrow down.
      body.setLinearDamping(0f);

      // Launch the arrow in the firing direction.
      body.setLinearVelocity(direction.cpy().scl(SPEED));
    }
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();

    timeAlive += delta;

    if (timeAlive >= LIFETIME) {
      Gdx.app.postRunnable(entity::dispose);
    }
  }
}
