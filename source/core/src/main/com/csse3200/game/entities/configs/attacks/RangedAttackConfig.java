package com.csse3200.game.entities.configs.attacks;

/**
 * Per-weider ranged attack settings, symmetric with MeleeAttackConfig.
 * Reused across RangedSkeletonConfig and CentaurConfig. Range/knockback stay here,
 * same reasoning as melee - not weapon properties.
 */

/**
 * Configuration for a ranged attack capability. Attached to any entity config that includes ranged
 * attacks.
 *
 * <p>Mechanically identical to {@link MeleeAttackConfig} - ranged and melee attacks share the exact
 * same underlying component logic (a distance + cooldown + knockback check), they just differ in
 * how far the attacker can be from its target when it lands a hit. The default {@code range} here
 * is deliberately much larger than {@link MeleeAttackConfig}'s.
 */
public class RangedAttackConfig {
  public float range = 6;
  public float cooldown = 2.5f;
  public float knockback = 0;

  private void validate() {
    if (range <= 0) {
      throw new IllegalArgumentException("Range must be positive.");
    }
    if (cooldown <= 0) {
      throw new IllegalArgumentException("Cooldown must be positive.");
    }
    if (knockback < 0) {
      throw new IllegalArgumentException("Knockback must not be negative.");
    }
  }
}
