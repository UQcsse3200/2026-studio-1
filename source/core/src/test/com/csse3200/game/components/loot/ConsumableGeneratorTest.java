package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.entities.configs.ConsumableConfigs;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableGeneratorTest {
  private ConsumableGenerator generator;

  @BeforeEach
  void setUp() {
    ConsumableConfigs configs = new ConsumableConfigs();

    configs.healthPotion.name = "Health Potion";
    configs.healthPotion.healAmount = 25;
    configs.healthPotion.maxQuantity = 9;

    configs.damageBuff.name = "Damage Potion";
    configs.damageBuff.magnitude = 1.5f;
    configs.damageBuff.durationSeconds = 10f;
    configs.damageBuff.maxQuantity = 9;

    configs.speedBuff.name = "Speed Potion";
    configs.speedBuff.magnitude = 1.5f;
    configs.speedBuff.durationSeconds = 10f;
    configs.speedBuff.maxQuantity = 9;

    configs.regeneration.name = "Regeneration Potion";
    configs.regeneration.healAmount = 5;
    configs.regeneration.durationSeconds = 5f;
    configs.regeneration.maxQuantity = 9;

    configs.resistance.name = "Resistance Potion";
    configs.resistance.magnitude = 0.8f;
    configs.resistance.durationSeconds = 10f;
    configs.resistance.maxQuantity = 9;

    generator = new ConsumableGenerator(configs);
  }

  /** Acceptance criterion: heal amount is configurable and scales per tier. */
  @Test
  void shouldScaleHealAmountWithTier() {
    HealEffect tier1 =
        (HealEffect) generator.generateConsumable(ConsumableType.HEALTH_POTION, 1).getEffect();
    HealEffect tier3 =
        (HealEffect) generator.generateConsumable(ConsumableType.HEALTH_POTION, 3).getEffect();

    assertEquals(25, tier1.getHealAmount());
    assertEquals(75, tier3.getHealAmount());
  }

  /** Acceptance criterion: buff magnitude and duration are configurable per tier. */
  @Test
  void shouldScaleBuffMagnitudeWithTier() {
    BuffEffect tier1 =
        (BuffEffect) generator.generateConsumable(ConsumableType.DAMAGE_BUFF, 1).getEffect();
    BuffEffect tier2 =
        (BuffEffect) generator.generateConsumable(ConsumableType.DAMAGE_BUFF, 2).getEffect();

    assertEquals(BuffStat.DAMAGE, tier1.getStat());
    assertEquals(1.5f, tier1.getMagnitude());
    assertEquals(10f, tier1.getDurationSeconds());
    assertEquals(2f, tier2.getMagnitude());
  }

  @Test
  void shouldGenerateSpeedBuffAgainstSpeedStat() {
    BuffEffect effect =
        (BuffEffect) generator.generateConsumable(ConsumableType.SPEED_BUFF, 1).getEffect();

    assertEquals(BuffStat.SPEED, effect.getStat());
  }

  @Test
  void shouldGenerateEveryTypeWithCorrectItemMetadata() {
    for (ConsumableType type : ConsumableType.values()) {
      ConsumableItem item = generator.generateConsumable(type, 1);

      assertEquals(ItemType.CONSUMABLE, item.getItemType());
      assertEquals(type, item.getConsumableType());
      assertEquals(type.getTexturePath(), item.getTexturePath());
      assertEquals(1, item.getQuantity());
      assertEquals(9, item.getMaxQuantity());
      assertNotNull(item.getEffect());
    }
  }

  @Test
  void shouldUseCorrectEffectTypePerConsumable() {
    assertInstanceOf(
        HealEffect.class,
        generator.generateConsumable(ConsumableType.HEALTH_POTION, 1).getEffect());
    assertInstanceOf(
        BuffEffect.class, generator.generateConsumable(ConsumableType.DAMAGE_BUFF, 1).getEffect());
    assertInstanceOf(
        BuffEffect.class, generator.generateConsumable(ConsumableType.SPEED_BUFF, 1).getEffect());
  }

  /** Higher tiers get distinct names so a weak potion never stacks with a strong one. */
  @Test
  void shouldQualifyNameAboveTierOne() {
    assertEquals(
        "Health Potion", generator.generateConsumable(ConsumableType.HEALTH_POTION, 1).getName());
    assertEquals(
        "Health Potion (Tier 3)",
        generator.generateConsumable(ConsumableType.HEALTH_POTION, 3).getName());
  }

  @Test
  void shouldRejectInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> generator.generateConsumable(null, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> generator.generateConsumable(ConsumableType.HEALTH_POTION, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> generator.generateConsumable(ConsumableType.HEALTH_POTION, -2));
    assertThrows(IllegalArgumentException.class, () -> new ConsumableGenerator(null));
  }

  /** Verifies configs/consumables.json parses into usable values. */
  @Test
  void shouldGenerateFromConfigFile() {
    ConsumableGenerator fromFile = new ConsumableGenerator();

    ConsumableItem potion = fromFile.generateConsumable(ConsumableType.HEALTH_POTION, 1);
    assertEquals("Health Potion", potion.getName());
    assertTrue(((HealEffect) potion.getEffect()).getHealAmount() > 0);

    BuffEffect damage =
        (BuffEffect) fromFile.generateConsumable(ConsumableType.DAMAGE_BUFF, 1).getEffect();
    assertTrue(damage.getMagnitude() > 1f);
    assertTrue(damage.getDurationSeconds() > 0f);
  }

  /** Acceptance criterion: regeneration heals per tick, and a higher tier heals faster. */
  @Test
  void shouldScaleRegenerationHealPerTickWithTier() {
    RegenerationEffect tier1 =
        (RegenerationEffect)
            generator.generateConsumable(ConsumableType.REGENERATION, 1).getEffect();
    RegenerationEffect tier3 =
        (RegenerationEffect)
            generator.generateConsumable(ConsumableType.REGENERATION, 3).getEffect();

    assertEquals(5, tier1.getHealPerTick());
    assertEquals(15, tier3.getHealPerTick());
    assertEquals(5f, tier1.getDurationSeconds());
  }

  /** Acceptance criterion: resistance reduces incoming damage, and a higher tier reduces more. */
  @Test
  void shouldMakeResistanceStrongerAtHigherTiers() {
    BuffEffect tier1 =
        (BuffEffect) generator.generateConsumable(ConsumableType.RESISTANCE, 1).getEffect();
    BuffEffect tier2 =
        (BuffEffect) generator.generateConsumable(ConsumableType.RESISTANCE, 2).getEffect();

    assertEquals(BuffStat.RESISTANCE, tier1.getStat());
    assertEquals(0.8f, tier1.getMagnitude(), 0.0001f);
    assertEquals(0.6f, tier2.getMagnitude(), 0.0001f);
  }

  /** A high enough tier must not make the player immune, or drive the multiplier negative. */
  @Test
  void shouldClampResistanceAboveZero() {
    BuffEffect tier9 =
        (BuffEffect) generator.generateConsumable(ConsumableType.RESISTANCE, 9).getEffect();

    assertTrue(tier9.getMagnitude() > 0f);
    assertTrue(tier9.getMagnitude() < 0.5f);
  }

  /** Every consumable type must be generatable, so a new type cannot be half-added. */
  @Test
  void shouldGenerateEveryConsumableType() {
    for (ConsumableType type : ConsumableType.values()) {
      ConsumableItem item = generator.generateConsumable(type, 1);
      assertNotNull(item.getEffect());
      assertNotNull(item.getTexturePath());
    }
  }
}
