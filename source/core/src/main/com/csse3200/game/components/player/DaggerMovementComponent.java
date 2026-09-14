package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Moves a thrown dagger in a fixed direction for a short duration. */
public class DaggerMovementComponent extends Component {
  private static final float SPEED = 10f;
  private static final float LIFETIME = 1.5f;

  private final Vector2 direction;
  private float timeAlive;

  public DaggerMovementComponent(Vector2 direction) {
    if (direction == null || direction.isZero()) {
      throw new IllegalArgumentException("Dagger direction must not be null or zero.");
    }

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
