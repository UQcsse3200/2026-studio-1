package com.csse3200.game.difficulty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DifficultyScalerTest {
  private static final int STARTING_HEALTH = 100;
  private static final int STARTING_ATTACK = 10;
  private static final float STARTING_COOLDOWN = 2f;
  private static final int STARTING_GOLD = 50;

  @BeforeEach
  @AfterEach
  void resetToDefault() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  private Entity entityWithMeleeAttack() {
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 5, 1, 1, 0f);
    return new Entity()
        .addComponent(new CombatStatsComponent(STARTING_HEALTH, STARTING_ATTACK))
        .addComponent(new MeleeAttackComponent(1f, STARTING_COOLDOWN, 0f, sword))
        .addComponent(new InventoryComponent(STARTING_GOLD));
  }

  private Entity entityWithRangedAttack() {
    WeaponItem bow = new WeaponItem("Bow", WeaponType.BOW, 5, 1, 1, 0f);
    return new Entity()
        .addComponent(new CombatStatsComponent(STARTING_HEALTH, STARTING_ATTACK))
        .addComponent(new RangedAttackComponent(5f, STARTING_COOLDOWN, 0f, bow, 8f))
        .addComponent(new InventoryComponent(STARTING_GOLD));
  }

  private int expectedScaled(int value, float multiplier, int minimum) {
    return Math.max(minimum, Math.round(value * multiplier));
  }

  @Test
  void scalesEveryStatForEasy() {
    DifficultyService.setCurrent(Difficulty.EASY);
    Entity enemy = entityWithMeleeAttack();

    DifficultyScaler.apply(enemy);

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    MeleeAttackComponent attack = enemy.getComponent(MeleeAttackComponent.class);
    InventoryComponent inventory = enemy.getComponent(InventoryComponent.class);

    assertEquals(
        expectedScaled(STARTING_HEALTH, Difficulty.EASY.getEnemyHealthMultiplier(), 1),
        stats.getHealth());
    assertEquals(
        expectedScaled(STARTING_ATTACK, Difficulty.EASY.getEnemyDamageMultiplier(), 1),
        stats.getBaseAttack());
    assertEquals(
        STARTING_COOLDOWN * Difficulty.EASY.getAttackCooldownMultiplier(), attack.getCooldown());
    assertEquals(
        expectedScaled(STARTING_GOLD, Difficulty.EASY.getGoldMultiplier(), 0), inventory.getGold());
  }

  @Test
  void scalesEveryStatForNormal() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
    Entity enemy = entityWithMeleeAttack();

    DifficultyScaler.apply(enemy);

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    MeleeAttackComponent attack = enemy.getComponent(MeleeAttackComponent.class);
    InventoryComponent inventory = enemy.getComponent(InventoryComponent.class);

    assertEquals(STARTING_HEALTH, stats.getHealth());
    assertEquals(STARTING_ATTACK, stats.getBaseAttack());
    assertEquals(STARTING_COOLDOWN, attack.getCooldown());
    assertEquals(STARTING_GOLD, inventory.getGold());
  }

  @Test
  void scalesEveryStatForHardUsingRangedAttack() {
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity enemy = entityWithRangedAttack();

    DifficultyScaler.apply(enemy);

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    RangedAttackComponent attack = enemy.getComponent(RangedAttackComponent.class);
    InventoryComponent inventory = enemy.getComponent(InventoryComponent.class);

    assertEquals(
        expectedScaled(STARTING_HEALTH, Difficulty.HARD.getEnemyHealthMultiplier(), 1),
        stats.getHealth());
    assertEquals(
        expectedScaled(STARTING_ATTACK, Difficulty.HARD.getEnemyDamageMultiplier(), 1),
        stats.getBaseAttack());
    assertEquals(
        STARTING_COOLDOWN * Difficulty.HARD.getAttackCooldownMultiplier(), attack.getCooldown());
    assertEquals(
        expectedScaled(STARTING_GOLD, Difficulty.HARD.getGoldMultiplier(), 0), inventory.getGold());
  }

  @Test
  void doesNotThrowWhenSomeComponentsAreMissing() {
    DifficultyService.setCurrent(Difficulty.HARD);
    // Only a CombatStatsComponent - no attack component, no inventory, as a stationary enemy
    // might have.
    Entity enemy =
        new Entity().addComponent(new CombatStatsComponent(STARTING_HEALTH, STARTING_ATTACK));

    assertDoesNotThrow(() -> DifficultyScaler.apply(enemy));

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    assertEquals(
        expectedScaled(STARTING_HEALTH, Difficulty.HARD.getEnemyHealthMultiplier(), 1),
        stats.getHealth());
  }

  @Test
  void doesNotThrowWhenEnemyHasNoScalableComponentsAtAll() {
    Entity enemy = new Entity();

    assertDoesNotThrow(() -> DifficultyScaler.apply(enemy));
  }

  @Test
  void applyingToNullDoesNotThrow() {
    assertDoesNotThrow(() -> DifficultyScaler.apply(null));
  }
}
