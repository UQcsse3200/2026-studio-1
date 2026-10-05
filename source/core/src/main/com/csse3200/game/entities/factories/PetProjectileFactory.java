package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.pet.PetComponent;
import com.csse3200.game.components.pet.PetProjectileComponent;
import com.csse3200.game.components.pet.PetType;
import com.csse3200.game.components.projectile.DirectedProjectileMovementStrategy;
import com.csse3200.game.components.projectile.HomingProjectileMovementStrategy;
import com.csse3200.game.components.projectile.ProjectileMovementStrategy;
import com.csse3200.game.components.projectile.WaveProjectileMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.PetProjectileRenderComponent;

/** Creates companion projectiles aimed at the enemy just hit by their owner. */
public final class PetProjectileFactory {
  private static final int DAMAGE = 5;
  private static final float SPEED = 8f;
  private static final float WAVE_AMPLITUDE = 0.25f;
  private static final float WAVE_FREQUENCY = 2f;
  private static final float HOMING_SPEED = 6f;
  private static final float HOMING_TURN_RATE = 360f;

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

    PetType type = pet.getComponent(PetComponent.class).getType();
    ProjectileMovementStrategy movement =
        switch (type) {
          case BIRD -> new DirectedProjectileMovementStrategy(direction, SPEED);
          case BAT ->
              new WaveProjectileMovementStrategy(direction, SPEED, WAVE_AMPLITUDE, WAVE_FREQUENCY);
          case SPIRIT ->
              new HomingProjectileMovementStrategy(target, HOMING_SPEED, HOMING_TURN_RATE);
        };
    float width = type == PetType.BIRD ? 0.5f : 0.35f;
    float height = type == PetType.BIRD ? 0.22f : 0.35f;
    Entity projectile =
        new Entity()
            .addComponent(new PetProjectileRenderComponent(type))
            // A dynamic sensor detects static terrain as well as moving enemies.
            .addComponent(new PhysicsComponent())
            .addComponent(
                new HitboxComponent()
                    .setLayer(PhysicsLayer.DEFAULT)
                    .setMask((short) (PhysicsLayer.NPC | PhysicsLayer.OBSTACLE)))
            .addComponent(new PetProjectileComponent(pet, target, DAMAGE, movement));
    projectile.setScale(width, height);
    projectile.setPosition(spawnCentre.x - width / 2f, spawnCentre.y - height / 2f);
    return projectile;
  }

  private PetProjectileFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
