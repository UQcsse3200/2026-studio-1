package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.entities.factories.DaggerFactory;
import com.csse3200.game.services.ServiceLocator;

/** Handles player attacks based on the currently equipped weapon. */
public class WeaponAttackComponent extends Component {
  private final WeaponItem weapon;
  private Vector2 attackDirection = new Vector2(1f, 0f);

  public WeaponAttackComponent(WeaponItem weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("Weapon must not be null.");
    }

    this.weapon = weapon;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("weaponAttack", this::attack);
    entity.getEvents().addListener("walk", this::updateAttackDirection);
  }

  private void updateAttackDirection(Vector2 direction) {
    if (direction != null && !direction.isZero()) {
      attackDirection = direction.cpy().nor();
    }
  }

  /** Performs an attack using the currently equipped weapon. */
  void attack() {
    WeaponItem activeWeapon = getActiveWeapon();

    if (activeWeapon == null) {
      return;
    }

    WeaponType type = activeWeapon.getWeaponType();

    switch (type) {
      case SWORD:
        swordAttack(activeWeapon);
        break;
      case BOW:
        bowAttack(activeWeapon);
        break;
      case DAGGER:
        daggerAttack(activeWeapon);
        break;
      default:
        throw new IllegalStateException("Unsupported weapon type: " + type);
    }
  }

  private WeaponItem getActiveWeapon() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    // Preserve existing behaviour for tests/entities without an inventory.
    if (inventory == null) {
      return weapon;
    }

    if (inventory.getActiveItem() instanceof WeaponItem activeWeapon) {
      return activeWeapon;
    }

    return null;
  }

  private void swordAttack(WeaponItem activeWeapon) {
    entity.getEvents().trigger("swordAttack", activeWeapon.getDamage());
  }

  private void bowAttack(WeaponItem activeWeapon) {
    entity.getEvents().trigger("bowAttack", activeWeapon.getDamage());

    Vector2 spawnPosition = entity.getCenterPosition();
    Vector2 direction = getProjectileDirection();

    Entity arrow =
        ArrowFactory.createArrow(spawnPosition, direction, activeWeapon.getDamage(), entity);

    ServiceLocator.getEntityService().register(arrow);
  }

  private void daggerAttack(WeaponItem activeWeapon) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null || activeWeapon.getQuantity() <= 0) {
      return;
    }

    Vector2 spawnPosition = entity.getCenterPosition();
    Vector2 direction = getProjectileDirection();

    Entity dagger =
        DaggerFactory.createDagger(spawnPosition, direction, activeWeapon.getDamage(), entity);

    ServiceLocator.getEntityService().register(dagger);

    inventory.removeItem(inventory.getActiveSlot(), 1);
  }

  private Vector2 getProjectileDirection() {
    WeaponRenderComponent weaponRender = entity.getComponent(WeaponRenderComponent.class);

    if (weaponRender != null) {
      Vector2 aimDirection = weaponRender.getAimDirection();

      if (aimDirection != null && !aimDirection.isZero()) {
        return aimDirection.nor();
      }
    }

    return attackDirection.cpy().nor();
  }

  public WeaponItem getWeapon() {
    return weapon;
  }
}
