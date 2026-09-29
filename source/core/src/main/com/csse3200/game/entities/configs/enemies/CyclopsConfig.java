package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

public class CyclopsConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public RangedAttackConfig ranged;
}
