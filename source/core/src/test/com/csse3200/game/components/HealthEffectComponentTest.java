package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class HealthEffectComponentTest {

  @Test
  void shouldApplyPoisonOverTime() {
    int startingHealth = 100;
    int time = 5;
    int healthChange = -50;

    Entity entity = createEntityWithEffect(startingHealth, time, healthChange);
    HealthEffectComponent effect = entity.getComponent(HealthEffectComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    for (int i = 0; i < 5; i++) {
      effect.triggerUpdate();
      assertEquals(startingHealth + (healthChange / time * (i + 1)), stats.getHealth());
    }

    assertEquals(50, stats.getHealth());
  }

  @Test
  void shouldApplyRegenOverTime() {
    int startingHealth = 50;
    int time = 5;
    int healthChange = 50;

    Entity entity = createEntityWithEffect(startingHealth, time, healthChange);
    HealthEffectComponent effect = entity.getComponent(HealthEffectComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    for (int i = 0; i < 5; i++) {
      effect.triggerUpdate();
      assertEquals(startingHealth + (healthChange / time * (i + 1)), stats.getHealth());
    }

    assertEquals(100, stats.getHealth());
  }

  @Test
  void shouldApplyEachTickCorrectlyWithRemainder() {
    int startingHealth = 100;
    int time = 3;
    int healthChange = 10; // 10 / 3 = 3 per tick, remainder folded into last tick
    // 10 health over 3 ticks does not divide evenly (3, 3, 4)
    int healthPerTick = healthChange / time;

    Entity entity = createEntityWithEffect(startingHealth, time, healthChange);
    HealthEffectComponent effect = entity.getComponent(HealthEffectComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    for (int i = 0; i < time; i++) {
      effect.triggerUpdate();

      int ticksSoFar = i + 1;
      int expected;
      if (ticksSoFar == time) {
        // Last tick: all remaining health should now be applied, so total is exact
        expected = startingHealth + healthChange;
      } else {
        expected = startingHealth + (healthPerTick * ticksSoFar);
      }

      assertEquals(expected, stats.getHealth());
    }

    assertEquals(startingHealth + healthChange, stats.getHealth());
  }

  @Test
  void shouldApplyInstantlyWhenTimeIsZero() {
    // health should already be applied straight after create(), before any update()
    Entity entity = createEntityWithEffect(100, 0, -30);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    assertEquals(70, stats.getHealth());
  }

  @Test
  void shouldDisableAfterInstantEffect() {
    Entity entity = createEntityWithEffect(100, 0, -30);
    HealthEffectComponent effect = entity.getComponent(HealthEffectComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    assertEquals(70, stats.getHealth());

    // Further updates should do nothing further
    effect.triggerUpdate();
    effect.triggerUpdate();

    assertEquals(70, stats.getHealth());
  }

  @Test
  void shouldDisableOnceEffectFinishes() {
    Entity entity = createEntityWithEffect(100, 2, -20);
    HealthEffectComponent effect = entity.getComponent(HealthEffectComponent.class);
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);

    effect.triggerUpdate();
    effect.triggerUpdate();
    // Effect should be finished and fully applied now
    assertEquals(80, stats.getHealth());

    // Extra ticks after completion should not apply more damage
    effect.triggerUpdate();
    assertEquals(80, stats.getHealth());
  }

  @Test
  void shouldThrowExceptionForNegativeTime() {
    assertThrows(IllegalArgumentException.class, () -> new HealthEffectComponent(-1, 10));
  }

  Entity createEntityWithEffect(int startingHealth, int time, int health) {
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(startingHealth, 0))
            .addComponent(new HealthEffectComponent(time, health));
    entity.create();
    return entity;
  }
}
