package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;

/**
 * Disposes an enemy entity when it dies. Lisens for enemyDeath event which is fired when the player
 * applies killing blow. Can be applied to any enemy type which can be killed by any player attack
 * type.
 */
public class EnemyDisposalComponent extends Component {
  /** Registers a listener for "enemyDeath" on this entity. */
  @Override
  public void create() {
    entity.getEvents().addListener("enemyDeath", this::onEnemyDeath);
  }

  /** Disposes this entity. */
  public void onEnemyDeath() {
    entity.dispose();
  }
}
