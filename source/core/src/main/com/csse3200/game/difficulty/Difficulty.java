package com.csse3200.game.difficulty;

/**
 * The player's selected difficulty mode, and the gameplay multipliers each mode applies.
 *
 * <p>Immutable per-mode data - each constant's four multipliers are fixed at construction and
 * exposed only via getters, so nothing downstream can accidentally mutate NORMAL's values while
 * tuning EASY's, for example.
 */
public enum Difficulty {
  EASY(0.65f, 0.6f, 1.25f, 1.0f, 1.0f),
  NORMAL(1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
  HARD(1.4f, 1.5f, 0.8f, 1.5f, 1.5f);

  private final float enemyHealthMultiplier;
  private final float enemyDamageMultiplier;
  private final float attackCooldownMultiplier;
  private final float goldMultiplier;
  private final float regenHealMultiplier;

  Difficulty(
      float enemyHealthMultiplier,
      float enemyDamageMultiplier,
      float attackCooldownMultiplier,
      float goldMultiplier,
      float regenHealMultiplier) {
    this.enemyHealthMultiplier = enemyHealthMultiplier;
    this.enemyDamageMultiplier = enemyDamageMultiplier;
    this.attackCooldownMultiplier = attackCooldownMultiplier;
    this.goldMultiplier = goldMultiplier;
    this.regenHealMultiplier = regenHealMultiplier;
  }

  /**
   * @return multiplier applied to enemy max health.
   */
  public float getEnemyHealthMultiplier() {
    return enemyHealthMultiplier;
  }

  /**
   * @return multiplier applied to enemy attack damage.
   */
  public float getEnemyDamageMultiplier() {
    return enemyDamageMultiplier;
  }

  /**
   * @return multiplier applied to enemy attack cooldown - lower means faster enemy attacks.
   */
  public float getAttackCooldownMultiplier() {
    return attackCooldownMultiplier;
  }

  /**
   * @return multiplier applied to gold gained.
   */
  public float getGoldMultiplier() {
    return goldMultiplier;
  }

  /**
   * @return multiplier applied to Regen on Kill's heal-per-kill amount.
   */
  public float getRegenHealMultiplier() {
    return regenHealMultiplier;
  }
}
