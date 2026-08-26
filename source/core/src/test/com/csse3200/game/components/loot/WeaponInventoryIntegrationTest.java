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
}
