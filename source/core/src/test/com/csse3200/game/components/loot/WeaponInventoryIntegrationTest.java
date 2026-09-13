package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.components.player.InventoryComponent;
import org.junit.jupiter.api.Test;

class WeaponInventoryIntegrationTest {

  @Test
  void shouldAddGeneratedWeaponToInventory() {
    WeaponGenerator generator = new WeaponGenerator();
    InventoryComponent inventory = new InventoryComponent(0);

    WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, 1);

    int remaining = inventory.addItem(sword);

    assertEquals(0, remaining);

    Item stored = inventory.getItem(1);
    assertInstanceOf(WeaponItem.class, stored);

    WeaponItem storedWeapon = (WeaponItem) stored;

    assertEquals("Basic Sword", storedWeapon.getName());
    assertEquals(WeaponType.SWORD, storedWeapon.getWeaponType());
    assertEquals(10, storedWeapon.getDamage());
  }

  @Test
  void shouldRemoveWeaponFromInventory() {
    WeaponGenerator generator = new WeaponGenerator();
    InventoryComponent inventory = new InventoryComponent(0);

    WeaponItem bow = generator.generateWeapon(WeaponType.BOW, 1);
    inventory.addItem(bow);

    Item removed = inventory.removeItem(1);

    assertInstanceOf(WeaponItem.class, removed);

    WeaponItem removedWeapon = (WeaponItem) removed;

    assertEquals(WeaponType.BOW, removedWeapon.getWeaponType());
    assertEquals(7, removedWeapon.getDamage());
    assertNull(inventory.getItem(1));
  }

  @Test
  void shouldStackDuplicateRoomWeaponsWhenAllInventorySlotsAreOccupied() {
    WeaponGenerator generator = new WeaponGenerator();
    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addItem(new Item("Health Potion", ItemType.CONSUMABLE, 1, 9));
    inventory.addItem(new Item("Damage Potion", ItemType.CONSUMABLE, 1, 9));
    inventory.addItem(new Item("Speed Potion", ItemType.CONSUMABLE, 1, 9));
    inventory.addItem(generator.generateWeapon(WeaponType.SWORD, 1));
    inventory.addItem(generator.generateWeapon(WeaponType.BOW, 1));

    int swordRemaining = inventory.addItem(generator.generateWeapon(WeaponType.SWORD, 1));
    int bowRemaining = inventory.addItem(generator.generateWeapon(WeaponType.BOW, 1));

    assertEquals(0, swordRemaining);
    assertEquals(0, bowRemaining);
    assertEquals(2, inventory.getTotalQuantity("Basic Sword", ItemType.WEAPON, 5));
    assertEquals(2, inventory.getTotalQuantity("Basic Bow", ItemType.WEAPON, 5));
  }

  @Test
  void shouldNotStackWeaponsWithDifferentDamageTiers() {
    WeaponGenerator generator = new WeaponGenerator();
    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(generator.generateWeapon(WeaponType.SWORD, 1));
    inventory.addItem(generator.generateWeapon(WeaponType.SWORD, 2));

    assertEquals(1, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getItem(2).getQuantity());
  }
}
