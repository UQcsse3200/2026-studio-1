package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.ChargeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/** Centaur: shoots from range and charges the player down. Loaded from the NPC config file. */
public class CentaurConfig extends BaseEntityConfig {
  /** The settings of its ranged attack. */
  public RangedAttackConfig ranged;

  /** The settings of its charge attack. */
  public ChargeAttackConfig charge;
}
