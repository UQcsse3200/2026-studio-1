package com.csse3200.game.components.loot;

// Generates weapon items with properties based on weapon type and loot tier.

import static com.csse3200.game.components.loot.WeaponType.*;

public class WeaponGenerator {

  public WeaponItem generateWeapon(WeaponType weaponType, int tier)
      throws IllegalArgumentException {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (tier <= 0) {
      throw new IllegalArgumentException("Tier must be greater than 0.");
    }
    String name =
        switch (weaponType) {
          case SWORD -> "Basic Sword";
          case BOW -> "Basic Bow";
          case DAGGER -> "Basic Dagger";
          default -> throw new IllegalArgumentException("Unsupported weapon type.");
        };
    float windUpDuration =
        switch (weaponType) {
          case SWORD -> 3;
          case BOW -> 2;
          case DAGGER -> 1;
        };
    int maxQuantity =
        switch (weaponType) {
          case SWORD -> 10;
          case BOW -> 10;
          case DAGGER -> 20;
        };
    return new WeaponItem(name, weaponType, windUpDuration, tier, 1, maxQuantity);

    switch (weaponType) {
      case SWORD:
        WeaponItem sword =
            new WeaponItem("Basic Sword", WeaponType.SWORD, WeaponTier.values()[tier - 1], 1, 1);
        sword.setSellPrice(10 * tier);
        return sword;

      case BOW:
        WeaponItem bow =
            new WeaponItem("Basic Bow", WeaponType.BOW, WeaponTier.values()[tier - 1], 1, 1);
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
