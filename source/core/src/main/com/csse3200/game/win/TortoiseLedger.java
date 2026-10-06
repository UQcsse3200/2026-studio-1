package com.csse3200.game.win;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks the tortoises hidden across the maps, for the Tortoise Champion honour on the win screen:
 * a player who has found every one of them is a Tortoise Champion, whatever their {@link WinTier}.
 *
 * <p><b>Not playable yet.</b> No tortoise exists in any map, so {@link #getTotal()} is 0 and the
 * honour cannot be earned. When tortoises are added, whatever places them calls {@link
 * #setTotal(int)} with how many the game hides, and whatever lets the player find one calls {@link
 * #recordFound(String)} with a stable id (the {@code <map name>:<x>,<y>} form that kills use works
 * well). Nothing else in the win system needs to change.
 *
 * <p>The state is static, like {@code EnemyRegistry}: the found ids are loaded from the save file
 * when a game is loaded and reset when a new game starts.
 */
public final class TortoiseLedger {

  /** The honour shown on the win screen for finding every tortoise. */
  public static final String CHAMPION_TITLE = "Tortoise Champion";

  private static final Set<String> found = new HashSet<>();
  private static int total = 0;

  private TortoiseLedger() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Sets how many tortoises the game hides.
   *
   * @param count the number hidden across every map; not negative
   * @throws IllegalArgumentException if the count is negative
   */
  public static void setTotal(int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }
    total = count;
  }

  /**
   * @return how many tortoises the game hides; 0 until tortoises exist
   */
  public static int getTotal() {
    return total;
  }

  /**
   * Records one found tortoise. Finding the same one twice counts once.
   *
   * @param id a stable id for that tortoise; not blank
   * @throws IllegalArgumentException if the id is null or blank
   */
  public static void recordFound(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    found.add(id);
  }

  /**
   * @return how many different tortoises have been found
   */
  public static int getFoundCount() {
    return found.size();
  }

  /**
   * @return true once every hidden tortoise has been found; always false while none exist
   */
  public static boolean isComplete() {
    return total > 0 && found.size() >= total;
  }

  /** Forgets every found tortoise; the total is kept. Called when a new game starts. */
  public static void reset() {
    found.clear();
  }

  /**
   * Replaces the found tortoises with saved ids. Blank ids are ignored; null clears them.
   *
   * @param ids the saved ids, or null
   */
  public static void loadFrom(Collection<String> ids) {
    found.clear();
    if (ids == null) {
      return;
    }
    for (String id : ids) {
      if (id != null && !id.isBlank()) {
        found.add(id);
      }
    }
  }

  /**
   * @return a copy of the found ids, for writing to the save file
   */
  public static List<String> exportAll() {
    return new ArrayList<>(found);
  }
}
