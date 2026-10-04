package com.csse3200.game.difficulty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
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
        .addComponent(new RangedAttackComponent(5f, STARTING_COOLDOWN, 0f, bow))
        .addComponent(new InventoryComponent(STARTING_GOLD));
  }

  /** A standalone MeleeAttackComponent, for tests that only need an entity to count as hostile. */
  private MeleeAttackComponent meleeAttack() {
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 5, 1, 1, 0f);
    return new MeleeAttackComponent(1f, STARTING_COOLDOWN, 0f, sword);
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

  /**
   * Regression test for the bug this fixes: at the old gold multipliers (EASY 1.25, HARD 1.15), a
   * typical 3-gold enemy rounded to the exact same 3 gold on every difficulty - round(3 * 1.15) ==
   * round(3 * 1.0) == 3 - making the difference invisible. Direct proof it's visible now.
   */
  @Test
  void aThreeGoldEnemyEndsWithFiveOnHardAndThreeOnNormalAndEasy() {
    // Needs an attack component - a 3-gold entity with no way to attack is not hostile, and
    // apply() now leaves non-hostile entities (and their gold) untouched entirely.
    Entity easyEnemy =
        new Entity().addComponent(new InventoryComponent(3)).addComponent(meleeAttack());
    Entity normalEnemy =
        new Entity().addComponent(new InventoryComponent(3)).addComponent(meleeAttack());
    Entity hardEnemy =
        new Entity().addComponent(new InventoryComponent(3)).addComponent(meleeAttack());

    DifficultyService.setCurrent(Difficulty.EASY);
    DifficultyScaler.apply(easyEnemy);
    DifficultyService.setCurrent(Difficulty.NORMAL);
    DifficultyScaler.apply(normalEnemy);
    DifficultyService.setCurrent(Difficulty.HARD);
    DifficultyScaler.apply(hardEnemy);

    assertEquals(3, easyEnemy.getComponent(InventoryComponent.class).getGold());
    assertEquals(3, normalEnemy.getComponent(InventoryComponent.class).getGold());
    assertEquals(5, hardEnemy.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void meleeAttackDamageMultiplierMatchesTheCurrentDifficultyForEasyNormalAndHard() {
    for (Difficulty difficulty : Difficulty.values()) {
      DifficultyService.setCurrent(difficulty);
      Entity enemy = entityWithMeleeAttack();

      DifficultyScaler.apply(enemy);

      assertEquals(
          difficulty.getEnemyDamageMultiplier(),
          enemy.getComponent(MeleeAttackComponent.class).getDamageMultiplier());
    }
  }

  @Test
  void meleeWindupMultiplierMatchesTheCurrentDifficultyForEasyNormalAndHard() {
    Difficulty[] modes = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
    float[] expected = {1.0f, 1.0f, 0.7f};
    for (int i = 0; i < modes.length; i++) {
      DifficultyService.setCurrent(modes[i]);
      Entity enemy = entityWithMeleeAttack();

      DifficultyScaler.apply(enemy);

      assertEquals(
          expected[i],
          enemy.getComponent(MeleeAttackComponent.class).getWindupMultiplier(),
          0.0001f);
    }
  }

  @Test
  void rangedOnlyEnemyIsStillScaledAsBeforeWithoutErrors() {
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity enemy = entityWithRangedAttack();

    assertDoesNotThrow(() -> DifficultyScaler.apply(enemy));

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    RangedAttackComponent attack = enemy.getComponent(RangedAttackComponent.class);
    assertEquals(
        expectedScaled(STARTING_HEALTH, Difficulty.HARD.getEnemyHealthMultiplier(), 1),
        stats.getHealth());
    assertEquals(
        STARTING_COOLDOWN * Difficulty.HARD.getAttackCooldownMultiplier(), attack.getCooldown());
  }

  /**
   * isHostile accepts a TouchAttackComponent as well as melee/ranged, but no other test exercises
   * that branch - removing it would go unnoticed otherwise. A ghost (CombatStatsComponent +
   * TouchAttackComponent, no melee, ranged, or inventory) is a real example of this shape.
   */
  @Test
  void aGhostWithOnlyTouchAttackIsScaledOnHard() {
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity ghost =
        new Entity()
            .addComponent(new CombatStatsComponent(40, 10))
            .addComponent(new TouchAttackComponent((short) 0));

    assertDoesNotThrow(() -> DifficultyScaler.apply(ghost));

    CombatStatsComponent stats = ghost.getComponent(CombatStatsComponent.class);
    assertEquals(56, stats.getHealth());
    assertEquals(15, stats.getBaseAttack());
  }

  @Test
  void doesNotThrowAndLeavesStatsUnscaledWhenNoAttackComponentIsPresent() {
    DifficultyService.setCurrent(Difficulty.HARD);
    // Only a CombatStatsComponent - no attack component, no inventory. Without a way to attack,
    // this entity isn't hostile, so apply() must leave it alone entirely rather than scaling it.
    Entity enemy =
        new Entity().addComponent(new CombatStatsComponent(STARTING_HEALTH, STARTING_ATTACK));

    assertDoesNotThrow(() -> DifficultyScaler.apply(enemy));

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    assertEquals(STARTING_HEALTH, stats.getHealth());
    assertEquals(STARTING_ATTACK, stats.getBaseAttack());
  }

  @Test
  void aNonHostileNpcWithZeroBaseAttackIsLeftCompletelyUntouchedOnHard() {
    // e.g. npc:traveler - CombatStatsComponent(50, 0) and no attack component. Without the
    // hostility gate, scale()'s floor-at-1 would also push the 0 base attack up to 1 even on
    // Normal; the gate must prevent apply() from touching this entity at all.
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity npc = new Entity().addComponent(new CombatStatsComponent(50, 0));

    DifficultyScaler.apply(npc);

    CombatStatsComponent stats = npc.getComponent(CombatStatsComponent.class);
    assertEquals(50, stats.getHealth());
    assertEquals(0, stats.getBaseAttack());
  }

  @Test
  void aHostileEnemyWithZeroGoldStaysAtZero() {
    DifficultyService.setCurrent(Difficulty.HARD);
    Entity enemy = new Entity().addComponent(meleeAttack()).addComponent(new InventoryComponent(0));

    DifficultyScaler.apply(enemy);

    assertEquals(0, enemy.getComponent(InventoryComponent.class).getGold());
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
