package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HealEffectTest {

  /**
   * Builds an entity that can be healed.
   *
   * @param health starting health
   * @param maxHealth health cap declared by the use component
   * @return a created entity
   */
  private Entity makeTarget(int health, int maxHealth) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 10))
            .addComponent(new ConsumableUseComponent(maxHealth));
    target.create();
    return target;
  }

  /** Acceptance criterion: healing below max HP increases health by the configured amount. */
  @Test
  void shouldHealByConfiguredAmountBelowMaxHealth() {
    Entity target = makeTarget(50, 100);

    assertTrue(new HealEffect(25).apply(target));
    assertEquals(75, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  /** Acceptance criterion: healing is capped at max health. */
  @Test
  void shouldCapHealingAtMaxHealth() {
    Entity target = makeTarget(90, 100);

    assertTrue(new HealEffect(50).apply(target));
    assertEquals(100, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  /** Acceptance criterion: at full HP the potion has no effect and reports that it was unused. */
  @Test
  void shouldDoNothingAtFullHealth() {
    Entity target = makeTarget(100, 100);

    assertFalse(new HealEffect(25).apply(target));
    assertEquals(100, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldHealUncappedWhenNoMaxHealthDeclared() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(50, 10));
    target.create();

    assertTrue(new HealEffect(25).apply(target));
    assertEquals(75, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotApplyWithoutCombatStats() {
    Entity target = new Entity().addComponent(new ConsumableUseComponent(100));
    target.create();

    assertFalse(new HealEffect(25).apply(target));
    assertFalse(new HealEffect(25).apply(null));
  }

  @Test
  void shouldRejectNonPositiveHealAmount() {
    assertThrows(IllegalArgumentException.class, () -> new HealEffect(0));
    assertThrows(IllegalArgumentException.class, () -> new HealEffect(-5));
  }
}
