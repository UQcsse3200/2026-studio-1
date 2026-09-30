package com.csse3200.game.components.loot;

public class WeaponUpgrader {

  private final WeaponGenerator generator = new WeaponGenerator();

  public WeaponItem upgrade(WeaponItem weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("Weapon must not be null.");
    }

    int currentTier = weapon.getTier();
    int maxTier = WeaponTier.values().length;

    if (weapon.getWeaponType() == WeaponType.DAGGER && currentTier == maxTier) {
      return generator.generateWeapon(WeaponType.SWORD, 1);
    } // Dagger's final upgrade becomes a sword rather than a higher-tier dagger, so the
    // existing sword attack code can be reused without any changes.

    if (currentTier >= maxTier) {
      throw new IllegalStateException(
          "Cannot upgrade "
              + weapon.getWeaponType()
              + " - already at maximum tier ("
              + currentTier
              + ").");
    }

    return generator.generateWeapon(weapon.getWeaponType(), currentTier + 1);
  }
}
