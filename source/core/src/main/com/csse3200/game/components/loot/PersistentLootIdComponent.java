package com.csse3200.game.components.loot;

import com.csse3200.game.components.Component;

/**
 * Attached to a loot entity alongside {@link LootPickupComponent}. Carries that spawn point's
 * stable ID, and marks it as collected in the {@link LootRegistry} when the entity is disposed
 * after being picked up.
 *
 * <p>Loot is also disposed when its level is unloaded (changing room, or dying and respawning).
 * That loot was never picked up, so it is not recorded and appears again when the level is rebuilt.
 *
 * <p>This component does not modify how loot is picked up. It only reads {@link
 * LootPickupComponent#isCollected()}, which is set before the picked-up entity is disposed.
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

  /** Returns the stable ID of the loot spot this entity represents. */
  public String getLootId() {
    return lootId;
  }

  @Override
  public void dispose() {
    LootPickupComponent pickup =
        entity == null ? null : entity.getComponent(LootPickupComponent.class);
    if (pickup != null && pickup.isCollected()) {
      LootRegistry.markCollected(lootId);
    }
  }
}
