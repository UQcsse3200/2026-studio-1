package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class WeaponTierTest {

  @Test
  void shouldReturnCorrectSwordStatsForEachTier() {
    WeaponStats tier1 = WeaponTier.TIER_1.getStats(WeaponType.SWORD);
    WeaponStats tier2 = WeaponTier.TIER_2.getStats(WeaponType.SWORD);
    WeaponStats tier3 = WeaponTier.TIER_3.getStats(WeaponType.SWORD);

    assertEquals(10, tier1.getDamage());
    assertEquals(20, tier2.getDamage());
    assertEquals(30, tier3.getDamage());
  }

  @Test
  void shouldReturnCorrectBowStatsForEachTier() {
    WeaponStats tier1 = WeaponTier.TIER_1.getStats(WeaponType.BOW);
    WeaponStats tier2 = WeaponTier.TIER_2.getStats(WeaponType.BOW);
    WeaponStats tier3 = WeaponTier.TIER_3.getStats(WeaponType.BOW);

    assertEquals(7, tier1.getDamage());
    assertEquals(4, tier2.getDamage());
    assertEquals(3, tier3.getDamage());
  }

  @Test
  void shouldReturnCorrectTierNumbers() {
    assertEquals(1, WeaponTier.TIER_1.getTier());
    assertEquals(2, WeaponTier.TIER_2.getTier());
    assertEquals(3, WeaponTier.TIER_3.getTier());
  }

  @Test
  void shouldReturnCorrectSwordAdditionalStatsForEachTier() {
    WeaponStats tier1 = WeaponTier.TIER_1.getStats(WeaponType.SWORD);
    WeaponStats tier2 = WeaponTier.TIER_2.getStats(WeaponType.SWORD);
    WeaponStats tier3 = WeaponTier.TIER_3.getStats(WeaponType.SWORD);

    assertEquals(1.0f, tier1.getAttackSpeed());
    assertEquals(1.2f, tier2.getAttackSpeed());
    assertEquals(1.5f, tier3.getAttackSpeed());

    assertEquals(1.0f, tier1.getKnockback());
    assertEquals(1.5f, tier2.getKnockback());
    assertEquals(2.0f, tier3.getKnockback());

    assertEquals(2.0f, tier1.getRange());
    assertEquals(2.5f, tier2.getRange());
    assertEquals(3.0f, tier3.getRange());
  }

  @Test
  void shouldReturnCorrectBowAdditionalStatsForEachTier() {
    WeaponStats tier1 = WeaponTier.TIER_1.getStats(WeaponType.BOW);
    WeaponStats tier2 = WeaponTier.TIER_2.getStats(WeaponType.BOW);
    WeaponStats tier3 = WeaponTier.TIER_3.getStats(WeaponType.BOW);

    assertEquals(1.0f, tier1.getAttackSpeed());
    assertEquals(1.2f, tier2.getAttackSpeed());
    assertEquals(1.5f, tier3.getAttackSpeed());

    assertEquals(0.5f, tier1.getKnockback());
    assertEquals(0.75f, tier2.getKnockback());
    assertEquals(1.0f, tier3.getKnockback());

    assertEquals(8.0f, tier1.getRange());
    assertEquals(10.0f, tier2.getRange());
    assertEquals(12.0f, tier3.getRange());
  }

  @Test
  void shouldReturnCorrectProjectileCountForEachTier() {
    assertEquals(1, WeaponTier.TIER_1.getStats(WeaponType.SWORD).getProjectileCount());
    assertEquals(1, WeaponTier.TIER_1.getStats(WeaponType.BOW).getProjectileCount());
    assertEquals(1, WeaponTier.TIER_1.getStats(WeaponType.DAGGER).getProjectileCount());

    assertEquals(1, WeaponTier.TIER_2.getStats(WeaponType.SWORD).getProjectileCount());
    assertEquals(5, WeaponTier.TIER_2.getStats(WeaponType.BOW).getProjectileCount());
    assertEquals(1, WeaponTier.TIER_2.getStats(WeaponType.DAGGER).getProjectileCount());

    assertEquals(1, WeaponTier.TIER_3.getStats(WeaponType.SWORD).getProjectileCount());
    assertEquals(10, WeaponTier.TIER_3.getStats(WeaponType.BOW).getProjectileCount());
    assertEquals(1, WeaponTier.TIER_3.getStats(WeaponType.DAGGER).getProjectileCount());
  }
}
