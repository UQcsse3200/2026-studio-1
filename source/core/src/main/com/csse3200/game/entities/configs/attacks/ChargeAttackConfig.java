package com.csse3200.game.entities.configs.attacks;

/**
 * Configuration for a charge capability. Attached to any entity config that includes charging
 * (Minotaur, Centaur). Symmetric with {@link MeleeAttackConfig} and {@link RangedAttackConfig} -
 * pulls the previously-duplicated charge fields out of MinotaurConfig/CentaurConfig into a single
 * reusable config class, the same way melee/ranged settings were already factored out.
 *
 * <p>Deliberately does not include {@code aggroRadius} - that governs when an enemy starts
 * chasing at all (read by ChargeTask's priority check), not charge behaviour itself, so it stays
 * a top-level field on each enemy config rather than moving in here.
 */
public class ChargeAttackConfig {
  /* Movement speed multiplier applied while charging */
  public float speedMultiplier = 2.0f;

  /* Seconds a single charge lasts once started */
  public float duration = 4;

  /* Minimum seconds between the end of one charge and the start of the next */
  public float cooldown = 3;

  /* Damage multiplier applied to the attack that lands while charging */
  public float damageMultiplier = 1.0f;
}