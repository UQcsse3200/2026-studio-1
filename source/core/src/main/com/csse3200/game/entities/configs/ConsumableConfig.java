package com.csse3200.game.entities.configs;

/**
 * Defines the properties stored in consumable config files to be loaded by the consumable
 * generator.
 *
 * <p>A single flat shape is used for every consumable so the JSON stays simple, following the same
 * public-field convention as {@link BaseEntityConfig}. Fields that do not apply to a given
 * consumable are simply left at their defaults: a health potion ignores {@code magnitude} and
 * {@code durationSeconds}, while a buff potion ignores {@code healAmount}.
 */
public class ConsumableConfig {
  /** Display name of the generated item. */
  public String name = "Consumable";

  /** Health restored by a health potion at tier 1. Unused by buff potions. */
  public int healAmount = 0;

  /** Stat multiplier applied by a buff potion at tier 1, where 1.0 is no change. */
  public float magnitude = 1f;

  /** How long a buff potion lasts, in seconds. Unused by health potions. */
  public float durationSeconds = 0f;

  /** Maximum stack size of the generated item. */
  public int maxQuantity = 9;
}
