package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

// Moves an arrow projectile in a fixed direction.
public class ArrowMovementComponent extends Component {
  private static final float SPEED = 8f;
  private static final float LIFETIME = 2f;

  private final Vector2 direction;
  private float timeAlive;

  public ArrowMovementComponent(Vector2 direction) {
    this.direction = direction.cpy().nor();
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();

    Vector2 movement = direction.cpy().scl(SPEED * delta);
    entity.setPosition(entity.getPosition().add(movement));

    timeAlive += delta;

    if (timeAlive >= LIFETIME) {
      entity.setPosition(-1000f, -1000f);
      entity.setEnabled(false);
    }
  }
}
