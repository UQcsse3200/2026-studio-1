package com.csse3200.game.components.loot;

// Generates weapon items with properties based on weapon type and loot tier.

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
        return new WeaponItem("Basic Sword", WeaponType.SWORD, WeaponTier.values()[tier - 1], 1, 1);

      case BOW:
        return new WeaponItem("Basic Bow", WeaponType.BOW, WeaponTier.values()[tier - 1], 1, 1);

      case DAGGER:
        return new WeaponItem("Basic Dagger", WeaponType.DAGGER, 8 * tier, 1, 20);

      default:
        throw new IllegalArgumentException("Unsupported weapon type.");
    }
  }
}
