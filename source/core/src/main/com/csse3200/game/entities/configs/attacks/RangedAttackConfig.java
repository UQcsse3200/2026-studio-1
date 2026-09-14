package com.csse3200.game.entities.configs.attacks;

/**
 * Per-weider ranged attack settings, symmetric with MeleeAttackConfig. Reused across
 * RangedSkeletonConfig and CentaurConfig. Range/knockback stay here, same reasoning as melee - not
 * weapon properties.
 */

/**
 * Configuration for a ranged attack capability. Attached to any entity config that includes ranged
 * attacks.
 *
 * <p>Unlike {@link MeleeAttackConfig}, a ranged attack fires a real arrow projectile (see {@link
 * com.csse3200.game.components.attacks.RangedAttackComponent}) rather than resolving damage
 * instantly - {@code range} and {@code cooldown} still gate when a shot is <i>fired</i>, the same
 * as melee, but whether it actually lands is resolved later by the arrow itself. The default {@code
 * range} here is deliberately much larger than {@link MeleeAttackConfig}'s.
 */
public class RangedAttackConfig {
  public float range = 6;
  public float cooldown = 4;
  public float knockback = 0;
  public float projectileSpeed = 8f;
}
