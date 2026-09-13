package com.csse3200.game.components.player;

/**
 * The player stats that a temporary buff can modify.
 *
 * <p>Most buffs are stronger the higher their multiplier, but a reduction such as resistance is
 * stronger the lower it goes. {@link #isStrongerWhenLower()} lets {@link PlayerBuffComponent} apply
 * one "strongest buff wins" rule to both kinds.
 */
public enum BuffStat {
  /** Scales the entity's base attack damage via {@code CombatStatsComponent}. */
  DAMAGE(false),
  /** Scales the player's movement speed, read back via {@code getSpeedMultiplier()}. */
  SPEED(false),
  /** Multiplies incoming damage, so a value below 1.0 reduces the damage taken. */
  RESISTANCE(true);

  private final boolean strongerWhenLower;

  BuffStat(boolean strongerWhenLower) {
    this.strongerWhenLower = strongerWhenLower;
  }

  /**
   * Returns whether a smaller multiplier is the better buff for this stat.
   *
   * @return {@code true} for reductions such as resistance, {@code false} for increases
   */
  public boolean isStrongerWhenLower() {
    return strongerWhenLower;
  }
}
