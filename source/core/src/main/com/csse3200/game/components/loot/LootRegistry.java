package com.csse3200.game.components.loot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which loot spawn points have already been collected, independent of the player's inventory
 * or current session. Populated from saved data on load, and read back into saved data when saving.
 */
public class LootRegistry {

  private static final Set<String> collected = new HashSet<>();

  /**
   * Marks a loot spawn point as collected.
   *
   * @param id the stable ID of the collected loot spot
   */
  public static void markCollected(String id) {
    collected.add(id);
  }

  /**
   * Checks whether a loot spawn point has already been collected.
   *
   * @param id the stable ID of the loot spot to check
   * @return true if this spot has already been collected
   */
  public static boolean isCollected(String id) {
    return collected.contains(id);
  }

  /**
   * Replaces the current collected set with one loaded from saved data.
   *
   * @param ids the collected loot IDs loaded from a save file
   */
  public static void loadFrom(List<String> ids) {
    collected.clear();
    if (ids != null) {
      collected.addAll(ids);
    }
  }

  /**
   * Exports the current collected set for saving.
   *
   * @return a new list containing every currently collected loot ID
   */
  public static List<String> exportAll() {
    return new ArrayList<>(collected);
  }

  private LootRegistry() {
    throw new IllegalStateException("Utility class");
  }
}
