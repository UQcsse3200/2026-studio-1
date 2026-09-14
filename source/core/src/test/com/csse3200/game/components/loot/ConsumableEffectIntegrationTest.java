package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the generated potions against a player-shaped entity, so a potion that passes its own unit
 * tests but has nothing to apply itself to is caught here.
 */
@ExtendWith(GameExtension.class)
class ConsumableEffectIntegrationTest {
  private GameTime time;
  private Entity player;
  private ConsumableGenerator generator;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new ConsumableUseComponent(100))
            .addComponent(new PlayerBuffComponent())
            .addComponent(new PlayerRegenComponent());
    player.create();
    player.getComponent(CombatStatsComponent.class).setHealth(50);

    generator = new ConsumableGenerator();
  }

  /** Every potion the generator can produce must actually do something to the player. */
  @Test
  void shouldApplyEveryPotionTypeToThePlayer() {
    for (ConsumableType type : ConsumableType.values()) {
      ConsumableItem item = generator.generateConsumable(type, 1);
      assertTrue(item.use(player), type + " should have an effect on the player");
    }
  }

  /** The health potion restores health immediately and stops at maximum health. */
  @Test
  void shouldHealImmediatelyWithAHealthPotion() {
    ConsumableItem potion = generator.generateConsumable(ConsumableType.HEALTH_POTION, 1);

    assertTrue(potion.use(player), "a health potion below full health should be usable");
    assertEquals(
        75,
        player.getComponent(CombatStatsComponent.class).getHealth(),
        "a tier 1 health potion should restore 25 health straight away");
  }

  /** The regeneration potion heals gradually and stops when its duration ends. */
  @Test
  void shouldHealGraduallyWithARegenerationPotion() {
    ConsumableItem potion = generator.generateConsumable(ConsumableType.REGENERATION, 1);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    assertTrue(potion.use(player), "a regeneration potion should be usable");
    assertEquals(50, stats.getHealth(), "regeneration should not heal the moment it is drunk");

    when(time.getTime()).thenReturn(1000L);
    player.update();
    assertEquals(55, stats.getHealth(), "the first tick should restore 5 health");

    when(time.getTime()).thenReturn(6000L);
    player.update();
    assertEquals(80, stats.getHealth(), "six ticks of 5 should have healed 30 health in total");

    when(time.getTime()).thenReturn(20000L);
    player.update();
    assertEquals(80, stats.getHealth(), "no healing should happen after the potion runs out");
  }

  /** The resistance potion reduces incoming damage until it expires. */
  @Test
  void shouldReduceIncomingDamageWithAResistancePotion() {
    ConsumableItem potion = generator.generateConsumable(ConsumableType.RESISTANCE, 1);
    PlayerBuffComponent buffs = player.getComponent(PlayerBuffComponent.class);

    assertTrue(potion.use(player), "a resistance potion should be usable");
    assertEquals(
        0.7f,
        buffs.getIncomingDamageMultiplier(),
        0.0001f,
        "a tier 1 resistance potion should cut incoming damage to 70%");
    assertTrue(buffs.hasBuff(BuffStat.RESISTANCE), "the resistance buff should be active");

    when(time.getTime()).thenReturn(10000L);
    player.update();
    assertEquals(
        1f,
        buffs.getIncomingDamageMultiplier(),
        "incoming damage should be back to normal once the potion expires");
  }

  /** The strength potion multiplies weapon damage through the buff component. */
  @Test
  void shouldMultiplyDamageWithAStrengthPotion() {
    ConsumableItem potion = generator.generateConsumable(ConsumableType.DAMAGE_BUFF, 1);
    PlayerBuffComponent buffs = player.getComponent(PlayerBuffComponent.class);

    assertTrue(potion.use(player), "a strength potion should be usable");
    assertEquals(
        1.5f,
        buffs.getDamageMultiplier(),
        0.0001f,
        "a tier 1 strength potion should multiply damage by 1.5");

    when(time.getTime()).thenReturn(10000L);
    player.update();
    assertEquals(
        1f, buffs.getDamageMultiplier(), "damage should be back to normal once the potion expires");
  }
}
