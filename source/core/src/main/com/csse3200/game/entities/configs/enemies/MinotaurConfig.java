package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

public class MinotaurConfig extends BaseEntityConfig {
  public float aggroRadius;
  public float chargeSpeedMultiplier;
  public float chargeDuration;
  public float chargeCooldown;
  public float chargeDamageMultiplier;
  public MeleeAttackConfig melee;
}
