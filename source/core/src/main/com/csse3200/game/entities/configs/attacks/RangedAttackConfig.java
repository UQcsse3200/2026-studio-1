package com.csse3200.game.entities.configs.attacks;

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
}
