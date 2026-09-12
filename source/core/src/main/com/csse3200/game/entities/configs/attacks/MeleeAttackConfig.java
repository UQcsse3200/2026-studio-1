package com.csse3200.game.entities.configs.attacks;

/**
 * Configuration for a melee attack capability. Attached to any entity config that includes melee
 * attacks.
 */
public class MeleeAttackConfig {
    /* Melee Reach - a property of the entity wielder, not the weapon */
    public float range = 2;
    /* Minimum time, in seconds, between attacks */
    public float cooldown = 3;
    /* Knockback magnitude onb a successful hit - property of the wilder not the weapon */
    public float knockback = 0;
    /* Delay, in seconds, between commit and resolve - matches the welder's swing animation.
     * Defaults to 0 if omitted. */
    public float windUpDuration;

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
      if (windUpDuration < 0) {
        throw new IllegalArgumentException("windUpDuration must not be negative.");
      }
      if (windUpDuration >= cooldown) {
        throw new IllegalArgumentException("windUpDuration must be less than cooldown");
      }
    }
}


