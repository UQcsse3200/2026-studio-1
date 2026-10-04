package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;

/** Shared base for flying enemies: adds how quickly stray velocity fades while hovering. */
public class FlyingEnemyConfig extends BaseEntityConfig {
  /* Linear damping applied to the physics body so knockback fades. Must not be negative. */
  public float flightDamping = 2.0f;
}
