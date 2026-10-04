package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/** Zeus: sword melee (carried weapon) plus lightning (natural ranged weapon). */
public class ZeusConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
}
