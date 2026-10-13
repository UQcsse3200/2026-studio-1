package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.enemies.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.enemies.attacks.PacingConfig;

/** Cerberus: stationary, natural melee with a short cooldown (three heads). */
public class CerberusConfig extends BaseEntityConfig {
  /** The settings of its melee attack. */
  public MeleeAttackConfig melee;
  public PacingConfig pacing;
}
