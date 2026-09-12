package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class WeaponGeneratorTierTest {

  @Test
  void shouldGenerateSwordWithCorrectTierDamage() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem tier1 = generator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem tier2 = generator.generateWeapon(WeaponType.SWORD, 2);
    WeaponItem tier3 = generator.generateWeapon(WeaponType.SWORD, 3);

    assertEquals(1, tier1.getTier());
    assertEquals(2, tier2.getTier());
    assertEquals(3, tier3.getTier());

    assertEquals(10, tier1.getDamage());
    assertEquals(20, tier2.getDamage());
    assertEquals(30, tier3.getDamage());
  }

  @Test
  void shouldGenerateBowWithCorrectTierDamage() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem tier1 = generator.generateWeapon(WeaponType.BOW, 1);
    WeaponItem tier2 = generator.generateWeapon(WeaponType.BOW, 2);
    WeaponItem tier3 = generator.generateWeapon(WeaponType.BOW, 3);

    assertEquals(1, tier1.getTier());
    assertEquals(2, tier2.getTier());
    assertEquals(3, tier3.getTier());

    assertEquals(7, tier1.getDamage());
    assertEquals(14, tier2.getDamage());
    assertEquals(21, tier3.getDamage());
  }

  @Test
  void shouldHaveHigherDamageForHigherSwordTiers() {
    WeaponGenerator generator = new WeaponGenerator();

    WeaponItem tier1 = generator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem tier3 = generator.generateWeapon(WeaponType.SWORD, 3);

    assertEquals(true, tier3.getDamage() > tier1.getDamage());
  }
}