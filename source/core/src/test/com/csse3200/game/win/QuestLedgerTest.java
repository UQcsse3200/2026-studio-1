package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link QuestLedger}, the running count of completed quests by kind.
 *
 * <p>The ledger is static state, so every test starts and finishes with a reset: a leftover count
 * from one test would silently change the next.
 */
class QuestLedgerTest {
  private static final String[] KINDS = {
    QuestLedger.JUMP,
    QuestLedger.ENEMIES_KILLED,
    QuestLedger.GOLD_SPENT,
    QuestLedger.SHIELDS_COLLECTED
  };

  @BeforeEach
  void setUp() {
    QuestLedger.reset();
  }

  @AfterEach
  void tearDown() {
    QuestLedger.reset();
  }

  // ---------- starting state ----------

  @Test
  void shouldStartEmpty() {
    assertEquals(0, QuestLedger.getCompletedCount());
    assertTrue(QuestLedger.getCompletedKinds().isEmpty());
    assertTrue(QuestLedger.exportAll().isEmpty());
  }

  @Test
  void shouldNameTheFourKindsAsTheQuestSystemDoes() {
    assertEquals("jump", QuestLedger.JUMP);
    assertEquals("enemiesKilled", QuestLedger.ENEMIES_KILLED);
    assertEquals("goldSpent", QuestLedger.GOLD_SPENT);
    assertEquals("shieldsCollected", QuestLedger.SHIELDS_COLLECTED);
  }

  // ---------- recording ----------

