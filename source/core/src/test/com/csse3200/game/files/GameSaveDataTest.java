package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for {@link GameSaveData}: its default values, and that older save files missing newer
 * fields still load with those defaults. Uses the same Json instance as {@link FileLoader}, so no
 * file is read or written.
 */
@ExtendWith(GameExtension.class)
class GameSaveDataTest {

  @Test
  void newSaveDataHasPlayerDefaults() {
    GameSaveData data = new GameSaveData();

    assertEquals(0, data.health);
    assertEquals(100f, data.stamina);
    assertEquals(0, data.gold);
    assertEquals(1, data.activeSlot);
    assertNull(data.level);
    assertNull(data.difficulty);
    assertNull(data.activePetName);
  }

  @Test
  void newSaveDataHasEmptyListsAndMaps() {
    GameSaveData data = new GameSaveData();

    assertTrue(data.items.isEmpty());
    assertTrue(data.lootSeedsByRoom.isEmpty());
    assertTrue(data.collectedLootIds.isEmpty());
    assertTrue(data.killedEnemyIds.isEmpty());
    assertTrue(data.ownedPetNames.isEmpty());
    assertTrue(data.upgrades.isEmpty());
    assertTrue(data.buffs.isEmpty());
    assertTrue(data.droppedLoot.isEmpty());
    assertTrue(data.completedQuestsByKind.isEmpty());
    assertTrue(data.foundTortoiseIds.isEmpty());
    assertTrue(data.npcs.isEmpty());
    assertTrue(data.killedNpcIds.isEmpty());
    assertTrue(data.tortoisePlan.isEmpty());
  }

  @Test
  void newSaveDataHasNoTimedEffectsOrQuestProgress() {
    GameSaveData data = new GameSaveData();

    assertFalse(data.shieldHeld);
    assertEquals(0L, data.shieldRemainingMillis);
    assertFalse(data.ballisticShieldHeld);
    assertEquals(0L, data.ballisticShieldRemainingMillis);
    assertEquals(0, data.regenHealPerTick);
    assertEquals(0f, data.regenRemainingSeconds);
    assertEquals(0, data.questJumps);
    assertEquals(0, data.questEnemiesKilled);
    assertEquals(0, data.questGoldSpent);
    assertEquals(0, data.questShieldsCollected);
    assertEquals(0, data.tortoiseTotal);
  }

  @Test
  void fieldsWithNoSafeDefaultStartAsNull() {
    GameSaveData data = new GameSaveData();

    // Null tells LoadService to leave these systems unchanged.
    assertNull(data.shieldHits);
    assertNull(data.shopItemSlots);
    assertNull(data.perks);
  }

  @Test
  void eachSaveDataHasItsOwnLists() {
    GameSaveData first = new GameSaveData();
    GameSaveData second = new GameSaveData();

    first.items.add(new SavedItem());
    first.killedEnemyIds.add("enemy");

    assertNotSame(first.items, second.items);
    assertTrue(second.items.isEmpty());
    assertTrue(second.killedEnemyIds.isEmpty());
  }

  @Test
  void olderSaveWithoutNewFieldsLoadsWithDefaults() {
    String oldSave = "{\"health\": 50, \"gold\": 20, \"level\": \"maps/level1-greek.json\"}";

    GameSaveData data = FileLoader.json.fromJson(GameSaveData.class, oldSave);

    assertNotNull(data);
    assertEquals(50, data.health);
    assertEquals(20, data.gold);
    assertEquals("maps/level1-greek.json", data.level);
    assertEquals(100f, data.stamina);
    assertEquals(1, data.activeSlot);
    assertTrue(data.npcs.isEmpty());
    assertTrue(data.droppedLoot.isEmpty());
    assertFalse(data.shieldHeld);
    assertNull(data.shieldHits);
    assertNull(data.shopItemSlots);
    assertNull(data.perks);
  }

  @Test
  void newFieldsSurviveAJsonRoundTrip() {
    GameSaveData data = new GameSaveData();
    data.shieldHeld = true;
    data.shieldRemainingMillis = 4200L;
    data.ballisticShieldHeld = true;
    data.ballisticShieldRemainingMillis = 1500L;
    data.questJumps = 4;
    data.questEnemiesKilled = 2;
    data.questGoldSpent = 30;
    data.questShieldsCollected = 1;
    data.tortoiseTotal = 6;
    data.foundTortoiseIds.add("level1_a");
    data.killedNpcIds.add("npc@Level 1 - Out of the Underworld:8,3");

    SavedNpc npc = new SavedNpc();
    npc.id = "npc@Level 1 - Out of the Underworld:8,3";
    npc.dialogueState = "CHECKING";
    npc.questType = "jumpquest";
    npc.amountToDo = 5;
    data.npcs.add(npc);

    String json = FileLoader.json.toJson(data);
    GameSaveData loaded = FileLoader.json.fromJson(GameSaveData.class, json);

    assertTrue(loaded.shieldHeld);
    assertEquals(4200L, loaded.shieldRemainingMillis);
    assertTrue(loaded.ballisticShieldHeld);
    assertEquals(1500L, loaded.ballisticShieldRemainingMillis);
    assertEquals(4, loaded.questJumps);
    assertEquals(2, loaded.questEnemiesKilled);
    assertEquals(30, loaded.questGoldSpent);
    assertEquals(1, loaded.questShieldsCollected);
    assertEquals(6, loaded.tortoiseTotal);
    assertEquals("level1_a", loaded.foundTortoiseIds.get(0));
    assertEquals("npc@Level 1 - Out of the Underworld:8,3", loaded.killedNpcIds.get(0));

    assertEquals(1, loaded.npcs.size());
    SavedNpc loadedNpc = loaded.npcs.get(0);
    assertEquals("CHECKING", loadedNpc.dialogueState);
    assertEquals("jumpquest", loadedNpc.questType);
    assertEquals(5, loadedNpc.amountToDo);
  }
}
