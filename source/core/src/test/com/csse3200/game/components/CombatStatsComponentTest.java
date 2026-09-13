package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CombatStatsComponentTest {
  @Test
  void shouldSetGetHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(100, combat.getHealth());

    combat.setHealth(150);
    assertEquals(150, combat.getHealth());

    combat.setHealth(-50);
    assertEquals(0, combat.getHealth());
  }

  @Test
  void shouldCheckIsDead() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertFalse(combat.isDead());

    combat.setHealth(0);
    assertTrue(combat.isDead());
  }

  @Test
  void shouldAddHealth() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    combat.addHealth(-500);
    assertEquals(0, combat.getHealth());

    combat.addHealth(100);
    combat.addHealth(-20);
    assertEquals(80, combat.getHealth());
  }

  @Test
  void shouldSetGetBaseAttack() {
    CombatStatsComponent combat = new CombatStatsComponent(100, 20);
    assertEquals(20, combat.getBaseAttack());

    combat.setBaseAttack(150);
    assertEquals(150, combat.getBaseAttack());

    combat.setBaseAttack(-50);
    assertEquals(150, combat.getBaseAttack());
  }

  @Test
  void shouldTriggerDeathEventWhenHealthReachesZero() {
    Entity entity = new Entity();
    CombatStatsComponent stats = new CombatStatsComponent(10, 5);
    entity.addComponent(stats);
    entity.create();

    int[] deathCount = {0};
    entity.getEvents().addListener("death", () -> deathCount[0]++);

    stats.setHealth(0);

    assertEquals(1, deathCount[0]);
  }

  @Test
  void shouldNotTriggerDeathEventWhileHealthPositive() {
    Entity entity = new Entity();
    CombatStatsComponent stats = new CombatStatsComponent(10, 5);
    entity.addComponent(stats);
    entity.create();

    int[] deathCount = {0};
    entity.getEvents().addListener("death", () -> deathCount[0]++);

    stats.setHealth(3);

    assertEquals(0, deathCount[0]);
  }
}
