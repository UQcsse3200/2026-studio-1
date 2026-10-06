package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link WinTier}: four values, levels 0 to 3, a title and subtitle for each, ordering, and
 * lookup by level. Pure enum tests, so no game fixture is needed.
 */
class WinTierTest {

  @Test
  void shouldHaveExactlyFourValues() {
    assertEquals(4, WinTier.values().length);
  }

  @ParameterizedTest(name = "{0} has level {1}")
  @CsvSource({"NONE, 0", "VICTORY, 1", "GLORY, 2", "LEGEND, 3"})
  void shouldHaveTheLevelItsNameImplies(WinTier tier, int level) {
    assertEquals(level, tier.getLevel());
  }

  @Test
  void shouldListTheValuesInAscendingLevelOrder() {
    WinTier[] tiers = WinTier.values();
    for (int i = 0; i < tiers.length; i++) {
      assertEquals(i, tiers[i].getLevel(), "values() order must match the levels");
    }
  }

  @ParameterizedTest(name = "{0} has a title")
  @CsvSource({"NONE", "VICTORY", "GLORY", "LEGEND"})
  void shouldGiveEveryTierANonBlankTitle(WinTier tier) {
    assertFalse(tier.getTitle() == null || tier.getTitle().isBlank());
  }

  @Test
  void shouldGiveEveryTierADifferentTitle() {
    Set<String> titles = new HashSet<>();
    for (WinTier tier : WinTier.values()) {
      titles.add(tier.getTitle());
    }
    assertEquals(4, titles.size());
  }

  @ParameterizedTest(name = "{0} explains itself")
  @CsvSource({"VICTORY", "GLORY", "LEGEND"})
  void shouldGiveEveryWinningTierASubtitle(WinTier tier) {
    assertFalse(tier.getSubtitle() == null || tier.getSubtitle().isBlank());
  }

  @Test
  void shouldGiveNoneAnEmptySubtitleNotNull() {
    assertEquals("", WinTier.NONE.getSubtitle());
  }

  @Test
  void shouldNameTheTiersAsTheDesignDoes() {
    assertEquals("Victory", WinTier.VICTORY.getTitle());
    assertEquals("Glory", WinTier.GLORY.getTitle());
    assertEquals("Legend", WinTier.LEGEND.getTitle());
  }

  @Test
  void shouldCountEveryTierExceptNoneAsAWin() {
    assertFalse(WinTier.NONE.isWin());
    assertTrue(WinTier.VICTORY.isWin());
    assertTrue(WinTier.GLORY.isWin());
    assertTrue(WinTier.LEGEND.isWin());
  }

  @ParameterizedTest(name = "{0} is at least {1}: {2}")
  @CsvSource({
    "NONE, NONE, true",
    "NONE, VICTORY, false",
    "VICTORY, NONE, true",
    "VICTORY, VICTORY, true",
    "VICTORY, GLORY, false",
    "GLORY, VICTORY, true",
    "GLORY, LEGEND, false",
    "LEGEND, GLORY, true",
    "LEGEND, LEGEND, true",
    "LEGEND, NONE, true"
  })
  void shouldCompareTiersByLevel(WinTier tier, WinTier other, boolean expected) {
    assertEquals(expected, tier.isAtLeast(other));
  }

  @Test
  void shouldRejectComparingWithNull() {
    assertThrows(IllegalArgumentException.class, () -> WinTier.GLORY.isAtLeast(null));
  }

  @ParameterizedTest(name = "level {0} gives {1}")
  @CsvSource({"0, NONE", "1, VICTORY", "2, GLORY", "3, LEGEND"})
  void shouldFindATierByItsLevel(int level, WinTier expected) {
    assertSame(expected, WinTier.fromLevel(level));
  }

  @ParameterizedTest(name = "level {0} is rejected")
  @ValueSource(ints = {-1, 4, 5, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void shouldRejectALevelOutsideZeroToThree(int level) {
    assertThrows(IllegalArgumentException.class, () -> WinTier.fromLevel(level));
  }

  @Test
  void shouldRoundTripEveryTierThroughItsLevel() {
    for (WinTier tier : WinTier.values()) {
      assertSame(tier, WinTier.fromLevel(tier.getLevel()));
    }
  }

  @Test
  void shouldKeepDistinctTiersDistinct() {
    assertNotEquals(WinTier.VICTORY, WinTier.GLORY);
  }
}
