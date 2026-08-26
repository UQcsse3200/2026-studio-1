package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WeaponItemTest {

  @Test
  void shouldCreateSword() {
    WeaponItem sword = new WeaponItem("Basic Sword", WeaponType.SWORD, 10, 1, 1);

    assertEquals("Basic Sword", sword.getName());
    assertEquals(ItemType.WEAPON, sword.getItemType());
    assertEquals(WeaponType.SWORD, sword.getWeaponType());
    assertEquals(10, sword.getDamage());
    assertEquals(1, sword.getQuantity());
    assertEquals(1, sword.getMaxQuantity());
  }

  @Test
  void shouldCreateBow() {
    WeaponItem bow = new WeaponItem("Basic Bow", WeaponType.BOW, 7, 1, 1);

    assertEquals("Basic Bow", bow.getName());
    assertEquals(ItemType.WEAPON, bow.getItemType());
    assertEquals(WeaponType.BOW, bow.getWeaponType());
    assertEquals(7, bow.getDamage());
  }

  @Test
  void shouldRejectNullWeaponType() {
    assertThrows(
        IllegalArgumentException.class, () -> new WeaponItem("Broken Weapon", null, 10, 1, 1));
  }

  @Test
  void shouldRejectNegativeDamage() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeaponItem("Broken Sword", WeaponType.SWORD, -1, 1, 1));
  }
}
