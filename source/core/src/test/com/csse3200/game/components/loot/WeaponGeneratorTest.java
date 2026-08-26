package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WeaponGeneratorTest {

  @Test
  void shouldGenerateSword() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem weapon = generator.generateWeapon(WeaponType.SWORD, 1);

    assertEquals("Basic Sword", weapon.getName());
    assertEquals(ItemType.WEAPON, weapon.getItemType());
    assertEquals(WeaponType.SWORD, weapon.getWeaponType());
    assertEquals(10, weapon.getDamage());
  }

  @Test
  void shouldGenerateBow() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem weapon = generator.generateWeapon(WeaponType.BOW, 1);

    assertEquals("Basic Bow", weapon.getName());
    assertEquals(WeaponType.BOW, weapon.getWeaponType());
    assertEquals(7, weapon.getDamage());
  }

  @Test
  void shouldIncreaseDamageForHigherTier() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem tierOne = generator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem tierTwo = generator.generateWeapon(WeaponType.SWORD, 2);

    assertEquals(10, tierOne.getDamage());
    assertEquals(20, tierTwo.getDamage());
  }

  @Test
  void shouldRejectInvalidTier() {
    WeaponGenerator generator = new WeaponGenerator();

    assertThrows(
        IllegalArgumentException.class, () -> generator.generateWeapon(WeaponType.SWORD, 0));
  }

  @Test
  void shouldRejectNullWeaponType() {
    WeaponGenerator generator = new WeaponGenerator();

    assertThrows(IllegalArgumentException.class, () -> generator.generateWeapon(null, 1));
  }
}
