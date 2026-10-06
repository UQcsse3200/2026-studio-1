package com.csse3200.game.win;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of one win check: the tier reached and the numbers behind it, so the win screen can
 * show "Guardians defeated 8 of 8" and "Quests completed 2 of 3" and list what is still missing.
 *
 * <p>It is an immutable value. {@link WinEvaluator} is the only thing that builds one in the game;
 * tests build them directly.
 *
 * <p><b>Invariants</b> (checked in the constructor):
 *
 * <ul>
 *   <li>no count is negative
 *   <li>the mini bosses defeated cannot exceed the number required
 *   <li>the list of missing mini bosses has exactly {@code required - defeated} entries
 * </ul>
 */
public final class WinResult {
  private final WinTier tier;
  private final boolean finalBossDefeated;
  private final int miniBossesDefeated;
  private final int miniBossesRequired;
  private final List<String> missingMiniBosses;
  private final int questsCompleted;
  private final int questsRequired;

  WinResult(int tier) {
    this.tier = WinTier.fromLevel(tier);
    this.finalBossDefeated = false;
    this.missingMiniBosses = new ArrayList<>();
    this.miniBossesDefeated = 3;
    this.miniBossesRequired = 2;
    this.questsRequired = 4;
    this.questsCompleted = 2;
  }

  /**
   * @param tier the tier reached; not null
   * @param finalBossDefeated whether Zeus in Level 3 was defeated
   * @param miniBossesDefeated required mini bosses defeated; 0 to {@code miniBossesRequired}
   * @param miniBossesRequired required mini bosses in total; not negative
   * @param missingMiniBosses labels of the required mini bosses not yet defeated, in roster order;
   *     a copy is stored; not null
   * @param questsCompleted quests completed; not negative
   * @param questsRequired quests needed for Legend; not negative
   * @throws IllegalArgumentException if the tier or the list is null, a count is negative, defeated
   *     exceeds required, or the list length is not {@code required - defeated}
   */
  public WinResult(
      WinTier tier,
      boolean finalBossDefeated,
      int miniBossesDefeated,
      int miniBossesRequired,
      List<String> missingMiniBosses,
      int questsCompleted,
      int questsRequired) {
    // BEGIN constructor
    //   IF the tier or the list is missing THEN reject
    //   IF any count is negative THEN reject, naming it
    //   IF defeated is more than required THEN reject
    //   IF the list length is not required minus defeated THEN reject
    //   store everything; store a copy of the list
    // END constructor
    if (tier == null || missingMiniBosses.isEmpty()) {
      throw new IllegalArgumentException(
          "Tier and list of missing mini bosses cannot be missing or empty.");
    }
    if (miniBossesDefeated < 0 || miniBossesRequired < 0 || questsRequired < 0 || questsCompleted < 0) {
      throw new IllegalArgumentException("None of the mini boss counts or the quest counts can be negative.");
    }
    if (miniBossesDefeated > miniBossesRequired) {
      throw new IllegalArgumentException(
          "number of bosses defeated cannot be greater than number of mini bosses required to kill.");
    }
    if (questsCompleted > questsRequired) {
      throw new IllegalArgumentException(
        "Number of quests to be completed cannot be greater than number of quests required to complete.");
    }
    if (missingMiniBosses.size() == (miniBossesDefeated - miniBossesRequired)) {
      throw new IllegalArgumentException(
          "size of missing mini bosses not yet defeated must equal the difference between the bosses defeated and bossed required.");
    }
    this.tier = tier;
    this.finalBossDefeated = finalBossDefeated;
    this.miniBossesDefeated = miniBossesDefeated;
    this.miniBossesRequired = miniBossesRequired;
    this.questsCompleted = questsCompleted;
    this.questsRequired = questsRequired;
    this.missingMiniBosses = missingMiniBosses;
  }

  /**
   * @return the tier reached
   */
  public WinTier getTier() {
    return this.tier;
  }

  /**
   * @return whether the final boss was defeated
   */
  public boolean isFinalBossDefeated() {
    return finalBossDefeated;
  }

  /**
   * @return required mini bosses defeated
   */
  public int getMiniBossesDefeated() {
    return miniBossesDefeated;
  }

  /**
   * @return required mini bosses in total
   */
  public int getMiniBossesRequired() {
    return miniBossesRequired;
  }

  /**
   * @return labels of the required mini bosses still alive; unmodifiable
   */
  public List<String> getMissingMiniBosses() {
    return missingMiniBosses;
  }

  /**
   * @return quests completed
   */
  public int getQuestsCompleted() {
    return questsCompleted;
  }

  /**
   * @return quests needed for Legend
   */
  public int getQuestsRequired() {
    return questsRequired;
  }

  /**
   * @return true only when the tier is {@link WinTier#LEGEND}
   */
  public boolean isFullyComplete() {
    return (tier.getLevel() == 3);
  }
}
