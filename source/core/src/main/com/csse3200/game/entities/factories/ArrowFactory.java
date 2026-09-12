package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.ArrowMovementComponent;
import com.csse3200.game.components.player.ProjectileHitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/** Factory for creating arrow projectiles. */
public class ArrowFactory {

  public static Entity createArrow(Vector2 position, Vector2 direction, int damage, Entity owner) {

    Entity arrow =
        new Entity()
            .addComponent(new TextureRenderComponent("images/arrow.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new ArrowMovementComponent(direction))
            .addComponent(new ProjectileHitComponent(damage, owner));

    arrow.setPosition(position);
    arrow.setScale(0.5f, 0.2f);

    return arrow;
  }

  private ArrowFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
