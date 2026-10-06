package com.csse3200.game.components.loot;

import java.util.HashMap;
import java.util.Map;

public enum WeaponTier {
  TIER_1(
      1,
      60,
      new WeaponStatsPreset(1, 10, 1.0f, 1.0f, 2.0f, 1),
      new WeaponStatsPreset(1, 7, 1.0f, 0.5f, 8.0f, 1),
      new WeaponStatsPreset(1, 3, 1.0f, 0.5f, 1.0f, 1),
      new WeaponStatsPreset(1, 12, 1.0f, 1.3f, 2.3f, 1)),
  TIER_2(
      2,
      30,
      new WeaponStatsPreset(2, 20, 1.2f, 1.5f, 2.5f, 1),
      new WeaponStatsPreset(2, 4, 1.2f, 0.75f, 10.0f, 5),
      new WeaponStatsPreset(2, 6, 1.3f, 0.75f, 1.2f, 1),
      new WeaponStatsPreset(2, 23, 1.2f, 1.8f, 2.8f, 1)),
  TIER_3(
      3,
      10,
      new WeaponStatsPreset(3, 30, 1.5f, 2.0f, 3.0f, 1),
      new WeaponStatsPreset(3, 3, 1.5f, 1.0f, 12.0f, 10),
      new WeaponStatsPreset(3, 9, 1.6f, 1.0f, 1.4f, 1),
      new WeaponStatsPreset(3, 34, 1.5f, 2.3f, 3.3f, 1));

  private int tier;
  private final int lootWeight;
  private Map<WeaponType, WeaponStatsPreset> statsByType;

  WeaponTier(
      int tier,
      int lootWeight,
      WeaponStatsPreset swordStats,
      WeaponStatsPreset bowStats,
      WeaponStatsPreset daggerStats,
      WeaponStatsPreset axeStats) {
    this.tier = tier;
    this.lootWeight = lootWeight;
    statsByType = new HashMap<>();
    this.statsByType.put(WeaponType.SWORD, swordStats);
    this.statsByType.put(WeaponType.BOW, bowStats);
    this.statsByType.put(WeaponType.DAGGER, daggerStats);
    this.statsByType.put(WeaponType.AXE, axeStats);
  }

  public int getLootWeight() {
    return lootWeight;
  }

  public int getTier() {
    return this.tier;
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

  public WeaponStatsPreset getStats(WeaponType weaponType) {
    if (weaponType == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }

    if (!statsByType.containsKey(weaponType)) {
      throw new IllegalArgumentException("No Stats are registered to weapon type");
    }
    return statsByType.get(weaponType);
  }
}
