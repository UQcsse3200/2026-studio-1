package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;

/**
 * Drops the player's inventory and gold when the player dies.
 *
 * <p>The actual loot entities are created after the current game/physics update has completed. This
 * is important because death can be triggered from a Box2D collision callback, during which the
 * physics world is locked and new physics bodies cannot safely be created.
 */
public class DeathLootDropComponent extends Component {

  private boolean lootDropScheduled = false;
  private boolean lootDropped = false;

  /** Registers the death event listener. */
  @Override
  public void create() {
    entity.getEvents().addListener("death", this::onDeath);
  }

  /**
   * Schedules the loot drop for after the current physics update.
   *
   * <p>The death event can be triggered while Box2D is processing a collision. Creating loot
   * entities immediately would attempt to create new Box2D bodies while the world is locked.
   */
  private void onDeath() {
    if (lootDropScheduled || lootDropped) {
      return;
    }

    lootDropScheduled = true;

    Gdx.app.postRunnable(this::dropLoot);
  }

  /**
   * Drops all inventory items and gold after the physics world has finished processing the
   * collision that caused the player's death.
   */
  private void dropLoot() {
    if (lootDropped) {
      return;
    }

    lootDropped = true;

    ItemDropComponent itemDrop = entity.getComponent(ItemDropComponent.class);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (itemDrop == null || inventory == null) {
      return;
    }

    // Drop every occupied inventory slot.
    while (inventory.getOccupiedSlots() > 0) {
      if (!itemDrop.dropFirstStack()) {
        break;
      }
    }

    // Drop all remaining gold.
    itemDrop.dropGold();

    // Drop a held, unactivated shield. It bypasses InventoryComponent entirely (see
    // ShieldComponent), so it is not covered by the inventory-slot loop above.
    itemDrop.dropShield();
  }
}
