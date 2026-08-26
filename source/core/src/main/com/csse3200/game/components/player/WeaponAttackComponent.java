package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;

/** Handles player attacks based on the currently equipped weapon. */
public class WeaponAttackComponent extends Component {
  private final WeaponItem weapon;

  public WeaponAttackComponent(WeaponItem weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("Weapon must not be null.");
    }

    this.weapon = weapon;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("weaponAttack", this::attack);
  }

  /** Performs an attack using the equipped weapon. */
  void attack() {
    WeaponType type = weapon.getWeaponType();

    switch (type) {
      case SWORD:
        swordAttack();
        break;
      case BOW:
        bowAttack();
        break;
      default:
        throw new IllegalStateException("Unsupported weapon type: " + type);
    }
  }

  private void swordAttack() {
    entity.getEvents().trigger("swordAttack", weapon.getDamage());
  }

  private void bowAttack() {
    entity.getEvents().trigger("bowAttack", weapon.getDamage());
  }

  public WeaponItem getWeapon() {
    return weapon;
  }
}
