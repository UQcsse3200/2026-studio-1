package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;

/** Defines the properties of the Zeus boss; {@code baseAttack} is his melee swing. */
public class ZeusConfig extends BaseEntityConfig {
  public int boltDamage = 12;
  public int strikeDamage = 18;
  public int shockwaveDamage = 12;
  public float moveSpeed = 2.2f;
  public float meleeRange = 1.6f;
  public float meleeKnockback = 4f;

  /** Fraction of full health at which Zeus leaves the floor for the throne balcony. */
  public float throneThreshold = 0.7f;

  /** Fraction of full health at which Zeus slams back to the floor, enraged. */
  public float enragedThreshold = 0.35f;
}
