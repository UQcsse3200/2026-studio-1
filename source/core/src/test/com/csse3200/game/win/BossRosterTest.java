package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the data in {@link BossRoster}: the final boss id, the required mini bosses and the
 * escorts. These tests pin the roster as the design document states it. {@code BossRosterMapsTest}
 * then checks that the same ids really exist in the map files.
 */
class BossRosterTest {
  private static final String FINAL = "Level 3 - Zeus's Palace:26,4";

  @Test
  void shouldNameTheFinalBossAsZeusInLevelThree() {
    assertEquals(FINAL, BossRoster.FINAL_BOSS_ID);
  }

  @Test
  void shouldRecogniseOnlyTheFinalBossAsTheFinalBoss() {
    assertTrue(BossRoster.isFinalBoss(FINAL));
    assertFalse(
        BossRoster.isFinalBoss("Zeus's Thunder Hall:26,12"),
        "the Thunder Hall guardian is a mini boss");
    assertFalse(BossRoster.isFinalBoss("Level 2 \u2014 Climb Mount Olympus:53,148"));
    assertFalse(BossRoster.isFinalBoss(null));
    assertFalse(BossRoster.isFinalBoss(""));
  }

  @Test
  void shouldNeverListTheFinalBossAsAMiniBoss() {
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      assertFalse(BossRoster.isFinalBoss(boss.getId()), boss.getId());
    }
  }

  @Test
  void shouldRequireEightMiniBosses() {
    assertEquals(8, BossRoster.getRequiredCount());
    assertEquals(8, BossRoster.getRequiredIds().size());
  }

  @Test
  void shouldListElevenEntriesInAll() {
    assertEquals(11, BossRoster.getMiniBosses().size(), "eight required and three escorts");
  }

  @Test
  void shouldHaveEightRequiredEntriesAndThreeEscorts() {
    long required =
        BossRoster.getMiniBosses().stream().filter(BossRoster.MiniBoss::isRequired).count();
    long escorts = BossRoster.getMiniBosses().size() - required;

    assertEquals(8, required);
    assertEquals(3, escorts);
  }

  @Test
  void shouldContainEachRequiredBossOfTheDesignDocument() {
    Set<String> required = BossRoster.getRequiredIds();

    assertTrue(required.contains("Hound's Den:48,2"));
    assertTrue(required.contains("Gorgon's Gallery:37,11"));
    assertTrue(required.contains("Minotaur's Labyrinth:24,35"));
    assertTrue(required.contains("Shades' Barracks:40,17"));
    assertTrue(required.contains("Cyclops Forge:30,15"));
    assertTrue(required.contains("Centaur Pavilion:44,12"));
    assertTrue(required.contains("Zeus's Outer Guard:24,16"));
    assertTrue(required.contains("Zeus's Thunder Hall:26,12"));
  }

  @Test
  void shouldKeepTheEscortsOutOfTheRequiredSet() {
    Set<String> required = BossRoster.getRequiredIds();

    assertFalse(required.contains("Minotaur's Labyrinth:28,17"));
    assertFalse(required.contains("Centaur Pavilion:40,5"));
    assertFalse(required.contains("Zeus's Outer Guard:32,9"));
  }

  @Test
  void shouldHaveExactlyOneRequiredBossPerRoom() {
    java.util.Map<String, Integer> perRoom = new java.util.HashMap<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (boss.isRequired()) {
        perRoom.merge(boss.getRoomName(), 1, Integer::sum);
      }
    }

    assertEquals(8, perRoom.size(), "eight rooms");
    perRoom.forEach(
        (room, count) -> assertEquals(1, count, room + " needs exactly one required boss"));
  }

  @Test
  void shouldGiveEveryEntryAUniqueId() {
    Set<String> ids = new HashSet<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      assertTrue(ids.add(boss.getId()), "duplicate id " + boss.getId());
    }
  }

  @Test
  void shouldGiveEveryEntryTheFormOfAKillId() {
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      assertTrue(
          boss.getId().matches(".+:\\d+,\\d+"), boss.getId() + " should be <map name>:<x>,<y>");
      assertTrue(
          boss.getId().startsWith(boss.getRoomName() + ":"),
          boss.getId() + " should start with its room name");
    }
  }

  @Test
  void shouldGiveEveryEntryAReadableLabelAndABossType() {
    Set<String> bossTypes = Set.of("cerberus", "medusa", "minotaur", "cyclops", "centaur", "zeus");
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      assertFalse(boss.getLabel().isBlank(), boss.getId());
      assertTrue(
          bossTypes.contains(boss.getEnemyType()),
          boss.getId() + " has type " + boss.getEnemyType());
    }
  }

  @Test
  void shouldHandBackAListThatCannotBeChanged() {
    List<BossRoster.MiniBoss> bosses = BossRoster.getMiniBosses();

    assertThrows(UnsupportedOperationException.class, bosses::clear);
    assertThrows(UnsupportedOperationException.class, () -> bosses.add(bosses.get(0)));
  }

  @Test
  void shouldHandBackASetThatCannotBeChanged() {
    Set<String> ids = BossRoster.getRequiredIds();

    assertThrows(UnsupportedOperationException.class, ids::clear);
    assertThrows(UnsupportedOperationException.class, () -> ids.add("x:1,1"));
  }

  @Test
  void shouldReturnTheSameRosterEveryTime() {
    assertEquals(BossRoster.getRequiredIds(), BossRoster.getRequiredIds());
    assertEquals(BossRoster.getMiniBosses().size(), BossRoster.getMiniBosses().size());
  }

  // ---------- the MiniBoss value ----------

  @Test
  void shouldStoreTheValuesOfAMiniBoss() {
    BossRoster.MiniBoss boss =
        new BossRoster.MiniBoss("Room:1,2", "A label", "Room", "cyclops", true);

    assertEquals("Room:1,2", boss.getId());
    assertEquals("A label", boss.getLabel());
    assertEquals("Room", boss.getRoomName());
    assertEquals("cyclops", boss.getEnemyType());
    assertTrue(boss.isRequired());
    assertNotNull(boss);
  }

  @ParameterizedTest(name = "blank id \"{0}\" is rejected")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "   "})
  void shouldRejectABlankIdLabelRoomOrType(String blank) {
    assertThrows(
        IllegalArgumentException.class, () -> new BossRoster.MiniBoss(blank, "l", "r", "t", true));
    assertThrows(
        IllegalArgumentException.class, () -> new BossRoster.MiniBoss("i", blank, "r", "t", true));
    assertThrows(
        IllegalArgumentException.class, () -> new BossRoster.MiniBoss("i", "l", blank, "t", true));
    assertThrows(
        IllegalArgumentException.class, () -> new BossRoster.MiniBoss("i", "l", "r", blank, true));
  }
}
