package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerkDefinitionsTest {

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    // PerkService is all-static: wipe its registry so each test starts from nothing
    Field f = PerkService.class.getDeclaredField("perksById");
    f.setAccessible(true);
    ((Map<String, Perk>) f.get(null)).clear();

    PerkDefinitions.registerAll();

    // register() reloads saved state from Preferences, so force a clean slate in memory
    for (Perk perk : PerkService.getAllPerks()) {
      perk.restoreState(0, false, false);
    }
  }

  private void assertPerk(
      String id, String name, String description, String eventKey, int threshold) {
    Perk perk = PerkService.getPerk(id);
    assertNotNull(perk, "perk not registered: " + id);
    assertEquals(name, perk.getName());
    assertEquals(description, perk.getDescription());
    assertEquals(eventKey, perk.getEventKey());
    assertEquals(threshold, perk.getThreshold());
  }

  @Test
  void registersExactlyFourPerks() {
    assertEquals(4, PerkService.getAllPerks().size());
  }

  @Test
  void registersPerksInDeclaredOrder() {
    List<String> ids = PerkService.getAllPerks().stream().map(Perk::getId).toList();
    assertEquals(List.of("shieldMaster", "thickSkin", "timeLord", "snatcher"), ids);
  }

  @Test
  void shieldMasterDefinition() {
    assertPerk(
        "shieldMaster", "Shield Enhancement", "Collect shield 5 times", "shieldCollected", 5);
  }

  @Test
  void thickSkinDefinition() {
    assertPerk("thickSkin", "Max Health", "Kill 10 enemies", "enemyKilled", 10);
  }

  @Test
  void timeLordDefinition() {
    assertPerk("timeLord", "Time Freeze", "Find the turtle", "Tortoise", 1);
  }

  @Test
  void snatcherDefinition() {
    assertPerk("snatcher", "Loot Drop", "Spend 120 amount of gold", "GoldSpent", 120);
  }

  @Test
  void allPerkIdsAreUnique() {
    Set<String> ids = new HashSet<>();
    for (Perk perk : PerkService.getAllPerks()) {
      assertTrue(ids.add(perk.getId()), "duplicate id: " + perk.getId());
    }
  }

  @Test
  void everyPerkHasPositiveThresholdAndNonBlankText() {
    for (Perk perk : PerkService.getAllPerks()) {
      assertTrue(perk.getThreshold() > 0, perk.getId());
      assertFalse(perk.getName().isBlank(), perk.getId());
      assertFalse(perk.getDescription().isBlank(), perk.getId());
      assertFalse(perk.getEventKey().isBlank(), perk.getId());
    }
  }

  @Test
  void registerAllIsSafeToCallTwice() {
    PerkDefinitions.registerAll();
    assertEquals(4, PerkService.getAllPerks().size());
  }

  @Test
  void recordingTheRightEventKeyUnlocksTheMatchingPerk() {
    PerkService.recordEvent("Tortoise", 1);

    assertTrue(PerkService.getPerk("timeLord").isUnlocked());
    assertFalse(PerkService.getPerk("thickSkin").isUnlocked());
  }

  @Test
  void eventKeysAreCaseSensitive() {
    PerkService.recordEvent("tortoise", 1);
    PerkService.recordEvent("goldSpent", 500);

    assertFalse(PerkService.getPerk("timeLord").isUnlocked());
    assertEquals(0, PerkService.getPerk("snatcher").getProgress());
  }

  @Test
  void progressBuildsUpTowardsThreshold() {
    PerkService.recordEvent("enemyKilled", 9);
    assertEquals(9, PerkService.getPerk("thickSkin").getProgress());
    assertFalse(PerkService.getPerk("thickSkin").isUnlocked());

    PerkService.recordEvent("enemyKilled", 1);
    assertTrue(PerkService.getPerk("thickSkin").isUnlocked());
  }

  @Test
  void utilityClassCannotBeInstantiatedAndIsFinal() throws Exception {
    assertTrue(Modifier.isFinal(PerkDefinitions.class.getModifiers()));
    Constructor<PerkDefinitions> ctor = PerkDefinitions.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(ctor.getModifiers()));
    ctor.setAccessible(true);
    assertNotNull(ctor.newInstance()); // covers the private constructor
  }
}
