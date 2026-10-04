package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link WeaponStatsPreset} (one weapon type's stat block for one tier) and the presets that
 * {@link WeaponTier} hands out. Pure unit tests: no services or entities are needed.
 */
class WeaponStatsPresetTest {
  private static final float DELTA = 1e-6f;

  // ---------- constructor and getters ----------

  @Test
  void shouldStoreEveryValueGivenToTheConstructor() {
    WeaponStatsPreset preset = new WeaponStatsPreset(2, 14, 1.4f, 0.8f, 3.5f, 3);

    assertEquals(2, preset.getTier(), "tier");
    assertEquals(14, preset.getDamage(), "damage");
    assertEquals(1.4f, preset.getAttackSpeed(), DELTA, "attack speed");
    assertEquals(0.8f, preset.getKnockback(), DELTA, "knockback");
    assertEquals(3.5f, preset.getRange(), DELTA, "range");
    assertEquals(3, preset.getProjectileCount(), "projectile count");
  }

  @Test
  void shouldBeUsableThroughTheWeaponStatsInterface() {
    WeaponStats stats = new WeaponStatsPreset(1, 10, 1f, 1f, 2f, 1);

    assertEquals(10, stats.getDamage());
    assertEquals(1, stats.getProjectileCount());
  }

  @Test
  void shouldReturnTheSameValueOnEveryCall() {
    WeaponStatsPreset preset = new WeaponStatsPreset(1, 10, 1.5f, 2f, 3f, 4);

    assertEquals(preset.getDamage(), preset.getDamage());
    assertEquals(preset.getAttackSpeed(), preset.getAttackSpeed(), DELTA);
    assertEquals(preset.getKnockback(), preset.getKnockback(), DELTA);
    assertEquals(preset.getRange(), preset.getRange(), DELTA);
    assertEquals(preset.getProjectileCount(), preset.getProjectileCount());
  }

  @Test
  void shouldKeepTwoPresetsIndependent() {
    WeaponStatsPreset first = new WeaponStatsPreset(1, 10, 1f, 1f, 2f, 1);
    WeaponStatsPreset second = new WeaponStatsPreset(3, 30, 3f, 3f, 6f, 9);

    assertEquals(10, first.getDamage(), "building a second preset must not change the first");
    assertEquals(30, second.getDamage());
    assertNotSame(first, second);
  }

  // The constructor does no validation today. These two tests document that on purpose, so adding
  // validation later is a deliberate change that updates them.

  @Test
  void documentsThatZeroValuesAreAcceptedToday() {
    WeaponStatsPreset preset = new WeaponStatsPreset(0, 0, 0f, 0f, 0f, 0);

    assertEquals(0, preset.getDamage());
    assertEquals(0, preset.getProjectileCount());
  }

  @Test
  void documentsThatNegativeValuesAreAcceptedToday() {
    WeaponStatsPreset preset = new WeaponStatsPreset(-1, -5, -1f, -1f, -1f, -1);

    assertEquals(-5, preset.getDamage());
    assertEquals(-1f, preset.getRange(), DELTA);
  }

  // ---------- the presets WeaponTier hands out ----------

  @Test
  void shouldGiveEveryRealWeaponTypeAPresetAtEveryTier() {
    for (WeaponTier tier : WeaponTier.values()) {
      for (WeaponType type :
          new WeaponType[] {WeaponType.SWORD, WeaponType.BOW, WeaponType.DAGGER, WeaponType.AXE}) {
        WeaponStatsPreset preset = tier.getStats(type);

        assertTrue(preset.getDamage() > 0, tier + " " + type + " damage must be positive");
        assertTrue(preset.getAttackSpeed() > 0, tier + " " + type + " attack speed");
        assertTrue(preset.getKnockback() > 0, tier + " " + type + " knockback");
        assertTrue(preset.getRange() > 0, tier + " " + type + " range");
        assertTrue(preset.getProjectileCount() >= 1, tier + " " + type + " projectile count");
      }
    }
  }

  @Test
  void shouldReturnTheSamePresetObjectForTheSameTierAndType() {
    assertSame(
        WeaponTier.TIER_2.getStats(WeaponType.AXE), WeaponTier.TIER_2.getStats(WeaponType.AXE));
  }

  @Test
  void shouldGiveDifferentTypesDifferentPresetsAtOneTier() {
    assertNotSame(
        WeaponTier.TIER_1.getStats(WeaponType.SWORD), WeaponTier.TIER_1.getStats(WeaponType.AXE));
    assertNotEquals(
        WeaponTier.TIER_1.getStats(WeaponType.SWORD).getDamage(),
        WeaponTier.TIER_1.getStats(WeaponType.AXE).getDamage());
  }

  @Test
  void shouldRaiseDamageWithTierForEveryNonBowWeapon() {
    for (WeaponType type : new WeaponType[] {WeaponType.SWORD, WeaponType.DAGGER, WeaponType.AXE}) {
      int one = WeaponTier.TIER_1.getStats(type).getDamage();
      int two = WeaponTier.TIER_2.getStats(type).getDamage();
      int three = WeaponTier.TIER_3.getStats(type).getDamage();

      assertTrue(one < two && two < three, type + " damage must rise with tier");
    }
  }

  @Test
  void shouldLowerBowDamagePerArrowButRaiseTheTotalWithTier() {
    WeaponStatsPreset one = WeaponTier.TIER_1.getStats(WeaponType.BOW);
    WeaponStatsPreset two = WeaponTier.TIER_2.getStats(WeaponType.BOW);
    WeaponStatsPreset three = WeaponTier.TIER_3.getStats(WeaponType.BOW);

    assertTrue(one.getDamage() > two.getDamage() && two.getDamage() > three.getDamage());
    int totalOne = one.getDamage() * one.getProjectileCount();
    int totalTwo = two.getDamage() * two.getProjectileCount();
    int totalThree = three.getDamage() * three.getProjectileCount();
    assertEquals(7, totalOne);
    assertEquals(20, totalTwo);
    assertEquals(30, totalThree);
    assertTrue(totalOne < totalTwo && totalTwo < totalThree);
  }

  @Test
  void shouldGiveTheBowOneFiveAndTenProjectilesAndEveryOtherWeaponOne() {
    assertEquals(1, WeaponTier.TIER_1.getStats(WeaponType.BOW).getProjectileCount());
    assertEquals(5, WeaponTier.TIER_2.getStats(WeaponType.BOW).getProjectileCount());
    assertEquals(10, WeaponTier.TIER_3.getStats(WeaponType.BOW).getProjectileCount());
    for (WeaponTier tier : WeaponTier.values()) {
      for (WeaponType type :
          new WeaponType[] {WeaponType.SWORD, WeaponType.DAGGER, WeaponType.AXE}) {
        assertEquals(1, tier.getStats(type).getProjectileCount(), tier + " " + type);
      }
    }
  }

  @Test
  void shouldNeverLowerAttackSpeedKnockbackOrRangeAsTierRises() {
    for (WeaponType type :
        new WeaponType[] {WeaponType.SWORD, WeaponType.BOW, WeaponType.DAGGER, WeaponType.AXE}) {
      WeaponStatsPreset one = WeaponTier.TIER_1.getStats(type);
      WeaponStatsPreset two = WeaponTier.TIER_2.getStats(type);
      WeaponStatsPreset three = WeaponTier.TIER_3.getStats(type);

      assertTrue(one.getAttackSpeed() <= two.getAttackSpeed(), type + " speed 1 to 2");
      assertTrue(two.getAttackSpeed() <= three.getAttackSpeed(), type + " speed 2 to 3");
      assertTrue(one.getKnockback() < two.getKnockback(), type + " knockback 1 to 2");
      assertTrue(two.getKnockback() < three.getKnockback(), type + " knockback 2 to 3");
      assertTrue(one.getRange() < two.getRange(), type + " range 1 to 2");
      assertTrue(two.getRange() < three.getRange(), type + " range 2 to 3");
    }
  }

  @Test
  void shouldGiveTheBowTheLongestRangeAndTheDaggerTheShortestAtEveryTier() {
    for (WeaponTier tier : WeaponTier.values()) {
      float bow = tier.getStats(WeaponType.BOW).getRange();
      float dagger = tier.getStats(WeaponType.DAGGER).getRange();
      for (WeaponType melee : new WeaponType[] {WeaponType.SWORD, WeaponType.AXE}) {
        float range = tier.getStats(melee).getRange();
        assertTrue(bow > range, tier + ": bow must out-range " + melee);
        assertTrue(dagger < range, tier + ": dagger must be shorter than " + melee);
      }
    }
  }

  @Test
  void shouldRejectANullWeaponTypeWhenAskingForStats() {
    assertThrows(IllegalArgumentException.class, () -> WeaponTier.TIER_1.getStats(null));
  }

  @Test
  void shouldRejectTheNaturalWeaponTypeWhenAskingForStats() {
    assertThrows(
        IllegalArgumentException.class, () -> WeaponTier.TIER_1.getStats(WeaponType.NATURAL));
  }

  /**
   * Known inconsistency, found while writing this test: {@code WeaponTier} builds every preset with
   * a tier argument of 1, so a tier-2 or tier-3 preset reports {@code getTier() == 1}. Nothing
   * reads that value today ({@code WeaponItem} takes its tier from the {@code WeaponTier} itself),
   * so it is harmless, but it is wrong. Enable this test if the owner of {@code WeaponTier} fixes
   * it.
   */
  @Test
  void shouldReportTheTierThePresetBelongsTo() {
    assertEquals(2, WeaponTier.TIER_2.getStats(WeaponType.SWORD).getTier());
    assertEquals(3, WeaponTier.TIER_3.getStats(WeaponType.AXE).getTier());
  }
}
