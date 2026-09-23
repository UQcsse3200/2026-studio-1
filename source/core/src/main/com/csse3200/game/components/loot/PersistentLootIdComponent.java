package com.csse3200.game.components.loot;

import com.csse3200.game.components.Component;

/**
 * Attached to a loot entity alongside {@link LootPickupComponent}. Carries that spawn point's
 * stable ID, and marks it as collected in the {@link LootRegistry} once the entity is disposed
 * (i.e. picked up).
 *
 * <p>This component does not modify how loot is picked up - it simply rides along on the same
 * entity, using Component's existing dispose() lifecycle hook to record collection with no changes
 * to LootPickupComponent itself.
 */
public class PersistentLootIdComponent extends Component {

  private final String lootId;

  /**
   * Creates a component carrying the given loot spot's stable ID.
   *
   * @param lootId the stable ID of the loot spot this entity represents
   */
  public PersistentLootIdComponent(String lootId) {
    this.lootId = lootId;
  }

  @Override
  public void dispose() {
    LootRegistry.markCollected(lootId);
  }
}
