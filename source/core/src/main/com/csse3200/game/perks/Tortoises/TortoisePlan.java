package com.csse3200.game.perks.Tortoises;

import java.util.HashMap;
import java.util.Map;

/**
 * Which maps hold how many random tortoises in this game. Decided once at the start of a game by
 * {@link TortoisePlanner}, saved with the game, and read by {@link TortoiseCarrierAssigner} each
 * time a map is built.
 *
 * <p>Static state, like {@code TortoiseLedger}: cleared when a new game starts and restored when a
 * save is loaded.
 */
public final class TortoisePlan {
  /** Map name to how many random tortoises it holds. */
  private static final Map<String, Integer> counts = new HashMap<>();

  /**
   * Replaces the plan.
   *
   * @param plan map name to how many random tortoises it holds; null is treated as empty
   * @throws IllegalArgumentException if any count is negative or null
   */
  public static void set(Map<String, Integer> plan) {
    if (plan != null) {
      // Check everything first, so a bad plan leaves the old one untouched
      for (Map.Entry<String, Integer> entry : plan.entrySet()) {
        if (entry.getValue() == null || entry.getValue() < 0) {
          throw new IllegalArgumentException("Tortoise count must not be null or negative.");
        }
      }
    }
    counts.clear();
    if (plan != null) {
      counts.putAll(plan);
    }
  }

  /**
   * @param mapName the map's name, as in {@code MapData.getName()}
   * @return how many random tortoises that map holds; 0 if it is not in the plan
   */
  public static int countFor(String mapName) {
    return counts.getOrDefault(mapName, 0);
  }

  /**
   * @return a copy of the plan, for the save file
   */
  public static Map<String, Integer> exportAll() {
    return new HashMap<>(counts);
  }

  /** Forgets the plan. Called when a new game starts. */
  public static void reset() {
    counts.clear();
  }

  private TortoisePlan() {
    throw new IllegalStateException("Utility class");
  }
}
