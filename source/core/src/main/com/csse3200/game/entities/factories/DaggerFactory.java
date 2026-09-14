package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.player.DaggerMovementComponent;
import com.csse3200.game.components.player.ProjectileHitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/** Factory for creating thrown dagger projectiles. */
public class DaggerFactory {

  /**
   * Legacy dagger creation method used by the rendering code.
   *
   * @param position spawn position
   * @param direction travel direction
   * @return dagger entity
   */
  public static Entity createDagger(Vector2 position, Vector2 direction) {
    Entity dagger =
        new Entity()
            .addComponent(new TextureRenderComponent("images/dagger.png"))
            .addComponent(new DaggerMovementComponent(direction));

    dagger.setPosition(position);
    dagger.setScale(0.35f, 0.2f);

    return dagger;
  }

  /**
   * Creates a combat dagger projectile with collision and damage.
   *
   * @param position spawn position
   * @param direction travel direction
   * @param damage projectile damage
   * @param owner entity that threw the dagger
   * @return dagger projectile entity
   */
  public static Entity createDagger(Vector2 position, Vector2 direction, int damage, Entity owner) {

    Entity dagger =
        new Entity()
            .addComponent(new TextureRenderComponent("images/dagger.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new DaggerMovementComponent(direction))
            .addComponent(new ProjectileHitComponent(damage, owner));

    dagger.setPosition(position);
    dagger.setScale(0.35f, 0.2f);

    return dagger;
  }

  private DaggerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
