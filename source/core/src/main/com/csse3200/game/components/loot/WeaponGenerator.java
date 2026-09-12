package com.csse3200.game.components.loot;

// Generates weapon items with properties based on weapon type and loot tier.

import static com.csse3200.game.components.loot.WeaponType.*;

public class WeaponGenerator {

  public WeaponItem generateWeapon(WeaponType weaponType, int tier)
      throws IllegalArgumentException {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    //    if (tier <= 0) {
    //      throw new IllegalArgumentException("Tier must be greater than 0.");
    //    }
    String name =
        switch (weaponType) {
          case SWORD -> "Basic Sword";
          case BOW -> "Basic Bow";
          case DAGGER -> "Basic Dagger";
          default -> throw new IllegalArgumentException("Unsupported weapon type.");
        };

    return new WeaponItem(name, weaponType, tier, 1, 1);
  }
}
