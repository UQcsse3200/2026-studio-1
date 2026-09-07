package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootTableTest {
  private static final long SEED = 1234L;

  /** Acceptance criterion: the same seed produces the same loot, so a run can be reproduced. */
  @Test
  void shouldProduceTheSameLootForTheSameSeed() {
    List<String> first = rollNames(LootTable.createDefault(SEED), 20);
    List<String> second = rollNames(LootTable.createDefault(SEED), 20);

    assertEquals(first, second);
  }

  /** A different seed gives a different run, otherwise seeding would be pointless. */
  @Test
  void shouldProduceDifferentLootForADifferentSeed() {
    List<String> first = rollNames(LootTable.createDefault(SEED), 20);
    List<String> second = rollNames(LootTable.createDefault(SEED + 1), 20);

    assertTrue(first.size() == second.size());
    assertTrue(!first.equals(second), "a different seed should not repeat the same run");
  }

  /** Acceptance criterion: entries are rolled in proportion to their weight. */
  @Test
  void shouldRollEntriesInProportionToTheirWeight() {
    LootTable table =
        new LootTable(SEED).addWeapon(WeaponType.SWORD, 1, 90).addWeapon(WeaponType.BOW, 1, 10);

    Map<String, Integer> counts = countNames(table, 1000);

    int swords = counts.getOrDefault("Basic Sword", 0);
    int bows = counts.getOrDefault("Basic Bow", 0);
    assertEquals(1000, swords + bows);
    assertTrue(swords > 850 && swords < 950, "expected about 900 swords, got " + swords);
    assertTrue(bows > 50 && bows < 150, "expected about 100 bows, got " + bows);
  }

  /** Acceptance criterion: the rolled item comes out with its tier already applied. */
  @Test
  void shouldApplyTheTierToTheRolledItem() {
    LootTable table = new LootTable(SEED).addWeapon(WeaponType.SWORD, 2, 1);

    Item item = table.rollItem();

    WeaponItem weapon = assertInstanceOf(WeaponItem.class, item);
    assertEquals(20, weapon.getDamage());
  }

  /** Consumables and weapons come out of the same table. */
  @Test
  void shouldRollBothPotionsAndWeapons() {
    LootTable table =
        new LootTable(SEED)
            .addConsumable(ConsumableType.HEALTH_POTION, 1, 50)
            .addWeapon(WeaponType.SWORD, 1, 50);

    Map<String, Integer> counts = countNames(table, 200);

    assertTrue(counts.getOrDefault("Health Potion", 0) > 0, "expected some potions");
    assertTrue(counts.getOrDefault("Basic Sword", 0) > 0, "expected some weapons");
  }

  /** Acceptance criterion: higher tiers are rarer in the standard table. */
  @Test
  void shouldMakeHigherTiersRarerInTheDefaultTable() {
    LootTable table = LootTable.createDefault(SEED);

    Map<Integer, Integer> tierCounts = new HashMap<>();
    for (int i = 0; i < 2000; i++) {
      Item item = table.rollItem();
      if (item instanceof WeaponItem weapon) {
        // WeaponGenerator scales damage by tier, so damage identifies the tier it rolled.
        int tier = weapon.getDamage() / (weapon.getWeaponType() == WeaponType.SWORD ? 10 : 7);
        tierCounts.merge(tier, 1, Integer::sum);
      }
    }

    int tier1 = tierCounts.getOrDefault(1, 0);
    int tier2 = tierCounts.getOrDefault(2, 0);
    int tier3 = tierCounts.getOrDefault(3, 0);
    assertTrue(
        tier1 > tier2, "tier 1 (" + tier1 + ") should be commoner than tier 2 (" + tier2 + ")");
    assertTrue(
        tier2 > tier3, "tier 2 (" + tier2 + ") should be commoner than tier 3 (" + tier3 + ")");
    assertTrue(tier3 > 0, "tier 3 should still appear sometimes");
  }

  /** The default table has to offer every potion, so a new potion is never unobtainable. */
  @Test
  void shouldOfferEveryConsumableTypeInTheDefaultTable() {
    LootTable table = LootTable.createDefault(SEED);

    Map<String, Integer> counts = countNames(table, 4000);
    for (ConsumableType type : ConsumableType.values()) {
      boolean found = counts.keySet().stream().anyMatch(name -> name.startsWith(baseName(type)));
      assertTrue(found, type + " should be obtainable from the default loot table");
    }
  }

  @Test
  void shouldRejectInvalidEntries() {
    LootTable table = new LootTable(SEED);

    assertThrows(IllegalArgumentException.class, () -> table.addWeapon(WeaponType.SWORD, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> table.addWeapon(WeaponType.SWORD, 0, 10));
    assertThrows(IllegalArgumentException.class, () -> table.addWeapon(null, 1, 10));
    assertThrows(
        IllegalArgumentException.class,
        () -> table.addConsumable(ConsumableType.SPEED_BUFF, 1, -1));
  }

  @Test
  void shouldRejectRollingAnEmptyTable() {
    LootTable table = new LootTable(SEED);

    assertTrue(table.isEmpty());
    assertThrows(IllegalStateException.class, table::rollItem);
  }

  /** Returns the display name a consumable type is generated with at tier 1. */
  private String baseName(ConsumableType type) {
    return new ConsumableGenerator().generateConsumable(type, 1).getName();
  }

  private List<String> rollNames(LootTable table, int rolls) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < rolls; i++) {
      names.add(table.rollItem().getName());
    }
    return names;
  }

  private Map<String, Integer> countNames(LootTable table, int rolls) {
    Map<String, Integer> counts = new HashMap<>();
    for (String name : rollNames(table, rolls)) {
      counts.merge(name, 1, Integer::sum);
    }
    return counts;
  }
}
