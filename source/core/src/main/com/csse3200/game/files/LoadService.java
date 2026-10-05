package com.csse3200.game.files;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.upgrades.UpgradeNode;
import java.util.List;

/** Applies saved game data to a newly created player. */
public class LoadService {

  /**
   * Loads saved player data and applies it to the given player.
   *
   * @param player player entity to restore
   */
  public static void load(
      Entity player, float mapWidth, float mapHeight, List<UpgradeNode> upgrades) {
    if (player == null) {
      return;
    }

    // No save file exists, so keep the player's default values.
    if (!SaveService.hasSave()) {
      return;
    }

    GameSaveData data = SaveService.load();

    loadHealth(player, data);
    loadStamina(player, data);
    loadInventory(player, data);
    loadPosition(player, data, mapWidth, mapHeight);
    loadPets(player, data);
    loadUpgrades(player, data, upgrades);
    loadBuffs(player, data);
  }

  private static void loadHealth(Entity player, GameSaveData data) {
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    if (stats != null && data.health > 0) {
      stats.setHealth(data.health);
    }
  }

  private static void loadStamina(Entity player, GameSaveData data) {
    StaminaComponent stamina = player.getComponent(StaminaComponent.class);

    if (stamina != null) {
      stamina.setStamina(data.stamina);
    }
  }

  private static void loadBuffs(Entity player, GameSaveData data) {
    PlayerBuffComponent buffs = player.getComponent(PlayerBuffComponent.class);
    if (buffs != null && data.buffs != null) {
      for (SavedBuff saved : data.buffs) {
        if (saved == null || saved.stat == null) {
          continue;
        }

        try {
          buffs.applyBuff(BuffStat.valueOf(saved.stat), saved.magnitude, saved.remainingSeconds);
        } catch (IllegalArgumentException e) {
          // Unknown stat name in the save file - skip this buff.
        }
      }
    }

    PlayerRegenComponent regen = player.getComponent(PlayerRegenComponent.class);
    if (regen != null) {
      regen.startRegen(data.regenHealPerTick, data.regenRemainingSeconds);
    }
  }

  private static void loadPosition(
      Entity player, GameSaveData data, float mapWidth, float mapHeight) {
    boolean validX = data.posX >= 0 && data.posX <= mapWidth;
    boolean validY = data.posY >= 0 && data.posY <= mapHeight;

    if (validX && validY) {
      player.setPosition(data.posX, data.posY);
    }
    // Otherwise, leave the player at its default spawn position.
  }

  private static void loadInventory(Entity player, GameSaveData data) {
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return;
    }

    // Clear out any default starting items (e.g. the starting weapon/bow/dagger
    // PlayerFactory gives every new player) so loaded items replace them,
    // rather than competing with them for the same limited slots.
    for (Integer slot : new java.util.ArrayList<>(inventory.getInventorySlots().keySet())) {
      inventory.removeItem(slot);
    }

    inventory.setGold(data.gold);

    for (SavedItem savedItem : data.items) {
      Item item = createItem(savedItem);

      if (item == null) {
        continue;
      }

      // Rebuilding an item resets these, so put the saved values back.
      item.setQuantity(savedItem.quantity);
      if (savedItem.sellPrice != null && savedItem.sellPrice >= 0) {
        item.setSellPrice(savedItem.sellPrice);
      }

      // Put the item back in the exact slot it was saved from. If that slot is
      // invalid, fall back to the normal add so the item isn't lost.
      if (inventory.setItem(savedItem.slot, item)) {
        continue;
      }

      int notAdded = inventory.addItem(item);
      if (notAdded > 0) {
        org.slf4j.LoggerFactory.getLogger(LoadService.class)
            .warn(
                "Inventory full - could not fully restore saved item: {} ({} not added)",
                savedItem.name,
                notAdded);
      }
    }

    inventory.setActiveSlot(data.activeSlot);
  }

  private static void loadPets(Entity player, GameSaveData data) {
    ShopComponent shop = player.getComponent(ShopComponent.class);
    if (shop != null) {
      shop.restorePurchasedPets(data.ownedPetNames);
    }

    if (data.activePetName != null && !data.activePetName.isBlank()) {
      PetManagerComponent petManager = player.getComponent(PetManagerComponent.class);
      if (petManager != null) {
        petManager.activatePet(new ShopComponent.Pet(data.activePetName));
      }
    }
  }

  private static void loadUpgrades(Entity player, GameSaveData data, List<UpgradeNode> upgrades) {
    if (upgrades == null || data.upgrades == null) {
      return;
    }

    for (SavedUpgrade saved : data.upgrades) {
      if (saved == null || saved.id == null) {
        continue;
      }

      for (UpgradeNode node : upgrades) {
        if (saved.id.equals(node.getId())) {
          node.restore(saved.tier, saved.remainingSeconds, saved.remainingKills);
          break;
        }
      }
    }

    // Restoring the shield upgrade refills its hits, so put back the saved remaining count.
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats != null && data.shieldHits != null) {
      stats.setShieldHits(data.shieldHits);
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

        if (savedItem.weaponTier != null) {
          try {
            WeaponTier weaponTier = WeaponTier.fromTierNumber(savedItem.weaponTier);
            return new WeaponItem(
                savedItem.name,
                weaponType,
                weaponTier,
                savedItem.quantity,
                savedItem.maxQuantity,
                0f);
          } catch (IllegalArgumentException e) {
            // Unrecognised tier number - fall back to the flat-damage constructor below.
          }
        }

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

    if (itemType == ItemType.CONSUMABLE && savedItem.consumableType != null) {
      try {
        ConsumableType consumableType = ConsumableType.valueOf(savedItem.consumableType);
        int tier = parseConsumableTier(savedItem.name);
        return new ConsumableGenerator().generateConsumable(consumableType, tier);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    return new Item(savedItem.name, itemType, savedItem.quantity, savedItem.maxQuantity);
  }

  /**
   * Recovers a consumable's loot tier from its saved name.
   *
   * <p>ConsumableItem doesn't store tier directly, but ConsumableGenerator names tier 2+ items as
   * "<name> (Tier N)". Tier 1 has no suffix, so a missing or unparsable match defaults to 1.
   *
   * @param name the item's saved display name
   * @return the parsed tier, or 1 if none is found
   */
  private static int parseConsumableTier(String name) {
    if (name == null) {
      return 1;
    }

    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("\\(Tier (\\d+)\\)$").matcher(name.trim());

    if (matcher.find()) {
      try {
        return Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException e) {
        return 1;
      }
    }

    return 1;
  }

  private LoadService() {
    throw new IllegalStateException("Utility class");
  }
}
