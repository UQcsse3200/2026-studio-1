package com.csse3200.game.entities.configs.attacks;

/**
 * Configuration for a petrify capability, attached to any enemy config whose enemy can petrify the
 * player. The attack that applies petrify must have a cooldown of at least twice {@code duration}
 * (a team design rule: the player always gets a window to move and retaliate); the factory enforces
 * that, this class only holds the number.
 */
public class PetrifyConfig {
  /* Seconds the player stays petrified per application. */
  public float duration = 3.0f;
}
