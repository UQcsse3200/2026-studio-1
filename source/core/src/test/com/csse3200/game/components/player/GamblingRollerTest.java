package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class GamblingRollerTest {
  /** Cumulative ranges: slot 1 = 0..39, 2 = 40..64, 3 = 65..84, 4 = 85..94, 5 = 95..99. */
  private static final int[] WEIGHTS = {40, 25, 20, 10, 5};

  @Test
  void shouldSumEveryPrizeWeight() {
    assertEquals(
        100, GamblingRoller.totalWeight(catalog(WEIGHTS)), "total should add all five weights");
  }

  @ParameterizedTest
  @CsvSource({"1, 0.40", "2, 0.25", "3, 0.20", "4, 0.10", "5, 0.05"})
  void shouldReturnWeightOverTotalAsProbability(int slot, float expected) {
    assertEquals(
        expected,
        GamblingRoller.probability(catalog(WEIGHTS), slot),
        1e-6f,
        "slot " + slot + " should have probability weight / total");
  }

  @Test
  void shouldHaveProbabilitiesThatSumToOne() {
    GamblingCatalogs.SpinCatalog catalog = catalog(7, 13, 29, 3, 48);

    float sum = 0f;
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      sum += GamblingRoller.probability(catalog, slot);
    }

    assertEquals(1f, sum, 1e-6f, "probabilities of all slots should sum to 1");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 6})
  void shouldReturnZeroProbabilityForSlotOutsideCatalog(int slot) {
    assertEquals(
        0f,
        GamblingRoller.probability(catalog(WEIGHTS), slot),
        "slot " + slot + " is not a prize slot, so it can never be rolled");
  }

  /** Each draw lands on the slot whose weight range contains it, including both range edges. */
  @ParameterizedTest
  @CsvSource({
    "0, 1", "39, 1", "40, 2", "64, 2", "65, 3", "84, 3", "85, 4", "94, 4", "95, 5", "99, 5"
  })
  void shouldMapDrawToSlotAtWeightBoundaries(int draw, int expectedSlot) {
    assertEquals(
        expectedSlot,
        GamblingRoller.rollSlot(catalog(WEIGHTS), new FixedRandom(draw)),
        "draw " + draw + " should fall in slot " + expectedSlot + "'s weight range");
  }

  @Test
  void shouldDrawWithTotalWeightAsBound() {
    FixedRandom random = new FixedRandom(0);

    GamblingRoller.rollSlot(catalog(WEIGHTS), random);

    assertEquals(List.of(100), random.bounds, "roll should draw once in [0, totalWeight)");
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

    assertEquals(
        rollSequence(catalog, new Random(7L), 50),
        rollSequence(catalog, new Random(7L), 50),
        "the same seed should roll the same slots in the same order");
  }

  @Test
  void shouldRejectNullCatalogForTotalWeight() {
    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.totalWeight(null));
  }

  @Test
  void shouldRejectNullCatalogForProbability() {
    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.probability(null, 1));
  }

  @Test
  void shouldRejectNullCatalogForRoll() {
    Random random = new Random();

    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.rollSlot(null, random));
  }

  @Test
  void shouldRejectNullRandomForRoll() {
    GamblingCatalogs.SpinCatalog catalog = catalog(WEIGHTS);

    assertThrows(IllegalArgumentException.class, () -> GamblingRoller.rollSlot(catalog, null));
  }

  private static List<Integer> rollSequence(
      GamblingCatalogs.SpinCatalog catalog, Random random, int rolls) {
    List<Integer> slots = new ArrayList<>();
    for (int i = 0; i < rolls; i++) {
      slots.add(GamblingRoller.rollSlot(catalog, random));
    }
    return slots;
  }

  private static GamblingCatalogs.SpinCatalog catalog(int... weights) {
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes = new HashMap<>();
    for (int slot = 1; slot <= weights.length; slot++) {
      prizes.put(slot, new GamblingCatalogs.PrizeEntry<>("prize " + slot, weights[slot - 1]));
    }
    return new GamblingCatalogs.SpinCatalog(0, prizes);
  }

  /** Random stub that always draws the same value and records every bound it was asked for. */
  private static class FixedRandom extends Random {
    private final int value;
    private final List<Integer> bounds = new ArrayList<>();

    private FixedRandom(int value) {
      this.value = value;
    }

    @Override
    public int nextInt(int bound) {
      bounds.add(bound);
      return value;
    }
  }
}
