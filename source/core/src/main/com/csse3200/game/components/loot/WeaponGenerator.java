package com.csse3200.game.components.loot;

/** Generates weapon items with properties based on weapon type and loot tier. */
public class WeaponGenerator {

  public WeaponItem generateWeapon(WeaponType weaponType, int tier) {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (tier <= 0) {
      throw new IllegalArgumentException("Tier must be greater than 0.");
    }

    switch (weaponType) {
      case SWORD:
        WeaponItem sword = new WeaponItem("Basic Sword", WeaponType.SWORD, 10 * tier, 1, 10);
        sword.setSellPrice(10 * tier);
        return sword;

      case BOW:
        WeaponItem bow = new WeaponItem("Basic Bow", WeaponType.BOW, 7 * tier, 1, 10);
        bow.setSellPrice(8 * tier);
        return bow;

      case DAGGER:
        WeaponItem dagger = new WeaponItem("Basic Dagger", WeaponType.DAGGER, 8 * tier, 1, 20);
        dagger.setSellPrice(6 * tier);
        return dagger;

      default:
        throw new IllegalArgumentException("Unsupported weapon type.");
    }
  }
}
