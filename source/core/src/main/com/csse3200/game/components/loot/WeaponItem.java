package com.csse3200.game.components.loot;

public class WeaponItem extends Item {
  private final WeaponType weaponType;
  private final int damage;
  private final int tier;
  private WeaponStats weaponStats;
  /* Delay, in seconds, between commit and resolve - matches the welder's swing animation.
   * Defaults to 0 if omitted. */
  private float windupDuration;
  private int projectileCount;

  /**
   * Creates the weapon item that is tiered with the damage, attack speed, knockback, range and
   * projectile count.
   *
   * @param name of weapon item
   * @param weaponType of weapon, listed in {@link WeaponType}
   * @param damage value of attack for the weapon which isn't tiered.
   * @param quantity number of items in the stack
   * @param maxQuantity max number of weapons that can be equipped in the stack
   * @param windupDuration duration needed for the animations to configure with the weapon
   * @throws IllegalArgumentException when {@link WeaponTier} are null or parameter damage is less
   *     or equal to 0
   */
  public WeaponItem(
      String name,
      WeaponType weaponType,
      int damage,
      int quantity,
      int maxQuantity,
      float windupDuration)
      throws IllegalArgumentException {
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

  /**
   * Creates the weapon item that is defaulted to no tiered value, only configured damage
   *
   * @param name of weapon item
   * @param weaponType of weapon, listed in {@link WeaponType}
   * @param quantity number of items in the stack
   * @param maxQuantity max number of weapons that can be equipped in the stack
   * @throws IllegalArgumentException when {@link WeaponTier} or {@link WeaponType} are null
   */
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

  /**
   * Creates the weapon item that is tiered with the damage, attack speed, knockback, range and
   * projectile count.
   *
   * @param name of weapon item
   * @param weaponType of weapon, listed in {@link WeaponType}
   * @param weaponTier listed values of set parameters outside of the given ones that are consistent
   *     for any weapon and
   * @param quantity number of items in the stack
   * @param maxQuantity max number of weapons that can be equipped in the stack
   * @param windupDuration duration needed for the animations to configure with the weapon
   * @throws IllegalArgumentException when {@link WeaponTier} or {@link WeaponType} are null
   */
  public WeaponItem(
      String name,
      WeaponType weaponType,
      WeaponTier weaponTier,
      int quantity,
      int maxQuantity,
      float windupDuration)
      throws IllegalArgumentException {
    super(name, ItemType.WEAPON, quantity, maxQuantity);

    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (weaponTier == null) {
      throw new IllegalArgumentException("WeaponTier must not be null.");
    }

    this.weaponType = weaponType;
    this.tier = weaponTier.getTier();
    this.weaponStats = weaponTier.getStats(weaponType);
    this.damage = weaponStats.getDamage();
    this.windupDuration = windupDuration;
    this.projectileCount = weaponTier.getStats(weaponType).getProjectileCount();
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
    return this.tier;
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

  public int getProjectileCount() {
    return this.projectileCount;
  }
}
