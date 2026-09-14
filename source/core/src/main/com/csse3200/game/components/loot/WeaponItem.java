package com.csse3200.game.components.loot;

public class WeaponItem extends Item {
  private final WeaponType weaponType;
  private final int damage;
  private final int tier;
  private WeaponStats weaponStats;
  /* Delay, in seconds, between commit and resolve - matches the welder's swing animation.
   * Defaults to 0 if omitted. */
  private float windupDuration;

  public WeaponItem(String name, WeaponType weaponType, int damage, int quantity,
                    int maxQuantity, float windupDuration) throws IllegalArgumentException {
    super(name, ItemType.WEAPON, quantity, maxQuantity);
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (damage <= 0) {
      throw new IllegalArgumentException("Damage must be greater than zero.");
    }
    this.weaponType = weaponType;
    this.tier = 1;
    this.damage = damage;
    this.windupDuration = windupDuration;
  }

  public WeaponItem(String name, WeaponType weaponType, int damage, int quantity, int maxQuantity) {
    super(name, ItemType.WEAPON, quantity, maxQuantity);
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (damage <= 0) {
      throw new IllegalArgumentException("Damage must be greater than zero.");
    }
    this.weaponType = weaponType;
    this.tier = 1;
    this.damage = damage;
  }

  public WeaponItem(
      String name, WeaponType weaponType, WeaponTier weaponTier, int quantity,
      int maxQuantity, float windupDuration) throws IllegalArgumentException {
    super(name, ItemType.WEAPON, quantity, maxQuantity);

    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (weaponTier == null) {
      throw new IllegalArgumentException("WeaponTier must not be null.");
    }

    this.weaponType = weaponType;
    this.tier = weaponTier.getStats(weaponType).getTier();
    this.weaponStats = weaponTier.getStats(weaponType);
    this.damage = weaponStats.getDamage();
    this.windupDuration = windupDuration;
  }

  /**
   * Returns the weapon type that has been created.
   *
   * @return this item's weapon type - i.e bow, sword,
   */
  public WeaponType getWeaponType() {
    return weaponType;
  }

  public int getDamage() {
    if (weaponStats != null) {
      return weaponStats.getDamage();
    }
    return damage;
  }

  public float getWindupDuration() {
    return windupDuration;
  }

  public void setWindupDuration(float windupDuration) {
    this.windupDuration = windupDuration;
  }

  public int getTier() {
    return tier;
  }

  public float getAttackSpeed() {
    if (weaponStats != null) {
      return weaponStats.getAttackSpeed();
    }

    return 0f;
  }

  public float getKnockback() {
    if (weaponStats != null) {
      return weaponStats.getKnockback();
    }

    return 0f;
  }

  public float getRange() {
    if (weaponStats != null) {
      return weaponStats.getRange();
    }

    return 0f;
  }
}
