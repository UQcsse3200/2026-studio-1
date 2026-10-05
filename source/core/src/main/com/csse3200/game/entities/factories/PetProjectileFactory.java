package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.pet.PetComponent;
import com.csse3200.game.components.pet.PetProjectileComponent;
import com.csse3200.game.components.projectile.DirectedProjectileMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/** Creates companion projectiles aimed at the enemy just hit by their owner. */
public final class PetProjectileFactory {
  private static final int DAMAGE = 5;
  private static final float SPEED = 8f;
  private static final float WIDTH = 0.4f;
  private static final float HEIGHT = 0.2f;

  /**
   * Creates an unregistered projectile at the pet's centre. Call outside physics callbacks.
   *
   * @param pet companion with a {@link PetComponent}
   * @param target enemy selected by the pet's assist controller
   * @return projectile ready to register with the entity service
   */
  public static Entity createProjectile(Entity pet, Entity target) {
    if (pet == null || pet.getComponent(PetComponent.class) == null || target == null) {
      throw new IllegalArgumentException("A pet with an owner and an enemy target are required");
    }
    Vector2 spawnCentre = pet.getCenterPosition();
    Vector2 direction = target.getCenterPosition().sub(spawnCentre);
    if (direction.isZero()) {
      direction.set(1f, 0f);
    }

    TextureRenderComponent renderer = new TextureRenderComponent("images/items/arrow.png");
    renderer.setRotationDegrees(direction.angleDeg());
    Entity projectile =
        new Entity()
            .addComponent(renderer)
            // A dynamic sensor detects static terrain as well as moving enemies.
            .addComponent(new PhysicsComponent())
            .addComponent(
                new HitboxComponent()
                    .setLayer(PhysicsLayer.DEFAULT)
                    .setMask((short) (PhysicsLayer.NPC | PhysicsLayer.OBSTACLE)))
            .addComponent(
                new PetProjectileComponent(
                    pet, target, DAMAGE, new DirectedProjectileMovementStrategy(direction, SPEED)));
    projectile.setScale(WIDTH, HEIGHT);
    projectile.setPosition(spawnCentre.x - WIDTH / 2f, spawnCentre.y - HEIGHT / 2f);
    return projectile;
  }

  private PetProjectileFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
