package com.csse3200.game.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Loads configs/NPCs.json through the real file loader and checks the new enemies' numbers. A typo
 * in the JSON would otherwise silently fall back to a default (for example giving a boss 1 health).
 */
@ExtendWith(GameExtension.class)
class NPCConfigsLoadingTest {
  private NPCConfigs configs;

  @BeforeEach
  void setUp() {
    configs = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
  }

  @Test
  void shouldLoadEveryNestedConfigTheNewEnemiesNeed() {
    assertNotNull(configs.medusa.melee);
    assertNotNull(configs.medusa.ranged);
    assertNotNull(configs.medusa.petrify);
    assertNotNull(configs.medusa.pacing);
    assertNotNull(configs.cerberus.melee);
    assertNotNull(configs.zeus.melee);
    assertNotNull(configs.zeus.ranged);
    assertNotNull(configs.harpy.melee);
    assertNotNull(configs.rangedHarpy.ranged);
  }

  @Test
  void shouldGiveEveryBossHealthOneHundred() {
    assertEquals(100, configs.medusa.health);
    assertEquals(100, configs.cerberus.health);
    assertEquals(100, configs.zeus.health);
  }

  @Test
  void shouldGiveEveryNewEnemyPositiveHealthAndAttack() {
    assertTrue(configs.medusa.health > 0 && configs.medusa.baseAttack > 0);
    assertTrue(configs.cerberus.health > 0 && configs.cerberus.baseAttack > 0);
    assertTrue(configs.zeus.health > 0 && configs.zeus.baseAttack > 0);
    assertTrue(configs.harpy.health > 0 && configs.harpy.baseAttack > 0);
    assertTrue(configs.rangedHarpy.health > 0 && configs.rangedHarpy.baseAttack > 0);
  }

  @Test
  void shouldKeepMeleeRangeBelowRangedRangeForMedusaAndZeus() {
    assertTrue(configs.medusa.melee.range < configs.medusa.ranged.range, "Medusa");
    assertTrue(configs.zeus.melee.range < configs.zeus.ranged.range, "Zeus");
  }

  @Test
  void shouldKeepMedusaCooldownAtLeastTwiceThePetrifyDuration() {
    assertEquals(3.0f, configs.medusa.petrify.duration, 1e-6f);
    assertTrue(configs.medusa.ranged.cooldown >= 2f * configs.medusa.petrify.duration);
  }

  @Test
  void shouldKeepEveryNaturalWindupBelowItsCooldown() {
    assertTrue(configs.medusa.melee.windup < configs.medusa.melee.cooldown, "Medusa melee");
    assertTrue(configs.medusa.ranged.windup < configs.medusa.ranged.cooldown, "Medusa gaze");
    assertTrue(configs.cerberus.melee.windup < configs.cerberus.melee.cooldown, "Cerberus");
    assertTrue(configs.zeus.ranged.windup < configs.zeus.ranged.cooldown, "Zeus lightning");
  }

  @Test
  void shouldGiveCerberusTheShortestMeleeCooldownOfAnyEnemy() {
    float cerberus = configs.cerberus.melee.cooldown;
    assertTrue(cerberus < configs.skeleton.melee.cooldown);
    assertTrue(cerberus < configs.minotaur.melee.cooldown);
    assertTrue(cerberus < configs.cyclops.melee.cooldown);
    assertTrue(cerberus < configs.medusa.melee.cooldown);
    assertTrue(cerberus < configs.zeus.melee.cooldown);
    assertTrue(cerberus < configs.harpy.melee.cooldown);
  }

  @Test
  void shouldAimOnlyTheEnemiesThatNeedToShootUpOrDown() {
    assertTrue(configs.zeus.ranged.aimed);
    assertTrue(configs.rangedHarpy.ranged.aimed);
    assertFalse(configs.medusa.ranged.aimed);
    assertFalse(configs.rangedSkeleton.ranged.aimed);
    assertFalse(configs.centaur.ranged.aimed);
  }

  @Test
  void shouldFallBackToTheDefaultWindupWhenTheJsonHasNone() {
    // The Cyclops melee block has no "windup" key, so it takes the class default.
    assertEquals(0.4f, configs.cyclops.melee.windup, 1e-6f);
    assertEquals(1.1f, configs.cyclops.rock.windup, 1e-6f);
    assertEquals(0.5f, configs.cyclops.laser.windup, 1e-6f);
  }

  @Test
  void shouldGiveBothHarpiesANonNegativeFlightDamping() {
    assertEquals(2.0f, configs.harpy.flightDamping, 1e-6f);
    assertEquals(2.0f, configs.rangedHarpy.flightDamping, 1e-6f);
  }

  @Test
  void shouldKeepTheExistingEnemiesNumbersUnchanged() {
    assertEquals(30, configs.skeleton.health);
    assertEquals(20, configs.rangedSkeleton.health);
    assertEquals(40, configs.minotaur.health);
    assertEquals(40, configs.centaur.health);
    assertEquals(50, configs.cyclops.health);
  }
}
