package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WeaponUpgraderTest {

  private final WeaponUpgrader upgrader = new WeaponUpgrader();

  @Test
  void shouldRejectNullWeapon() {
    assertThrows(IllegalArgumentException.class, () -> upgrader.upgrade(null));
  }

  @Test
  void shouldUpgradeSwordThroughEachTier() {
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem sword1 = generator.generateWeapon(WeaponType.SWORD, 1);

    WeaponItem sword2 = upgrader.upgrade(sword1);
    assertEquals(2, sword2.getTier());
    assertEquals(WeaponType.SWORD, sword2.getWeaponType());

    WeaponItem sword3 = upgrader.upgrade(sword2);
    assertEquals(3, sword3.getTier());
  }

  @Test
  void shouldRejectUpgradingAMaxTierSword() {
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem sword3 = generator.generateWeapon(WeaponType.SWORD, 3);

    assertThrows(IllegalStateException.class, () -> upgrader.upgrade(sword3));
  }

  @Test
  void shouldUpgradeBowThroughEachTier() {
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem bow1 = generator.generateWeapon(WeaponType.BOW, 1);

    WeaponItem bow2 = upgrader.upgrade(bow1);
    assertEquals(2, bow2.getTier());
    assertEquals(WeaponType.BOW, bow2.getWeaponType());
  }

  @Test
  void shouldUpgradeDaggerThroughItsTiers() {
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem dagger1 = generator.generateWeapon(WeaponType.DAGGER, 1);

    WeaponItem dagger2 = upgrader.upgrade(dagger1);
    assertEquals(2, dagger2.getTier());
    assertEquals(WeaponType.DAGGER, dagger2.getWeaponType());
  }

  @Test
  void shouldTurnAMaxTierDaggerIntoATier1Sword() {
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem dagger3 = generator.generateWeapon(WeaponType.DAGGER, 3);

    WeaponItem upgraded = upgrader.upgrade(dagger3);

    assertEquals(WeaponType.SWORD, upgraded.getWeaponType());
    assertEquals(1, upgraded.getTier());
  }
}
