package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.DaggerMovementComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.TextureRenderComponent;

/** Factory for creating thrown dagger projectiles. */
public class DaggerFactory {

  public static Entity createDagger(Vector2 position, Vector2 direction) {
    Entity dagger =
        new Entity()
            .addComponent(new TextureRenderComponent("images/dagger.png"))
            .addComponent(new DaggerMovementComponent(direction));

    dagger.setPosition(position);
    dagger.setScale(0.35f, 0.2f);

    return dagger;
  }

  private DaggerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
