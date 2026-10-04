package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.attacks.PacingConfig;
import com.csse3200.game.entities.configs.attacks.PetrifyConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/** Medusa: short-range plain melee, long-range petrifying gaze, small bounded pacing. */
public class MedusaConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
  public PetrifyConfig petrify;
  public PacingConfig pacing;
}
