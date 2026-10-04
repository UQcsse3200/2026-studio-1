package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

/** Cerberus: stationary, natural melee with a short cooldown (three heads). */
public class CerberusConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
}