  @Test
  void shouldCountOneCompletedQuest() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    assertEquals(1, QuestLedger.getCompletedCount());
    assertEquals(1, QuestLedger.getCompleted(QuestLedger.JUMP));
  }

  @Test
  void shouldCountEachKindSeparately() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.GOLD_SPENT);
    QuestLedger.recordCompleted(QuestLedger.GOLD_SPENT);

    assertEquals(1, QuestLedger.getCompleted(QuestLedger.JUMP));
    assertEquals(2, QuestLedger.getCompleted(QuestLedger.GOLD_SPENT));
    assertEquals(0, QuestLedger.getCompleted(QuestLedger.ENEMIES_KILLED));
    assertEquals(3, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldCountTheSameKindCompletedTwiceAsTwo() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    assertEquals(2, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldTotalAllFourKinds() {
    for (String kind : KINDS) {
      QuestLedger.recordCompleted(kind);
    }

    assertEquals(4, QuestLedger.getCompletedCount());
    assertEquals(4, QuestLedger.getCompletedKinds().size());
  }

  @ParameterizedTest(name = "kind \"{0}\" is rejected")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "jumps", "JUMP", "gold", "unknown", "enemiesKilled "})
  void shouldRejectAnUnknownOrBlankKind(String kind) {
    assertThrows(IllegalArgumentException.class, () -> QuestLedger.recordCompleted(kind));
  }

  @Test
  void shouldNotCountARejectedKind() {
    assertThrows(IllegalArgumentException.class, () -> QuestLedger.recordCompleted("nonsense"));

    assertEquals(0, QuestLedger.getCompletedCount());
  }

  // ---------- reading ----------

  @Test
  void shouldReportZeroForAKindNeverCompletedAndForAnUnknownKind() {
    assertEquals(0, QuestLedger.getCompleted(QuestLedger.SHIELDS_COLLECTED));
    assertEquals(0, QuestLedger.getCompleted("nonsense"));
    assertEquals(0, QuestLedger.getCompleted(null));
  }

  @Test
  void shouldListOnlyTheKindsCompletedAtLeastOnce() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.SHIELDS_COLLECTED);

    assertEquals(
      Set.of(QuestLedger.JUMP, QuestLedger.SHIELDS_COLLECTED), QuestLedger.getCompletedKinds());
  }

  @Test
  void shouldHandBackACopyOfTheKindsSoCallersCannotChangeTheLedger() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    Set<String> kinds = QuestLedger.getCompletedKinds();
    kinds.clear();

    assertEquals(1, QuestLedger.getCompletedKinds().size());
  }

  // ---------- reset ----------

  @Test
  void shouldForgetEverythingWhenReset() {
    for (String kind : KINDS) {
      QuestLedger.recordCompleted(kind);
    }

    QuestLedger.reset();

    assertEquals(0, QuestLedger.getCompletedCount());
    assertTrue(QuestLedger.getCompletedKinds().isEmpty());
  }

  @Test
  void shouldBeSafeToResetTwice() {
    assertDoesNotThrow(
      () -> {
        QuestLedger.reset();
        QuestLedger.reset();
      });
  }

  // ---------- saving and loading ----------

  @Test
  void shouldExportTheCountsByKind() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.GOLD_SPENT);

    Map<String, Integer> exported = QuestLedger.exportAll();

    assertEquals(2, exported.get(QuestLedger.JUMP));
    assertEquals(1, exported.get(QuestLedger.GOLD_SPENT));
    assertFalse(
      exported.containsKey(QuestLedger.ENEMIES_KILLED), "kinds with no completions are left out");
  }

  @Test
  void shouldExportACopyThatCannotChangeTheLedger() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    Map<String, Integer> exported = QuestLedger.exportAll();
    exported.put(QuestLedger.JUMP, 99);
    exported.clear();

    assertEquals(1, QuestLedger.getCompleted(QuestLedger.JUMP));
    assertNotSame(exported, QuestLedger.exportAll());
  }

  @Test
  void shouldRestoreWhatItExported() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.ENEMIES_KILLED);
    QuestLedger.recordCompleted(QuestLedger.ENEMIES_KILLED);
    Map<String, Integer> saved = QuestLedger.exportAll();
    QuestLedger.reset();

    QuestLedger.loadFrom(saved);

    assertEquals(1, QuestLedger.getCompleted(QuestLedger.JUMP));
    assertEquals(2, QuestLedger.getCompleted(QuestLedger.ENEMIES_KILLED));
    assertEquals(3, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldReplaceTheLedgerWhenLoading() {
    QuestLedger.recordCompleted(QuestLedger.SHIELDS_COLLECTED);

    Map<String, Integer> saved = new HashMap<>();
    saved.put(QuestLedger.JUMP, 2);
    QuestLedger.loadFrom(saved);

    assertEquals(0, QuestLedger.getCompleted(QuestLedger.SHIELDS_COLLECTED), "old counts are gone");
    assertEquals(2, QuestLedger.getCompleted(QuestLedger.JUMP));
  }

  @Test
  void shouldClearTheLedgerWhenLoadingNull() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    assertDoesNotThrow(() -> QuestLedger.loadFrom(null));

    assertEquals(0, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldLoadAnOldSaveWithNoQuestDataAsAnEmptyLedger() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);

    QuestLedger.loadFrom(new HashMap<>());

    assertEquals(0, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldIgnoreUnknownKindsAndNonPositiveCountsWhenLoading() {
    Map<String, Integer> saved = new HashMap<>();
    saved.put("nonsense", 5);
    saved.put(QuestLedger.JUMP, -3);
    saved.put(QuestLedger.GOLD_SPENT, 0);
    saved.put(QuestLedger.SHIELDS_COLLECTED, 2);

    QuestLedger.loadFrom(saved);

    assertEquals(2, QuestLedger.getCompletedCount());
    assertEquals(Set.of(QuestLedger.SHIELDS_COLLECTED), QuestLedger.getCompletedKinds());
  }

  @Test
  void shouldIgnoreAKindSavedWithNoCountAtAll() {
    Map<String, Integer> saved = new HashMap<>();
    saved.put(QuestLedger.JUMP, null);
    saved.put(QuestLedger.GOLD_SPENT, 4);

    assertDoesNotThrow(() -> QuestLedger.loadFrom(saved));

    assertEquals(0, QuestLedger.getCompleted(QuestLedger.JUMP));
    assertEquals(4, QuestLedger.getCompletedCount());
  }

  @Test
  void shouldReadCountsOfAnyNumberTypeBecauseTheSaveFileDoesNotPromiseWholeNumbers() {
    // The save file is parsed without type information, so a count can come back as a Long or
    // a Float even though it was written as a whole number.
    Map<String, Number> saved = new HashMap<>();
    saved.put(QuestLedger.JUMP, 2L);
    saved.put(QuestLedger.ENEMIES_KILLED, 3.0f);

    QuestLedger.loadFrom(saved);

    assertEquals(2, QuestLedger.getCompleted(QuestLedger.JUMP));
    assertEquals(3, QuestLedger.getCompleted(QuestLedger.ENEMIES_KILLED));
  }

  @Test
  void shouldKeepCountingAfterALoad() {
    Map<String, Integer> saved = new HashMap<>();
    saved.put(QuestLedger.JUMP, 1);
    QuestLedger.loadFrom(saved);

    QuestLedger.recordCompleted(QuestLedger.JUMP);

    assertEquals(2, QuestLedger.getCompleted(QuestLedger.JUMP));
  }

  @Test
  void shouldNotBeChangedLaterByTheMapItWasLoadedFrom() {
    Map<String, Integer> saved = new HashMap<>();
    saved.put(QuestLedger.JUMP, 1);
    QuestLedger.loadFrom(saved);

    saved.put(QuestLedger.JUMP, 50);

    assertEquals(1, QuestLedger.getCompleted(QuestLedger.JUMP));
  }
}
