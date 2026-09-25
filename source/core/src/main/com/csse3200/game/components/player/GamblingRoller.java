package com.csse3200.game.components.player;

import java.util.Random;

/**
 * Picks a prize slot from a {@link GamblingCatalogs.SpinCatalog} by weight.
 *
 * <p>Each slot is chosen with probability {@code weight / totalWeight}, so a rarer prize is just a
 * smaller weight. The roll uses the same approach as {@link
 * com.csse3200.game.components.loot.LootTable#rollItem()}: draw a number in {@code [0,
 * totalWeight)} and walk the slots subtracting their weights until it drops below zero.
 *
 * <p>Slots are always walked in order {@code 1}..{@link
 * GamblingCatalogs.SpinCatalog#PRIZE_SLOT_COUNT}, never in map iteration order, so the same random
 * draw always lands on the same slot. The random source is passed in so callers and tests control
 * the outcome.
 */
public final class GamblingRoller {
  private GamblingRoller() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /**
   * Returns the sum of the weights of every prize slot in a catalog.
   *
   * @param catalog catalog to sum; must be non-null
   * @return total weight, always {@code > 0} because every prize weight is positive
   * @throws IllegalArgumentException if {@code catalog} is null
   */
  public static int totalWeight(GamblingCatalogs.SpinCatalog catalog) {
    requireCatalog(catalog);
    int total = 0;
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      total += catalog.getPrize(slot).getWeight();
    }
    return total;
  }

  /**
   * Returns the chance that a roll lands on a slot.
   *
   * @param catalog catalog to read; must be non-null
   * @param slot prize slot
   * @return {@code weight / totalWeight}, or {@code 0} if {@code slot} is outside {@code 1}..{@code
   *     5}
   * @throws IllegalArgumentException if {@code catalog} is null
   */
  public static float probability(GamblingCatalogs.SpinCatalog catalog, int slot) {
    requireCatalog(catalog);
    GamblingCatalogs.PrizeEntry<?> prize = catalog.getPrize(slot);
    if (prize == null) {
      return 0f;
    }
    return prize.getWeight() / (float) totalWeight(catalog);
  }

  /**
   * Rolls one prize slot by weight.
   *
   * @param catalog catalog to roll; must be non-null
   * @param random random source; must be non-null
   * @return the chosen slot, {@code 1}..{@code 5}
   * @throws IllegalArgumentException if {@code catalog} or {@code random} is null
   */
  public static int rollSlot(GamblingCatalogs.SpinCatalog catalog, Random random) {
    requireCatalog(catalog);
    if (random == null) {
      throw new IllegalArgumentException("Random must not be null.");
    }

    // Walk the slots subtracting their weights, so a slot twice as heavy covers twice as much of
    // the range and is picked twice as often.
    int pick = random.nextInt(totalWeight(catalog));
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      pick -= catalog.getPrize(slot).getWeight();
      if (pick < 0) {
        return slot;
      }
    }

    // Only reachable if the weights and total disagree, which the catalog prevents.
    return GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT;
  }

  private static void requireCatalog(GamblingCatalogs.SpinCatalog catalog) {
    if (catalog == null) {
      throw new IllegalArgumentException("SpinCatalog must not be null.");
    }
  }
}
