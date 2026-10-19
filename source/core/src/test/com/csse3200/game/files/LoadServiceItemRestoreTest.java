package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests how {@link LoadService} rebuilds saved inventory items, including potion tiers, weapons
 * saved without full details, and damaged or incomplete entries in the save file.
 */
@ExtendWith(GameExtension.class)
class LoadServiceItemRestoreTest {
  private static final List<UpgradeNode> NO_UPGRADES = List.of();

  private Entity player;
  private InventoryComponent inventory;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    inventory = new InventoryComponent(0);
    player = new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(inventory);
    player.create();
  }

  private void loadItems(SavedItem... items) {
    GameSaveData data = new GameSaveData();
    data.health = 100;
    data.items.addAll(Arrays.asList(items));
    LoadService.apply(player, data, 50f, 30f, NO_UPGRADES);
  }

  private static SavedItem plainItem(int slot, String name) {
    SavedItem saved = new SavedItem();
    saved.slot = slot;
    saved.name = name;
    saved.itemType = ItemType.CURRENCY.name();
    saved.quantity = 1;
    saved.maxQuantity = 10;
    return saved;
  }

  private static SavedItem potion(int slot, String name, String consumableType) {
    SavedItem saved = plainItem(slot, name);
    saved.itemType = ItemType.CONSUMABLE.name();
    saved.maxQuantity = 9;
    saved.consumableType = consumableType;
    return saved;
  }

  private static SavedItem sword(int slot, Integer damage, Integer tier) {
    SavedItem saved = plainItem(slot, "Sword");
    saved.itemType = ItemType.WEAPON.name();
    saved.maxQuantity = 1;
    saved.weaponType = WeaponType.SWORD.name();
    saved.damage = damage;
    saved.weaponTier = tier;
    return saved;
  }

  private static String potionName(int tier) {
    return new ConsumableGenerator()
        .generateConsumable(ConsumableType.HEALTH_POTION, tier)
        .getName();
  }

  // Potions

  @Test
  void tierTwoPotionKeepsItsTier() {
    String tierTwo = potionName(2);

    loadItems(potion(1, tierTwo, ConsumableType.HEALTH_POTION.name()));

    ConsumableItem restored = assertInstanceOf(ConsumableItem.class, inventory.getItem(1));
    assertEquals(tierTwo, restored.getName());
    assertNotEquals(potionName(1), restored.getName());
  }

  @Test
  void potionWithoutATierInItsNameIsTierOne() {
    loadItems(potion(1, potionName(1), ConsumableType.HEALTH_POTION.name()));

    assertEquals(potionName(1), inventory.getItem(1).getName());
  }

  @Test
  void potionWithAnUnknownTypeIsSkipped() {
    loadItems(potion(1, "Odd Potion", "NOT_A_POTION"), plainItem(2, "Gem"));

    assertNull(inventory.getItem(1));
    assertEquals("Gem", inventory.getItem(2).getName());
  }

  @Test
  void consumableWithoutAPotionTypeComesBackAsAPlainItem() {
    loadItems(potion(1, "Old Potion", null));

    Item restored = inventory.getItem(1);
    assertNotNull(restored);
    assertFalse(restored instanceof ConsumableItem);
    assertEquals("Old Potion", restored.getName());
  }

  // Weapons

  @Test
  void weaponWithAnUnknownTypeIsSkipped() {
    SavedItem broken = sword(1, 10, 1);
    broken.weaponType = "NOT_A_WEAPON";

    loadItems(broken);

    assertNull(inventory.getItem(1));
  }

  @Test
  void weaponWithoutATierUsesItsSavedDamage() {
    loadItems(sword(1, 17, null));

    WeaponItem restored = assertInstanceOf(WeaponItem.class, inventory.getItem(1));
    assertEquals(WeaponType.SWORD, restored.getWeaponType());
    assertEquals(17, restored.getDamage());
  }

  @Test
  void weaponWithoutDamageComesBackAsAPlainItem() {
    loadItems(sword(1, null, null));

    Item restored = inventory.getItem(1);
    assertNotNull(restored);
    assertFalse(restored instanceof WeaponItem);
    assertEquals("Sword", restored.getName());
  }

  // Damaged entries

  @Test
  void nullAndIncompleteEntriesAreSkipped() {
    SavedItem noName = plainItem(1, null);
    SavedItem noType = plainItem(2, "Typeless");
    noType.itemType = null;
    SavedItem noCapacity = plainItem(3, "Empty");
    noCapacity.maxQuantity = 0;

    assertDoesNotThrow(() -> loadItems(null, noName, noType, noCapacity, plainItem(4, "Gem")));

    assertNull(inventory.getItem(1));
    assertNull(inventory.getItem(2));
    assertNull(inventory.getItem(3));
    assertEquals("Gem", inventory.getItem(4).getName());
  }

  @Test
  void itemSavedInAnInvalidSlotIsAddedToAFreeSlot() {
    loadItems(plainItem(99, "Gem"));

    assertEquals("Gem", inventory.getItem(1).getName());
  }

  @Test
  void playerWithoutAnInventoryDoesNotCrash() {
    Entity noInventory = new Entity().addComponent(new CombatStatsComponent(100, 10));
    noInventory.create();
    GameSaveData data = new GameSaveData();
    data.items.add(plainItem(1, "Gem"));

    assertDoesNotThrow(() -> LoadService.apply(noInventory, data, 50f, 30f, NO_UPGRADES));
    assertTrue(noInventory.getComponent(CombatStatsComponent.class).getHealth() > 0);
  }
}
