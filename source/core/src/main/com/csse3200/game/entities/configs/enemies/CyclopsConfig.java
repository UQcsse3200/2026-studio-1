package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.enemies.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.enemies.attacks.RangedAttackConfig;

public class CyclopsConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee; // the stomp
  public RangedAttackConfig rock; // lobbed rock, long range
  public RangedAttackConfig laser; // eye laser, mid range, fast
}
