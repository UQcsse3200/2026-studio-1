package com.csse3200.game.components.pet;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PetProjectileFactory;
import com.csse3200.game.services.ServiceLocator;

/** Creates projectiles for the attack requests emitted by {@link PetCombatComponent#update()}. */
public class PetProjectileSpawnerComponent extends Component {
  @Override
  public void create() {
    entity.getEvents().addListener("petAttack", this::spawnProjectile);
  }

  private void spawnProjectile(Entity target) {
    if (!enabled || entity.isDisposed()) {
      return;
    }
    // petAttack is emitted during update, after Box2D finishes processing collision callbacks.
    Entity projectile = PetProjectileFactory.createProjectile(entity, target);
    ServiceLocator.getEntityService().register(projectile);
  }
}
