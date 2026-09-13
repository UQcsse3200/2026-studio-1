package com.csse3200.game.components.loot;

// Generates weapon items with properties based on weapon type and loot tier.

public class WeaponGenerator {
  /** One copy of a basic weapon can be collected from each of the planned rooms. */
  private static final int BASIC_WEAPON_MAX_QUANTITY = 5;

  public WeaponItem generateWeapon(WeaponType weaponType, int tier) {
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
    float windUpDuration =
        switch (weaponType) {
          case SWORD -> 3;
          case BOW -> 2;
          case DAGGER -> 1;
        };
    int maxQuantity = BASIC_WEAPON_MAX_QUANTITY;
    return new WeaponItem(name, weaponType, windUpDuration, tier, 1, maxQuantity);
  }
}
