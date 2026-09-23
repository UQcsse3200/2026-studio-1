package com.csse3200.game.components.loot;

// Generates weapon items with properties based on weapon type and loot tier.

public class WeaponGenerator {

  public WeaponItem generateWeapon(WeaponType weaponType, int tier)
      throws IllegalArgumentException {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (tier <= 0) {
      throw new IllegalArgumentException("Tier must be greater than 0.");
    }

    WeaponTier weaponTier = WeaponTier.fromTierNumber(tier);

    switch (weaponType) {
      case SWORD:
        WeaponItem sword =
            new WeaponItem("Basic Sword", WeaponType.SWORD, weaponTier, 1, 10, 2);
        sword.setSellPrice(10 * tier);
        return sword;

      case BOW:
        WeaponItem bow =
            new WeaponItem("Basic Bow", WeaponType.BOW, weaponTier, 1, 10, 3);
        bow.setSellPrice(8 * tier);
        return bow;

      case DAGGER:
        WeaponItem dagger =
            new WeaponItem("Basic Dagger", WeaponType.DAGGER, weaponTier, 1, 20, 1);
        dagger.setSellPrice(6 * tier);
        return dagger;

      default:
        throw new IllegalArgumentException("Unsupported weapon type.");
    }
  }
}