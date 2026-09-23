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

  // --- Added tests below (existing tests above are unchanged) ---

  // Generating a Dagger produces the correct name, type, and tier-1 damage.
  @Test
  void shouldGenerateDagger() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem weapon = generator.generateWeapon(WeaponType.DAGGER, 1);

    assertEquals("Basic Dagger", weapon.getName());
    assertEquals(WeaponType.DAGGER, weapon.getWeaponType());
    assertEquals(3, weapon.getDamage());
  }

  // Each weapon type is generated with its own fixed windup duration (Sword=3, Bow=2, Dagger=1).
  @Test
  void shouldSetWindupDurationPerWeaponType() {
    WeaponGenerator generator = new WeaponGenerator();

    assertEquals(2f, generator.generateWeapon(WeaponType.SWORD, 1).getWindupDuration());
    assertEquals(3f, generator.generateWeapon(WeaponType.BOW, 1).getWindupDuration());
    assertEquals(1f, generator.generateWeapon(WeaponType.DAGGER, 1).getWindupDuration());
  }

  // Damage scales linearly with tier for Bow and Dagger too, not just Sword.
  @Test
  void shouldIncreaseDamageForHigherTierAcrossWeaponTypes() {
    WeaponGenerator generator = new WeaponGenerator();

    assertEquals(7, generator.generateWeapon(WeaponType.BOW, 1).getDamage());
    assertEquals(14, generator.generateWeapon(WeaponType.BOW, 2).getDamage());
    assertEquals(3, generator.generateWeapon(WeaponType.DAGGER, 1).getDamage());
    assertEquals(6, generator.generateWeapon(WeaponType.DAGGER, 2).getDamage());
  }

  // Generated weapons always have a quantity of 1 and a maxQuantity of 1.
  @Test
  void shouldGenerateWeaponsWithSingleQuantity() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem weapon = generator.generateWeapon(WeaponType.SWORD, 1);

    assertEquals(1, weapon.getQuantity());
    assertEquals(10, weapon.getMaxQuantity());
  }

  @Test
  void shouldSetSwordSellPrice() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, 1);

    assertEquals(10, sword.getSellPrice());
  }

  @Test
  void shouldSetBowSellPrice() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem bow = generator.generateWeapon(WeaponType.BOW, 1);

    assertEquals(8, bow.getSellPrice());
  }

  @Test
  void shouldSetDaggerSellPrice() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem dagger = generator.generateWeapon(WeaponType.DAGGER, 1);

    assertEquals(6, dagger.getSellPrice());
  }

  @Test
  void shouldScaleWeaponSellPriceWithTier() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, 3);

    assertEquals(30, sword.getSellPrice());
  }
}
