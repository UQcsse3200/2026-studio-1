package com.csse3200.game.win;

import java.util.List;

/**
 * The outcome of one win check: the tier reached and the numbers behind it, so the win screen can
 * show how many guardians, quests and tortoises were done and list what is still missing.
 *
 * <p>It is an immutable value. {@link WinEvaluator} is the only thing that builds one in the game.
 * No count is negative, the mini bosses defeated never exceed the number required, and the list of
 * missing mini bosses has exactly {@code required - defeated} entries.
 */
public final class WinResult {
  private final WinTier tier;
  private final boolean finalBossDefeated;
  private final int miniBossesDefeated;
  private final int miniBossesRequired;
  private final List<String> missingMiniBosses;
  private final int questsCompleted;
  private final int questsRequired;
  private final int tortoisesFound;
  private final int tortoisesTotal;

  /**
   * Creates a result for a game with no tortoises hidden in it.
   *
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
    this(
        tier,
        finalBossDefeated,
        miniBossesDefeated,
        miniBossesRequired,
        missingMiniBosses,
        questsCompleted,
        questsRequired,
        0,
        0);
  }

  /**
   * Creates a result, including the tortoise count.
   *
   * @param tier the tier reached; not null
   * @param finalBossDefeated whether Zeus in Level 3 was defeated
   * @param miniBossesDefeated required mini bosses defeated; 0 to {@code miniBossesRequired}
   * @param miniBossesRequired required mini bosses in total; not negative
   * @param missingMiniBosses labels of the required mini bosses not yet defeated; not null
   * @param questsCompleted quests completed; not negative
   * @param questsRequired quests needed for Legend; not negative
   * @param tortoisesFound hidden tortoises found; not negative
   * @param tortoisesTotal hidden tortoises in the game; not negative, 0 while none exist
   * @throws IllegalArgumentException as the seven-argument constructor, or if a tortoise count is
   *     negative
   */
  public WinResult(
      WinTier tier,
      boolean finalBossDefeated,
      int miniBossesDefeated,
      int miniBossesRequired,
      List<String> missingMiniBosses,
      int questsCompleted,
      int questsRequired,
      int tortoisesFound,
      int tortoisesTotal) {
    if (tier == null) {
      throw new IllegalArgumentException("tier must not be null");
    }
    if (missingMiniBosses == null) {
      throw new IllegalArgumentException("missingMiniBosses must not be null");
    }
    requireNotNegative(miniBossesDefeated, "miniBossesDefeated");
    requireNotNegative(miniBossesRequired, "miniBossesRequired");
    requireNotNegative(questsCompleted, "questsCompleted");
    requireNotNegative(questsRequired, "questsRequired");
    requireNotNegative(tortoisesFound, "tortoisesFound");
    requireNotNegative(tortoisesTotal, "tortoisesTotal");
    if (miniBossesDefeated > miniBossesRequired) {
      throw new IllegalArgumentException("miniBossesDefeated must not exceed miniBossesRequired");
    }
    if (missingMiniBosses.size() != miniBossesRequired - miniBossesDefeated) {
      throw new IllegalArgumentException(
          "missingMiniBosses must list every required mini boss not yet defeated");
    }
    this.tier = tier;
    this.finalBossDefeated = finalBossDefeated;
    this.miniBossesDefeated = miniBossesDefeated;
    this.miniBossesRequired = miniBossesRequired;
    this.missingMiniBosses = List.copyOf(missingMiniBosses);
    this.questsCompleted = questsCompleted;
    this.questsRequired = questsRequired;
    this.tortoisesFound = tortoisesFound;
    this.tortoisesTotal = tortoisesTotal;
  }

  private static void requireNotNegative(int count, String name) {
    if (count < 0) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }

  /**
   * Returns the tier reached.
   *
   * @return the tier reached
   */
  public WinTier getTier() {
    return tier;
  }

  /**
   * Says whether the final boss was defeated.
   *
   * @return whether the final boss was defeated
   */
  public boolean isFinalBossDefeated() {
    return finalBossDefeated;
  }

  /**
   * Returns required mini bosses defeated.
   *
   * @return required mini bosses defeated
   */
  public int getMiniBossesDefeated() {
    return miniBossesDefeated;
  }

  /**
   * Returns required mini bosses in total.
   *
   * @return required mini bosses in total
   */
  public int getMiniBossesRequired() {
    return miniBossesRequired;
  }

  /**
   * Returns labels of the required mini bosses still alive.
   *
   * @return labels of the required mini bosses still alive; unmodifiable
   */
  public List<String> getMissingMiniBosses() {
    return missingMiniBosses;
  }

  /**
   * Returns quests completed.
   *
   * @return quests completed
   */
  public int getQuestsCompleted() {
    return questsCompleted;
  }

  /**
   * Returns quests needed for Legend.
   *
   * @return quests needed for Legend
   */
  public int getQuestsRequired() {
    return questsRequired;
  }

  /**
   * Returns hidden tortoises found.
   *
   * @return hidden tortoises found
   */
  public int getTortoisesFound() {
    return tortoisesFound;
  }

  /**
   * Returns hidden tortoises in the game.
   *
   * @return hidden tortoises in the game; 0 while none exist
   */
  public int getTortoisesTotal() {
    return tortoisesTotal;
  }

  /**
   * Says whether the player earned the Tortoise Champion honour.
   *
   * @return true when the game hides at least one tortoise and all of them were found
   */
  public boolean isTortoiseChampion() {
    return tortoisesTotal > 0 && tortoisesFound >= tortoisesTotal;
  }

  /**
   * Says whether the top tier was reached, so nothing is left to achieve.
   *
   * @return true only when the tier is {@link WinTier#LEGEND}
   */
  public boolean isFullyComplete() {
    return tier == WinTier.LEGEND;
  }
}
