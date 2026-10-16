package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.enemies.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.enemies.attacks.PacingConfig;
import com.csse3200.game.entities.configs.enemies.attacks.PetrifyConfig;
import com.csse3200.game.entities.configs.enemies.attacks.RangedAttackConfig;

/** Medusa: short-range plain melee, long-range petrifying gaze, small bounded pacing. */
public class MedusaConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
  public PetrifyConfig petrify;
  public PacingConfig pacing;
}
