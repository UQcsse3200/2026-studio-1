package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link WinResult}: an immutable value with three invariants (no negative counts, defeated
 * not above required, missing list length equal to required minus defeated).
 */
class WinResultTest {
  private static final List<String> NONE_MISSING = List.of();

  private WinResult result(
    WinTier tier,
    boolean boss,
    int defeated,
    int required,
    List<String> missing,
    int quests,
    int questsRequired) {
    return new WinResult(tier, boss, defeated, required, missing, quests, questsRequired);
  }

  // ---------- storing ----------

  @Test
  void shouldStoreEveryValueItWasGiven() {
    WinResult r = result(WinTier.GLORY, true, 6, 8, List.of("A", "B"), 2, 3);

    assertEquals(WinTier.GLORY, r.getTier());
    assertTrue(r.isFinalBossDefeated());
    assertEquals(6, r.getMiniBossesDefeated());
    assertEquals(8, r.getMiniBossesRequired());
    assertEquals(List.of("A", "B"), r.getMissingMiniBosses());
    assertEquals(2, r.getQuestsCompleted());
    assertEquals(3, r.getQuestsRequired());
  }

  @Test
  void shouldAcceptAResultWithNothingRequired() {
    WinResult r = result(WinTier.VICTORY, true, 0, 0, NONE_MISSING, 0, 0);

    assertEquals(0, r.getMiniBossesRequired());
    assertTrue(r.getMissingMiniBosses().isEmpty());
  }

  @Test
  void shouldAcceptAFullyCompleteResult() {
    WinResult r = result(WinTier.LEGEND, true, 8, 8, NONE_MISSING, 3, 3);

    assertTrue(r.isFullyComplete());
  }

  // ---------- isFullyComplete ----------

  @ParameterizedTest(name = "{0} is fully complete: {1}")
  @CsvSource({"NONE, false", "VICTORY, false", "GLORY, false", "LEGEND, true"})
  void shouldBeFullyCompleteOnlyAtLegend(WinTier tier, boolean expected) {
    WinResult r = result(tier, tier.isWin(), 0, 0, NONE_MISSING, 0, 0);

    assertEquals(expected, r.isFullyComplete());
  }

  // ---------- the list is protected ----------

  @Test
  void shouldNotBeAffectedByLaterEditsToTheListItWasGiven() {
    List<String> given = new ArrayList<>(List.of("A", "B"));
    WinResult r = result(WinTier.VICTORY, true, 6, 8, given, 0, 3);

    given.add("C");
    given.clear();

    assertEquals(List.of("A", "B"), r.getMissingMiniBosses());
  }

  @Test
  void shouldHandBackAListThatCannotBeChanged() {
    WinResult r = result(WinTier.VICTORY, true, 6, 8, List.of("A", "B"), 0, 3);

    assertThrows(UnsupportedOperationException.class, () -> r.getMissingMiniBosses().add("C"));
    assertThrows(UnsupportedOperationException.class, () -> r.getMissingMiniBosses().clear());
  }

  // ---------- validation ----------

  @Test
  void shouldRejectANullTier() {
    assertThrows(
      IllegalArgumentException.class, () -> result(null, true, 0, 0, NONE_MISSING, 0, 0));
  }

  @Test
  void shouldRejectANullMissingList() {
    assertThrows(
      IllegalArgumentException.class, () -> result(WinTier.VICTORY, true, 0, 0, null, 0, 0));
  }

  @ParameterizedTest(name = "defeated {0}, required {1}, quests {2}, quests required {3}")
  @CsvSource({"-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1"})
  void shouldRejectANegativeCount(int defeated, int required, int quests, int questsRequired) {
    assertThrows(
      IllegalArgumentException.class,
      () ->
        result(
          WinTier.VICTORY, true, defeated, required, NONE_MISSING, quests, questsRequired));
  }

  @Test
  void shouldRejectMoreDefeatedThanRequired() {
    assertThrows(
      IllegalArgumentException.class,
      () -> result(WinTier.GLORY, true, 9, 8, NONE_MISSING, 0, 3));
  }

  @ParameterizedTest(name = "defeated {0} of {1} with {2} missing")
  @CsvSource({"6, 8, 0", "6, 8, 1", "6, 8, 3", "8, 8, 1", "0, 8, 7"})
  void shouldRejectAMissingListOfTheWrongLength(int defeated, int required, int missingCount) {
    List<String> missing = new ArrayList<>();
    for (int i = 0; i < missingCount; i++) {
      missing.add("boss " + i);
    }
    assertThrows(
      IllegalArgumentException.class,
      () -> result(WinTier.VICTORY, true, defeated, required, missing, 0, 3));
  }

  @Test
  void shouldKeepTheTierIndependentOfTheCounts() {
    // The result records what it was told; deciding the tier is the evaluator's job, not its.
    WinResult r = result(WinTier.NONE, false, 8, 8, NONE_MISSING, 3, 3);

    assertEquals(WinTier.NONE, r.getTier());
    assertFalse(r.isFinalBossDefeated());
  }
}
