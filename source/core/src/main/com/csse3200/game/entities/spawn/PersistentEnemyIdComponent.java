package com.csse3200.game.entities.spawn;

import com.csse3200.game.components.Component;

/** Records an enemy spawn as killed only when that enemy's death event occurs. */
public class PersistentEnemyIdComponent extends Component {

  private final String enemyId;

  public PersistentEnemyIdComponent(String enemyId) {
    this.enemyId = enemyId;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("death", () -> EnemyRegistry.markKilled(enemyId));
  }
}
