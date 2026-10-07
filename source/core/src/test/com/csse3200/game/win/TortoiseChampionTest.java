package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the Tortoise Champion honour: {@link TortoiseLedger}, which counts the hidden tortoises
 * found, and how {@link WinResult} and {@link WinEvaluator} carry that count.
 *
 * <p>No tortoise exists in the game yet, so the total is 0 in play. These tests set a total by hand
 * to prove the honour works the moment tortoises are added. The ledger is static state, so every
 * test starts and finishes with it cleared.
 */
class TortoiseChampionTest {

  @BeforeEach
  void setUp() {
    TortoiseLedger.reset();
    TortoiseLedger.setTotal(0);
  }

  @AfterEach
  void tearDown() {
    TortoiseLedger.reset();
    TortoiseLedger.setTotal(0);
  }

  // ---------- the ledger ----------

  @Test
  void shouldStartWithNoTortoisesHiddenOrFound() {
    assertEquals(0, TortoiseLedger.getTotal());
    assertEquals(0, TortoiseLedger.getFoundCount());
    assertFalse(TortoiseLedger.isComplete(), "nothing to find means nothing has been completed");
  }

  @Test
  void shouldNameTheHonour() {
    assertEquals("Tortoise Champion", TortoiseLedger.CHAMPION_TITLE);
  }

  @Test
  void shouldCountEachTortoiseOnceHoweverOftenItIsFound() {
    TortoiseLedger.recordFound("Hound's Den:3,2");
    TortoiseLedger.recordFound("Hound's Den:3,2");
    TortoiseLedger.recordFound("Cyclops Forge:9,8");

    assertEquals(2, TortoiseLedger.getFoundCount());
  }

  @ParameterizedTest(name = "id \"{0}\" is rejected")
  @NullAndEmptySource
  @ValueSource(strings = {" ", "   "})
  void shouldRejectABlankTortoiseId(String id) {
    assertThrows(IllegalArgumentException.class, () -> TortoiseLedger.recordFound(id));
  }

  @Test
  void shouldRejectANegativeTotal() {
    assertThrows(IllegalArgumentException.class, () -> TortoiseLedger.setTotal(-1));
  }

  @ParameterizedTest(name = "{0} found of {1}: complete {2}")
  @CsvSource({"0, 0, false", "0, 3, false", "2, 3, false", "3, 3, true", "1, 1, true"})
  void shouldBeCompleteOnlyWhenEveryHiddenTortoiseIsFound(int found, int total, boolean expected) {
    TortoiseLedger.setTotal(total);
    for (int i = 0; i < found; i++) {
      TortoiseLedger.recordFound("tortoise " + i);
    }

    assertEquals(expected, TortoiseLedger.isComplete());
  }

  @Test
  void shouldForgetFoundTortoisesButKeepTheTotalWhenReset() {
    TortoiseLedger.setTotal(4);
    TortoiseLedger.recordFound("a");

    TortoiseLedger.reset();

    assertEquals(0, TortoiseLedger.getFoundCount());
    assertEquals(4, TortoiseLedger.getTotal(), "the total belongs to the maps, not to one run");
  }

  @Test
  void shouldRestoreWhatItExported() {
    TortoiseLedger.recordFound("a");
    TortoiseLedger.recordFound("b");
    List<String> saved = TortoiseLedger.exportAll();
    TortoiseLedger.reset();

    TortoiseLedger.loadFrom(saved);

    assertEquals(2, TortoiseLedger.getFoundCount());
  }

  @Test
  void shouldExportACopyThatCannotChangeTheLedger() {
    TortoiseLedger.recordFound("a");

    TortoiseLedger.exportAll().clear();

    assertEquals(1, TortoiseLedger.getFoundCount());
  }

  @Test
  void shouldClearTheFoundTortoisesWhenLoadingNullAndSkipBlankIds() {
    TortoiseLedger.recordFound("old");

    TortoiseLedger.loadFrom(null);
    assertEquals(0, TortoiseLedger.getFoundCount());

    TortoiseLedger.loadFrom(new ArrayList<>(Arrays.asList("a", null, " ", "b")));
    assertEquals(2, TortoiseLedger.getFoundCount());
  }

  // ---------- the result ----------

  @Test
  void shouldNotBeAChampionWhenNoTortoisesExist() {
    WinResult result = new WinResult(WinTier.LEGEND, true, 0, 0, List.of(), 0, 0, 0, 0);

    assertFalse(result.isTortoiseChampion(), "an honour nobody can earn is never handed out");
  }

  @ParameterizedTest(name = "{0} found of {1}: champion {2}")
  @CsvSource({"0, 5, false", "4, 5, false", "5, 5, true"})
  void shouldBeAChampionOnlyWithEveryTortoiseFound(int found, int total, boolean expected) {
    WinResult result = new WinResult(WinTier.VICTORY, true, 0, 0, List.of(), 0, 0, found, total);

    assertEquals(found, result.getTortoisesFound());
    assertEquals(total, result.getTortoisesTotal());
    assertEquals(expected, result.isTortoiseChampion());
  }

  @Test
  void shouldRejectANegativeTortoiseCount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WinResult(WinTier.VICTORY, true, 0, 0, List.of(), 0, 0, -1, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WinResult(WinTier.VICTORY, true, 0, 0, List.of(), 0, 0, 0, -1));
  }

  @Test
  void shouldReportNoTortoisesFromTheShortConstructor() {
    WinResult result = new WinResult(WinTier.VICTORY, true, 0, 0, List.of(), 0, 0);

    assertEquals(0, result.getTortoisesTotal());
    assertFalse(result.isTortoiseChampion());
  }

  // ---------- the evaluator ----------

  @Test
  void shouldNeverChangeTheTierBecauseOfTortoises() {
    assertEquals(WinTier.VICTORY, WinEvaluator.evaluate(true, List.of(), 0, 9, 9).getTier());
    assertEquals(WinTier.NONE, WinEvaluator.evaluate(false, List.of(), 0, 9, 9).getTier());
  }

  @Test
  void shouldMakeAChampionOfAPlainVictoryWithEveryTortoiseFound() {
    WinResult result = WinEvaluator.evaluate(true, List.of(), 0, 6, 6);

    assertEquals(WinTier.VICTORY, result.getTier());
    assertTrue(result.isTortoiseChampion(), "the honour does not depend on the tier");
  }

  @Test
  void shouldReadTheTortoisesFromTheLedgerWhenEvaluatingNow() {
    TortoiseLedger.setTotal(2);
    TortoiseLedger.recordFound("a");
    TortoiseLedger.recordFound("b");

    WinResult result = WinEvaluator.evaluateNow(true);

    assertEquals(2, result.getTortoisesFound());
    assertEquals(2, result.getTortoisesTotal());
    assertTrue(result.isTortoiseChampion());
  }

  @Test
  void shouldNotMakeAChampionInTheGameAsItShipsToday() {
    assertFalse(WinEvaluator.evaluateNow(true).isTortoiseChampion());
  }
}
