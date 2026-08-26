package com.csse3200.game.components.player;

/** The player stats that a temporary buff can modify. */
public enum BuffStat {
  /** Scales the entity's base attack damage via {@code CombatStatsComponent}. */
  DAMAGE,
  /** Scales the player's movement speed, read back via {@code getSpeedMultiplier()}. */
  SPEED
}
