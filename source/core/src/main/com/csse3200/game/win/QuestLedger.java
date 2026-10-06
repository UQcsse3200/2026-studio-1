package com.csse3200.game.win;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A running count of the quests the player has COMPLETED, by kind. The quest system today only
 * remembers quests that are still active (and the running totals of jumps, kills, gold spent and
 * shields collected); once a quest is turned in it is cleared and forgotten. The win scale needs
 * "how many quests were finished", so this class remembers that.
 *
 * <p><b>Kinds</b> match the four quest types already in {@code Quests/}: {@link #JUMP}, {@link
 * #ENEMIES_KILLED}, {@link #GOLD_SPENT} and {@link #SHIELDS_COLLECTED}. Hidden tortoises are not
 * quests and are counted separately, in {@link TortoiseLedger}.
 *
 * <p><b>Who writes to it:</b> only {@code QuestGiverComponent}, at the moment a quest is turned in
 * with a progress of 100 or more. A quest that is cleared at less than 100 is abandoned and must
 * NOT be recorded.
 *
 * <p><b>Saving:</b> the state is static, like {@code Quest} and {@code EnemyRegistry}. It must be
 * loaded from the save file when a game is loaded and reset when a new game starts, otherwise
 * completed quests would leak between runs.
 *
 * <p><b>Limitations:</b> it counts completions, not distinct quests, so the same kind completed
 * twice counts twice. It does not know how many quests the game offers; that is {@link
 * WinEvaluator#QUESTS_REQUIRED_FOR_LEGEND}'s job.
 *
 * <p><b>Style reference:</b> {@code EnemyRegistry} (static state with load and export).
 */
public final class QuestLedger {

  /** Quest kind: do a number of jumps. */
  public static final String JUMP = "jump";

  /** Quest kind: defeat a number of enemies. */
  public static final String ENEMIES_KILLED = "enemiesKilled";

  /** Quest kind: spend an amount of gold. */
  public static final String GOLD_SPENT = "goldSpent";

  /** Quest kind: collect a number of shields. */
  public static final String SHIELDS_COLLECTED = "shieldsCollected";

  /** Every kind the ledger accepts. Anything else is rejected or ignored. */
  private static final Set<String> VALID_KINDS =
    Set.of(JUMP, ENEMIES_KILLED, GOLD_SPENT, SHIELDS_COLLECTED);

  /** How many of each kind have been completed. A kind with no entry has a count of zero. */
  private static final Map<String, Integer> completed = new HashMap<>();

  private QuestLedger() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Records one completed quest by adding one to that kind's count.
   *
   * @param kind one of the kind constants
   * @throws IllegalArgumentException if the kind is null, blank or not one of the constants
   */
  public static void recordCompleted(String kind) {
    if (kind == null || !VALID_KINDS.contains(kind)) {
      throw new IllegalArgumentException("unknown quest kind: " + kind);
    }
    completed.merge(kind, 1, Integer::sum);
  }

  /**
   * Adds up the quests completed across all four kinds.
   *
   * @return how many quests have been completed in total
   */
  public static int getCompletedCount() {
    int total = 0;
    for (int count : completed.values()) {
      total += count;
    }
    return total;
  }

  /**
   * Looks up the count for one kind.
   *
   * @param kind a kind constant
   * @return how many of that kind were completed; 0 for a kind never completed or unknown
   */
  public static int getCompleted(String kind) {
    if (kind == null) {
      return 0;
    }
    return completed.getOrDefault(kind, 0);
  }

  /**
   * Lists the quest kinds that have been completed at least once.
   *
   * @return the kinds completed at least once; a copy, so the caller cannot change the ledger
   */
  public static Set<String> getCompletedKinds() {
    return new HashSet<>(completed.keySet());
  }

  /** Forgets every completed quest. Call it when a new game starts. */
  public static void reset() {
    completed.clear();
  }

  /**
   * Replaces the ledger with saved counts. Unknown kinds and counts of zero or below are ignored;
   * null clears the ledger.
   *
   * <p>The values are read as {@code Number}, not {@code Integer}, because the save file is parsed
   * without type information: a count written as a whole number can come back as a Long or a Float.
   *
   * @param saved a map of kind to count, or null
   */
  public static void loadFrom(Map<String, ? extends Number> saved) {
    completed.clear();
    if (saved == null) {
      return;
    }
    for (Map.Entry<String, ? extends Number> entry : saved.entrySet()) {
      Number count = entry.getValue();
      if (VALID_KINDS.contains(entry.getKey()) && count != null && count.intValue() > 0) {
        completed.put(entry.getKey(), count.intValue());
      }
    }
  }

  /**
   * Copies the ledger for the save file. Kinds that have never been completed are left out.
   *
   * @return a copy of the counts by kind, for writing to the save file
   */
  public static Map<String, Integer> exportAll() {
    return new HashMap<>(completed);
  }
}
