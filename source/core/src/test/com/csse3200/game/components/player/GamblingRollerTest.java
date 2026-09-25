package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(GameExtension.class)
class GamblingRollerTest {
  private static final int[] WEIGHTS = {40, 25, 20, 10, 5};

  @Test
  void shouldSumEveryPrizeWeight() {
    assertEquals(100, GamblingRoller.totalWeight(catalog(WEIGHTS)));
    assertEquals(5, GamblingRoller.totalWeight(catalog(1, 1, 1, 1, 1)));
  }

  @Test
  void shouldReturnWeightOverTotalAsProbability() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);

    assertEquals(0.40f, GamblingRoller.probability(catalog, 1), 1e-6f);
    assertEquals(0.25f, GamblingRoller.probability(catalog, 2), 1e-6f);
    assertEquals(0.20f, GamblingRoller.probability(catalog, 3), 1e-6f);
    assertEquals(0.10f, GamblingRoller.probability(catalog, 4), 1e-6f);
    assertEquals(0.05f, GamblingRoller.probability(catalog, 5), 1e-6f);
  }

  @Test
  void shouldHaveProbabilitiesThatSumToOne() {
    GamblingCatalogs.SpinCatalog catalog = catalog(7, 13, 29, 3, 48);

    float sum = 0f;
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      sum += GamblingRoller.probability(catalog, slot);
    }
    assertEquals(1f, sum, 1e-6f);
  }

  @Test
  void shouldReturnZeroProbabilityForInvalidSlot() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);

    assertEquals(0f, GamblingRoller.probability(catalog, 0));
    assertEquals(0f, GamblingRoller.probability(catalog, 6));
  }

  /** Every draw in {@code [0, total)} maps to the slot whose weight range contains it. */
  @ParameterizedTest
  @CsvSource({
    "0, 1", "39, 1", "40, 2", "64, 2", "65, 3", "84, 3", "85, 4", "94, 4", "95, 5", "99, 5"
  })
  void shouldMapDrawToSlotAtWeightBoundaries(int draw, int expectedSlot) {
    FixedRandom random = new FixedRandom(draw);

    assertEquals(expectedSlot, GamblingRoller.rollSlot(catalog(WEIGHTS), random));
  }

  @Test
  void shouldDrawOnceWithTotalWeightAsBound() {
    FixedRandom random = new FixedRandom(0);

    GamblingRoller.rollSlot(catalog(WEIGHTS), random);

    assertEquals(1, random.calls);
    assertEquals(100, random.lastBound);
  }

  @Test
  void shouldRollSlotsInProportionToTheirWeight() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);
    Random random = new Random(42L);
    int rolls = 100_000;
    int[] counts = new int[GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT + 1];

    for (int i = 0; i < rolls; i++) {
      counts[GamblingRoller.rollSlot(catalog, random)]++;
    }

    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      float expected = GamblingRoller.probability(catalog, slot);
      float actual = counts[slot] / (float) rolls;
      assertTrue(
          Math.abs(actual - expected) < 0.01f,
          "slot " + slot + ": expected about " + expected + ", got " + actual);
    }
  }

  @Test
  void shouldRollTheSameSlotsForTheSameSeed() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);
    Random first = new Random(7L);
    Random second = new Random(7L);

    for (int i = 0; i < 50; i++) {
      assertEquals(
          GamblingRoller.rollSlot(catalog, first), GamblingRoller.rollSlot(catalog, second));
    }
  }

  @Test
  void shouldRejectNullArguments() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);
    Random random = new Random();

    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.totalWeight(null));
    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.probability(null, 1));
    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.rollSlot(null, random));
    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.rollSlot(catalog, null));
  }

  private static GamblingCatalogs.SpinCatalog catalog(int... weights) {
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes = new HashMap<>();
    for (int slot = 1; slot <= weights.length; slot++) {
      prizes.put(slot, new GamblingCatalogs.PrizeEntry<>("prize " + slot, weights[slot - 1]));
    }
    return new GamblingCatalogs.SpinCatalog(0, prizes);
  }

  /** Random stub that always draws the same value and records how it was called. */
  private static class FixedRandom extends Random {
    private final int value;
    private int calls;
    private int lastBound;

    private FixedRandom(int value) {
      this.value = value;
    }

    @Override
    public int nextInt(int bound) {
      calls++;
      lastBound = bound;
      return value;
    }
  }
}
