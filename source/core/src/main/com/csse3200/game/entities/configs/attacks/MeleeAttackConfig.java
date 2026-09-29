package com.csse3200.game.entities.configs.attacks;

/**
 * Configuration for a melee attack capability. Attached to any entity config that includes melee
 * attacks.
 */
public class MeleeAttackConfig {
  /* Melee Reach - a property of the entity wielder, not the weapon */
  public float range = 2;
  /* Minimum time, in seconds, between attacks */
  public float cooldown = 4;
  /* Knockback magnitude onb a successful hit - property of the wilder not the weapon */
  public float knockback = 0;
}
