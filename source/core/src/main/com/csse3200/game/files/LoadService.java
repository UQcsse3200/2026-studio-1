package com.csse3200.game.files;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

/** Applies saved game data to a newly created player. */
public class LoadService {

  /**
   * Loads saved player data and applies it to the given player.
   *
   * @param player player entity to restore
   */
  public static void load(Entity player) {
    if (player == null) {
      return;
    }

    GameSaveData data = SaveService.load();

    // No saved progress exists, so keep the player's default values.
    if (!hasSavedData(data)) {
      return;
    }

    loadHealth(player, data);
    loadInventory(player, data);
  }

  /**
   * Checks whether the loaded data represents an existing save.
   *
   * @param data loaded save data
   * @return true if saved progress exists
   */
  private static boolean hasSavedData(GameSaveData data) {
    return data.health > 0 || data.gold > 0 || !data.items.isEmpty();
  }

  private static void loadHealth(Entity player, GameSaveData data) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    if (stats != null && data.health > 0) {
      stats.setHealth(data.health);
    }
  }

  private static void loadInventory(Entity player, GameSaveData data) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return;
    }

    inventory.setGold(data.gold);

    for (SavedItem savedItem : data.items) {
      Item item = createItem(savedItem);

      if (item != null) {
        inventory.addItem(item);
      }
    }
  }

  private static Item createItem(SavedItem savedItem) {
    if (savedItem == null
        || savedItem.name == null
        || savedItem.itemType == null
        || savedItem.maxQuantity <= 0) {
      return null;
    }

    ItemType itemType;

    try {
      itemType = ItemType.valueOf(savedItem.itemType);
    } catch (IllegalArgumentException e) {
      return null;
    }

    if (itemType == ItemType.WEAPON && savedItem.weaponType != null && savedItem.damage != null) {
      try {
        WeaponType weaponType = WeaponType.valueOf(savedItem.weaponType);

        return new WeaponItem(
            savedItem.name,
            weaponType,
            savedItem.damage,
            savedItem.quantity,
            savedItem.maxQuantity);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    return new Item(savedItem.name, itemType, savedItem.quantity, savedItem.maxQuantity);
  }

  private LoadService() {
    throw new IllegalStateException("Utility class");
  }
}
