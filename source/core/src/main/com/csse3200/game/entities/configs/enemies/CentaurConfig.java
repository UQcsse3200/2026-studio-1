package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.ChargeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

public class CentaurConfig extends BaseEntityConfig {
  public RangedAttackConfig ranged;
  public ChargeAttackConfig charge;
}
