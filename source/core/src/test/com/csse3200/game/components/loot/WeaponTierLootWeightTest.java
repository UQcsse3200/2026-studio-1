package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WeaponTierLootWeightTest {

  @Test
  void shouldHaveCorrectLootWeights() {
    assertEquals(70, WeaponTier.TIER_1.getLootWeight());
    assertEquals(25, WeaponTier.TIER_2.getLootWeight());
    assertEquals(5, WeaponTier.TIER_3.getLootWeight());
  }

  @Test
  void higherTiersShouldBeRarer() {
    assertTrue(
        WeaponTier.TIER_1.getLootWeight() > WeaponTier.TIER_2.getLootWeight());

    assertTrue(
        WeaponTier.TIER_2.getLootWeight() > WeaponTier.TIER_3.getLootWeight());
  }

  @Test
  void lootWeightsShouldTotal100() {
    int totalWeight =
        WeaponTier.TIER_1.getLootWeight()
            + WeaponTier.TIER_2.getLootWeight()
            + WeaponTier.TIER_3.getLootWeight();

    assertEquals(100, totalWeight);
  }
}