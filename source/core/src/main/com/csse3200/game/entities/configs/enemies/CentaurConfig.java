package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

public class CentaurConfig extends BaseEntityConfig {
  public float aggroRadius;
  public float chargeSpeedMultiplier;
  public float chargeDuration;
  public float chargeCooldown;
  public float chargeDamageMultiplier;
  public RangedAttackConfig ranged;
}
