package com.csse3200.game.entities.configs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.configs.enemies.MedusaConfig;
import com.csse3200.game.entities.configs.enemies.ZeusConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Loads the real {@code configs/NPCs.json} (the same file {@link
 * com.csse3200.game.entities.factories.NPCFactory} reads from) and checks it against the Sprint 3
 * "Mini-Boss and Flying Enemies" ticket's cross-cutting team design rules:
 *
 * <ol>
 *   <li>a mini-boss's health is a boss-level value near 100;
 *   <li>an enemy's melee range is below its ranged range;
 *   <li>an attack's effect cooldown is at least twice the effect's duration.
 * </ol>
 *
 * <p>This is executable documentation, not a test of any one class's logic - it exists so a future
 * balance tweak to {@code NPCs.json} that breaks one of these rules fails a test instead of only
 * being caught (or missed) in review.
 */
@ExtendWith(GameExtension.class)
class EnemyDesignRulesTest {
  /**
   * Ticks-to-seconds conversion for effect durations (petrifyTicks/freezeTicks). Matches the
   * "about 60 per second" assumption already documented on {@link
   * com.csse3200.game.entities.factories.ArrowFactory#createLightning} and {@link
   * com.csse3200.game.components.attacks.RangedAttackComponent#getLightningFreezeTicks}.
   */
  private static final float TICKS_PER_SECOND = 60f;

  private static final float BOSS_HEALTH_TOLERANCE = 15f;

  private NPCConfigs configs;

  @BeforeEach
  void setUp() {
    configs = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
    assertNotNull(configs, "configs/NPCs.json failed to load");
  }

  @Test
  void medusaHealthShouldBeNearBossLevel() {
    assertNear(100f, configs.medusa.health, BOSS_HEALTH_TOLERANCE, "Medusa health");
  }

  @Test
  void cerberusHealthShouldBeNearBossLevel() {
    assertNear(100f, configs.cerberus.health, BOSS_HEALTH_TOLERANCE, "Cerberus health");
  }

  @Test
  void zeusHealthShouldBeNearBossLevel() {
    assertNear(100f, configs.zeus.health, BOSS_HEALTH_TOLERANCE, "Zeus health");
  }

  @Test
  void medusaMeleeRangeShouldBeBelowGazeRange() {
    MedusaConfig config = configs.medusa;
    assertTrue(
        config.melee.range < config.ranged.range,
        "Medusa's melee range ("
            + config.melee.range
            + ") must be below her gaze range ("
            + config.ranged.range
            + ")");
  }

  @Test
  void zeusMeleeRangeShouldBeBelowLightningRange() {
    ZeusConfig config = configs.zeus;
    assertTrue(
        config.melee.range < config.ranged.range,
        "Zeus's melee range ("
            + config.melee.range
            + ") must be below his lightning range ("
            + config.ranged.range
            + ")");
  }

  @Test
  void medusaGazeCooldownShouldBeAtLeastTwicePetrifyDuration() {
    MedusaConfig config = configs.medusa;
    float petrifySeconds = config.petrifyTicks / TICKS_PER_SECOND;
    assertTrue(
        config.ranged.cooldown >= 2 * petrifySeconds,
        "Medusa's gaze cooldown ("
            + config.ranged.cooldown
            + "s) must be at least twice the petrify duration ("
            + petrifySeconds
            + "s)");
  }

  @Test
  void zeusLightningCooldownShouldBeAtLeastTwiceFreezeDuration() {
    ZeusConfig config = configs.zeus;
    float freezeSeconds = config.freezeTicks / TICKS_PER_SECOND;
    assertTrue(
        config.ranged.cooldown >= 2 * freezeSeconds,
        "Zeus's lightning cooldown ("
            + config.ranged.cooldown
            + "s) must be at least twice the freeze duration ("
            + freezeSeconds
            + "s)");
  }

  private void assertNear(float expected, float actual, float tolerance, String label) {
    assertTrue(
        Math.abs(expected - actual) <= tolerance,
        label + " (" + actual + ") expected to be within " + tolerance + " of " + expected);
  }
}
