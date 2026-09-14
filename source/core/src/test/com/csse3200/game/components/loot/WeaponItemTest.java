package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WeaponItemTest {

  @Test
  void shouldCreateSword() {
    WeaponItem sword = new WeaponItem("Basic Sword", WeaponType.SWORD, WeaponTier.TIER_2, 1, 1, 2);

    assertEquals("Basic Sword", sword.getName());
    assertEquals(ItemType.WEAPON, sword.getItemType());
    assertEquals(WeaponType.SWORD, sword.getWeaponType());
    assertEquals(2, sword.getWindupDuration());
    assertEquals(20, sword.getDamage());
    assertEquals(1, sword.getQuantity());
    assertEquals(1, sword.getMaxQuantity());
  }

  @Test
  void shouldCreateBow() {
    WeaponItem bow = new WeaponItem("Basic Bow", WeaponType.BOW, WeaponTier.TIER_3, 1, 1, 3);
    assertEquals("Basic Bow", bow.getName());
    assertEquals(ItemType.WEAPON, bow.getItemType());
    assertEquals(WeaponType.BOW, bow.getWeaponType());
    assertEquals(3, bow.getWindupDuration());
    assertEquals(21, bow.getDamage());
  }

  @Test
  void shouldRejectNullWeaponType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeaponItem("Broken Weapon", null, WeaponTier.TIER_1, 10, 1, 1));
  }

  @Test
  void shouldRejectNegativeTier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeaponGenerator().generateWeapon(WeaponType.DAGGER, -1));
  }

  // --- Added tests below (existing tests above are unchanged) ---

  // Creating a Dagger stores the correct type, windup, and damage (parity with Sword/Bow coverage).
  @Test
  void shouldCreateDagger() {
    WeaponItem dagger = new WeaponItem("Basic Dagger", WeaponType.DAGGER, 9, 3, 1, 1);

    assertEquals("Basic Dagger", dagger.getName());
    assertEquals(ItemType.WEAPON, dagger.getItemType());
    assertEquals(WeaponType.DAGGER, dagger.getWeaponType());
    assertEquals(1, dagger.getWindupDuration());
    assertEquals(9, dagger.getDamage());
  }

  // A windup duration of exactly zero is accepted (matches the "instant attack" weapon case).
  @Test
  void shouldAcceptZeroWindupDuration() {
    assertDoesNotThrow(
        () -> new WeaponItem("Fast Dagger", WeaponType.DAGGER, WeaponTier.TIER_1, 3, 1, 1),
        "Expected a windupDuration of exactly 0 to be accepted as a valid boundary value.");
  }

  // setWindupDuration() updates the stored windup duration for an existing weapon.
  @Test
  void shouldUpdateWindupDurationViaSetter() {
    WeaponItem sword = new WeaponItem("Basic Sword", WeaponType.SWORD, WeaponTier.TIER_2, 10, 1, 1);

    sword.setWindupDuration(5f);

    assertEquals(
        5f,
        sword.getWindupDuration(),
        "Expected getWindupDuration() to reflect the value set via setWindupDuration().");
  }
}
