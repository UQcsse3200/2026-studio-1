package com.csse3200.game.entities.configs.attacks;

/**
 * Configuration for small back-and-forth pacing around a spawn point, for enemies that do not chase
 * the player but should not stand perfectly still.
 */
public class PacingConfig {
  /** How far, in world units, the enemy may stray either side of where it spawned. */
  public float radius = 2.0f;
}
