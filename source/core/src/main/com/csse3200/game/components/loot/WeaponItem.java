package com.csse3200.game.components.loot;

/**
 * Represents a weapon item that can be stored in any entity's inventory and usable to supply an
 * attack component's damage. Damage is computed from type + tier on demand. A weapon has the common
 * properties of an Item, as well as a weapon type and damage value.
 */
public class WeaponItem extends Item {

  private final WeaponType weaponType;
  private final int tier;
  /* Delay, in seconds, between commit and resolve - matches the welder's swing animation.
   * Defaults to 0 if omitted. */
  private float windupDuration;

  // Creates a weapon item.
  public WeaponItem(String name, WeaponType weaponType, float windupDuration, int tier,
                    int quantity, int maxQuantity)
      throws IllegalArgumentException {

    super(name, ItemType.WEAPON, quantity, maxQuantity);

    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (tier < 1) {
      throw new IllegalArgumentException("Damage must not be negative.");
    }
    this.windupDuration = windupDuration;
    this.weaponType = weaponType;
    this.tier = tier;
  }

  /**
   * Returns the weapon type that has been created.
   * @return this item's weapon type - i.e bow, sword,
   */
  public WeaponType getWeaponType() {
    return weaponType;
  }

  public int getDamage() {
    return weaponType.getBaseDamage() * this.tier;
  }

  public float getWindupDuration() {
    return windupDuration;
  }

  public void setWindupDuration(float windupDuration) {
    this.windupDuration = windupDuration;
  }
}
