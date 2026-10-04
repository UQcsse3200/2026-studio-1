package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
<<<<<<< HEAD
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/**
 * Configuration for Medusa: a mini-boss who paces near her spawn, melees up close with natural
 * claws, and petrifies the player from range with her gaze. Like Cyclops, she carries no weapons of
 * her own - both attacks flow through {@link com.csse3200.game.components.loot.WeaponItem#natural}
 * - so {@code melee} and {@code ranged} here configure the wielder-side reach/cooldown/knockback
 * only, same as every other enemy config.
 *
 * <p>Satisfies the team's cross-cutting design rules: {@code health} is a boss-level value near
 * 100; {@code melee.range} is below {@code ranged.range}; and {@code ranged.cooldown} is at least
 * twice {@code petrifyTicks} converted to seconds (90 ticks ≈ 1.5s at ~60 ticks/second, so a 6s
 * cooldown clears the 3s minimum with margin).
 */
public class MedusaConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;

  /** Ticks the player is petrified (speed multiplier 0) for after being hit by Medusa's gaze. */
  public int petrifyTicks = 90;
=======
import com.csse3200.game.entities.configs.attacks.PacingConfig;
import com.csse3200.game.entities.configs.attacks.PetrifyConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/** Medusa: short-range plain melee, long-range petrifying gaze, small bounded pacing. */
public class MedusaConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
  public PetrifyConfig petrify;
  public PacingConfig pacing;
>>>>>>> origin/enemies
}
