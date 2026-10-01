package com.csse3200.game.components.loot;

public enum WeaponTier {
  TIER_1(1, 60, 10, 1.0f, 1.0f, 2.0f, 7, 1.0f, 0.5f, 8.0f, 3, 1.0f, 0.5f, 1.0f),
  TIER_2(2, 30, 20, 1.2f, 1.5f, 2.5f, 14, 1.2f, 0.75f, 10.0f, 6, 1.3f, 0.75f, 1.2f),
  TIER_3(3, 10, 30, 1.5f, 2.0f, 3.0f, 21, 1.5f, 1.0f, 12.0f, 9, 1.6f, 1.0f, 1.4f);

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
  private final int daggerDamage;
  private final float daggerAttackSpeed;
  private final float daggerKnockback;
  private final float daggerRange;

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
      float bowRange,
      int daggerDamage,
      float daggerAttackSpeed,
      float daggerKnockback,
      float daggerRange) {
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
    this.daggerDamage = daggerDamage;
    this.daggerAttackSpeed = daggerAttackSpeed;
    this.daggerKnockback = daggerKnockback;
    this.daggerRange = daggerRange;
  }

  public int getLootWeight() {
    return lootWeight;
  }

  public static WeaponTier fromTierNumber(int tierNumber) {
    for (WeaponTier weaponTier : values()) {
      if (weaponTier.tier == tierNumber) {
        return weaponTier;
      }
    }
    throw new IllegalArgumentException(
        "Invalid weapon tier: " + tierNumber + ". Must be between 1 and " + values().length + ".");
  }

  public WeaponStats getStats(WeaponType weaponType) {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    return switch (weaponType) {
      case SWORD -> new TierStats(tier, swordDamage, swordAttackSpeed, swordKnockback, swordRange);
      case BOW -> new TierStats(tier, bowDamage, bowAttackSpeed, bowKnockback, bowRange);
      case DAGGER ->
          new TierStats(tier, daggerDamage, daggerAttackSpeed, daggerKnockback, daggerRange);
    };
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
