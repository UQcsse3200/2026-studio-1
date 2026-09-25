package com.csse3200.game.entities.spawn;

import com.csse3200.game.components.Component;

// Attached to an enemy entity alongside its normal death-handling component. Carries that
// spawn point's stable ID, and marks it as killed in the EnemyRegistry once the entity is
// disposed - using Component's dispose() lifecycle hook, with no changes needed to
// EnemyDeathComponent itself.
public class PersistentEnemyIdComponent extends Component {

  private final String enemyId;

  public PersistentEnemyIdComponent(String enemyId) {
    this.enemyId = enemyId;
  }

  @Override
  public void dispose() {
    EnemyRegistry.markKilled(enemyId);
  }
}
