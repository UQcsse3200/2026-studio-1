package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

<<<<<<< HEAD
/**
 * Configuration for Zeus: a mini-boss who fights with a carried sword up close and natural
 * lightning from range. Unlike Medusa/Cerberus/Cyclops, Zeus's melee attack uses a real {@link
 * com.csse3200.game.components.loot.WeaponItem} generated via {@link
 * com.csse3200.game.components.loot.WeaponGenerator} - only his lightning is a natural attack (no
 * conventional weapon represents a god hurling lightning from his own hands).
 *
 * <p>Satisfies the team's cross-cutting design rules: {@code health} is a boss-level value near
 * 100; {@code melee.range} is below {@code ranged.range}; and {@code ranged.cooldown} is at least
 * twice {@code freezeTicks} converted to seconds (120 ticks ≈ 2s at ~60 ticks/second, so a 6s
 * cooldown clears the 4s minimum with margin).
 */
public class ZeusConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;

  /**
   * Ticks the player is frozen (speed multiplier 0) for after being struck by Zeus's lightning.
   * Note: the bolt's own fall speed/height are currently fixed by {@link
   * com.csse3200.game.entities.factories.ArrowFactory#createLightning} rather than configurable
   * here - only the damage (sourced from this config's {@code baseAttack}, same as every natural
   * attack) and this freeze duration are wired through to Zeus specifically.
   */
  public int freezeTicks = 120;
=======
/** Zeus: sword melee (carried weapon) plus lightning (natural ranged weapon). */
public class ZeusConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
>>>>>>> origin/enemies
}
