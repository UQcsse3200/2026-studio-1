package com.csse3200.game.components.loot;

/**
 * One weapon type's stat block for a single tier. Replaces the private TierStats class - same role,
 * now built directly per type rather than assembled from scattered fields inside getStats().
 */
public class WeaponStatsPreset implements WeaponStats {
  private final int damage;
  private final float attackSpeed;
  private final float knockback;
  private final float range;
  private final int projectileCount;
  private final int tier;

  /**
   * @param damage value for the attack damage
   * @param attackSpeed configured attack speed per weapon
   * @param knockback configured knockback caused from weapon attack
   * @param range range at which weapon can be used to attack
   * @param projectileCount count of projectile count that weapon will be used if the weapon was
   *     configured for throwing.
   */
  public WeaponStatsPreset(
      int tier, int damage, float attackSpeed, float knockback, float range, int projectileCount) {
    this.damage = damage;
    this.attackSpeed = attackSpeed;
    this.knockback = knockback;
    this.range = range;
    this.projectileCount = projectileCount;
    this.tier = tier;
  }

  /**
   * @return configured damage from weapon
   */
  @Override
  public int getDamage() {
    return this.damage;
  }

  /**
   * @return attack speed of weapon
   */
  @Override
  public float getAttackSpeed() {
    return this.attackSpeed;
  }

  /**
   * @return knockback of weapon from attacking
   */
  @Override
  public float getKnockback() {
    return this.knockback;
  }

  /**
   * @return range of weapon to be able to attack
   */
  @Override
  public float getRange() {
    return this.range;
  }

  /**
   * @return number of projectiles when weapon is configured as a ranged component or for any
   *     attacking by the player.
   */
  @Override
  public int getProjectileCount() {
    return this.projectileCount;
  }

  /**
   * @return tier value of weapon in specific tier.
   */
  @Override
  public int getTier() {
    return this.tier;
  }
}
