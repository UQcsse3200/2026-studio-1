package com.csse3200.game.win;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A running count of the quests the player has COMPLETED, by kind. The quest system today only
 * remembers quests that are still active (and the running totals of jumps, kills, gold spent and
 * shields collected); once a quest is turned in it is cleared and forgotten. The win scale needs
 * "how many quests were finished", so this class remembers that.
 *
 * <p><b>Kinds</b> match the four quest types already in {@code Quests/}: {@link #JUMP}, {@link
 * #ENEMIES_KILLED}, {@link #GOLD_SPENT} and {@link #SHIELDS_COLLECTED}.
 *
 * <p><b>Who writes to it:</b> only {@code QuestGiverComponent}, at the moment a quest is turned in
 * with a progress of 100 or more (see {@code 06_wiring.md}). A quest that is cleared at less than
 * 100 is abandoned and must NOT be recorded.
 *
 * <p><b>Saving:</b> the state is static, like {@code Quest} and {@code EnemyRegistry}. It must be
 * loaded from the save file when a game is loaded and reset when a new game starts, otherwise
 * completed quests would leak between runs.
 *
 * <p><b>Limitations:</b> it counts completions, not distinct quests, so the same kind completed
 * twice counts twice. It does not know how many quests the game offers; that is {@link
 * WinEvaluator#QUESTS_REQUIRED_FOR_LEGEND}'s job. At the time of writing no NPC on the enemies
 * branch hands out quests, so the count stays at zero until team 2's NPCs are merged.
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

  private QuestLedger() {
    // utility class
  }

  /**
   * Records one completed quest.
   *
   * @param kind one of the four kind constants
   * @throws IllegalArgumentException if the kind is null, blank or not one of the four
   */
  public static void recordCompleted(String kind) {
    // BEGIN recordCompleted
    //   IF the kind is missing, blank or not one of the four known kinds THEN reject with
    //     "unknown quest kind: <kind>"
    //   add one to the count for that kind
    // END recordCompleted
  }

  /**
   * @return how many quests have been completed in total, across all kinds
   */
  public static int getCompletedCount() {
    // BEGIN getCompletedCount
    //   add up the counts of all four kinds
    // END getCompletedCount
    return 0;
  }

  /**
   * @param kind a kind constant
   * @return how many quests of that kind were completed; 0 for a kind never completed or unknown
   */
  public static int getCompleted(String kind) {
    // BEGIN getCompleted
    //   give back the count for the kind, or zero if there is none
    // END getCompleted
    return 0;
  }

  /**
   * @return the kinds completed at least once; a copy, so the caller cannot change the ledger
   */
  public static Set<String> getCompletedKinds() {
    // BEGIN getCompletedKinds
    //   collect every kind whose count is above zero into a new set
    // END getCompletedKinds
    return Collections.emptySet();
  }

  /** Forgets every completed quest. Call it when a new game starts. */
  public static void reset() {
    // BEGIN reset
    //   clear all counts
    // END reset
  }

  /**
   * Replaces the ledger with saved counts. Unknown kinds and counts below zero are ignored; null
   * clears the ledger.
   *
   * @param saved a map of kind to count, or null
   */
  public static void loadFrom(Map<String, Integer> saved) {
    // BEGIN loadFrom
    //   clear all counts
    //   IF the argument is missing THEN stop
    //   FOR each entry
    //     IF the kind is one of the four AND the count is above zero THEN store the count
    // END loadFrom
  }

  /**
   * @return a copy of the counts by kind, for writing to the save file
   */
  public static Map<String, Integer> exportAll() {
    // BEGIN exportAll
    //   give back a new map holding every kind with a count above zero
    // END exportAll
    return Collections.emptyMap();
  }
}
