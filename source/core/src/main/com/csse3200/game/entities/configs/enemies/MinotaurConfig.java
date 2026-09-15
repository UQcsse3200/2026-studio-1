package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.ChargeAttackConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

public class MinotaurConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
  public ChargeAttackConfig charge;
}
