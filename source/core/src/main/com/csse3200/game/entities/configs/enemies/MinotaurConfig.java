package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.ChargeAttackConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

/**
 * Minotaur: melee attacks up close and charges from a distance. Loaded from the NPC config file.
 */
public class MinotaurConfig extends BaseEntityConfig {
  /**
   * The settings of its melee attack.
   */
  public MeleeAttackConfig melee;

  /** The settings of its charge attack. */
  public ChargeAttackConfig charge;
}
