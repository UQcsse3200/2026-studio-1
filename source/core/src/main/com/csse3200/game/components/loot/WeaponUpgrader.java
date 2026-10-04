package com.csse3200.game.components.loot;

public class WeaponUpgrader {

  private final WeaponGenerator generator = new WeaponGenerator();

  public WeaponItem upgrade(WeaponItem weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("Weapon must not be null.");
    }

    int currentTier = weapon.getTier();
    int maxTier = WeaponTier.values().length;

    if (currentTier >= maxTier) {
      throw new IllegalStateException(
          "Cannot upgrade "
              + weapon.getWeaponType()
              + " - already at maximum tier ("
              + currentTier
              + ").");
    }

    if (weapon.getWeaponType() == WeaponType.DAGGER) {
      return generator.generateWeapon(WeaponType.SWORD, currentTier + 1);
    }

    return generator.generateWeapon(weapon.getWeaponType(), currentTier + 1);
  }
}
