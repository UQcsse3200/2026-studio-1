package com.csse3200.game.components.loot;

public enum WeaponTier {
  TIER_1(
      1,
      70,
      10,
      1.0f,
      1.0f,
      2.0f,
      7,
      1.0f,
      0.5f,
      8.0f),
  TIER_2(
      2,
      25,
      20,
      1.2f,
      1.5f,
      2.5f,
      14,
      1.2f,
      0.75f,
      10.0f),
  TIER_3(
      3,
      5,
      30,
      1.5f,
      2.0f,
      3.0f,
      21,
      1.5f,
      1.0f,
      12.0f);

  private final int tier;
  private final int lootWeight;
  private final int swordDamage;
  private final float swordAttackSpeed;
  private final float swordKnockback;
  private final float swordRange;
  private final int bowDamage;
  private final float bowAttackSpeed;
  private final float bowKnockback;
  private final float bowRange;

  WeaponTier(
      int tier,
      int lootWeight,
      int swordDamage,
      float swordAttackSpeed,
      float swordKnockback,
      float swordRange,
      int bowDamage,
      float bowAttackSpeed,
      float bowKnockback,
      float bowRange) {
    this.tier = tier;
    this.lootWeight = lootWeight;
    this.swordDamage = swordDamage;
    this.swordAttackSpeed = swordAttackSpeed;
    this.swordKnockback = swordKnockback;
    this.swordRange = swordRange;
    this.bowDamage = bowDamage;
    this.bowAttackSpeed = bowAttackSpeed;
    this.bowKnockback = bowKnockback;
    this.bowRange = bowRange;
  }

  public int getLootWeight() {
    return lootWeight;
  }

  public WeaponStats getStats(WeaponType weaponType) {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (weaponType == WeaponType.SWORD) {
      return new TierStats(
          tier, swordDamage, swordAttackSpeed, swordKnockback, swordRange);
    }

    return new TierStats(
        tier, bowDamage, bowAttackSpeed, bowKnockback, bowRange);
  }

  private static class TierStats implements WeaponStats {
    private final int tier;
    private final int damage;
    private final float attackSpeed;
    private final float knockback;
    private final float range;

    TierStats(int tier, int damage, float attackSpeed, float knockback, float range) {
      this.tier = tier;
      this.damage = damage;
      this.attackSpeed = attackSpeed;
      this.knockback = knockback;
      this.range = range;
    }

    @Override
    public int getTier() {
      return tier;
    }

    @Override
    public int getDamage() {
      return damage;
    }

    @Override
    public float getAttackSpeed() {
      return attackSpeed;
    }

    @Override
    public float getKnockback() {
      return knockback;
    }

    @Override
    public float getRange() {
      return range;
    }
  }
}