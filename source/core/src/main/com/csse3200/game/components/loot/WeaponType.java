package com.csse3200.game.components.loot;

// Represents the different types of weapons available in the game.

public enum WeaponType {
  SWORD(/* baseDamage */ 10),
  BOW(/* baseDamage */ 7),
  DAGGER(/* baseDamage */ 3);

  private final int baseDamage;

  /**
   * Constructor to set Base damage for each weapon type
   *
   * @param baseDamage - tier 1 damage made by weapon
   */
  WeaponType(int baseDamage) {
    this.baseDamage = baseDamage;
  }

  /**
   * Returns the actual base damage set for each weapon type
   *
   * @return this type's tier-1 damage
   */
  public int getBaseDamage() {
    return this.baseDamage;
  }
}
