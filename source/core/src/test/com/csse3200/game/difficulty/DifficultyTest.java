package com.csse3200.game.difficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Each enum constant's four multipliers must match the tuned starting values exactly. */
class DifficultyTest {
  private static final float DELTA = 0.0001f;

  @Test
  void easyMultipliersMatchTheTunedStartingValues() {
    assertEquals(0.65f, Difficulty.EASY.getEnemyHealthMultiplier(), DELTA);
    assertEquals(0.6f, Difficulty.EASY.getEnemyDamageMultiplier(), DELTA);
    assertEquals(1.25f, Difficulty.EASY.getAttackCooldownMultiplier(), DELTA);
    assertEquals(1.0f, Difficulty.EASY.getGoldMultiplier(), DELTA);
  }

  @Test
  void normalMultipliersAreAllUnchanged() {
    assertEquals(1.0f, Difficulty.NORMAL.getEnemyHealthMultiplier(), DELTA);
    assertEquals(1.0f, Difficulty.NORMAL.getEnemyDamageMultiplier(), DELTA);
    assertEquals(1.0f, Difficulty.NORMAL.getAttackCooldownMultiplier(), DELTA);
    assertEquals(1.0f, Difficulty.NORMAL.getGoldMultiplier(), DELTA);
  }

  @Test
  void hardMultipliersMatchTheTunedStartingValues() {
    assertEquals(1.4f, Difficulty.HARD.getEnemyHealthMultiplier(), DELTA);
    assertEquals(1.5f, Difficulty.HARD.getEnemyDamageMultiplier(), DELTA);
    assertEquals(0.8f, Difficulty.HARD.getAttackCooldownMultiplier(), DELTA);
    assertEquals(1.5f, Difficulty.HARD.getGoldMultiplier(), DELTA);
  }
}
