package com.csse3200.game.win;

import com.csse3200.game.entities.spawn.EnemyRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides the scale of a win: given what has happened in the game, it says which of the three
 * {@link WinTier}s the player has earned, or that they have not won.
 *
 * <p>The rules, in order:
 *
 * <ol>
 *   <li>If the final boss has not been defeated, the tier is {@link WinTier#NONE}. Nothing else can
 *       win the game: killing every mini boss does not end it. The counts are still worked out.
 *   <li>Otherwise the tier is at least {@link WinTier#VICTORY}.
 *   <li>If every required mini boss in {@link BossRoster} is among the killed enemy ids, the tier
 *       is at least {@link WinTier#GLORY}.
 *   <li>If the tier is Glory and at least {@link #QUESTS_REQUIRED_FOR_LEGEND} quests have been
 *       completed, the tier is {@link WinTier#LEGEND}. Quests alone never lift a Victory.
 * </ol>
 *
 * <p>The tortoise count is carried through to the result for the Tortoise Champion honour. It never
 * changes the tier.
 *
 * <p>{@link #evaluate} takes everything as arguments and touches no static state, so it is tested
 * without a game; {@link #evaluateNow} is the thin wrapper that reads the live registries. The
 * final boss arrives as a boolean because a kill is saved when the enemy entity is disposed, just
 * after it dies, so Zeus's own id may not be in the registry yet when the win is checked.
 */
public final class WinEvaluator {

  /** Quests that must be completed for {@link WinTier#LEGEND}. */
  public static final int QUESTS_REQUIRED_FOR_LEGEND = 3;

  private WinEvaluator() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Works out the tier from explicit inputs, for a game with no tortoises hidden in it.
   *
   * @param finalBossDefeated whether Zeus in Level 3 has been defeated
   * @param killedEnemyIds every enemy id killed so far, in the {@code <map name>:<x>,<y>} form;
   *     null is treated as empty; duplicates and unknown ids are ignored
   * @param questsCompleted how many quests have been completed; not negative
   * @return the result, never null
   * @throws IllegalArgumentException if {@code questsCompleted} is negative
   */
  public static WinResult evaluate(
      boolean finalBossDefeated, Collection<String> killedEnemyIds, int questsCompleted) {
    return evaluate(finalBossDefeated, killedEnemyIds, questsCompleted, 0, 0);
  }

  /**
   * Works out the tier from explicit inputs.
   *
   * @param finalBossDefeated whether Zeus in Level 3 has been defeated
   * @param killedEnemyIds every enemy id killed so far; null is treated as empty
   * @param questsCompleted how many quests have been completed; not negative
   * @param tortoisesFound hidden tortoises found; not negative
   * @param tortoisesTotal hidden tortoises in the game; not negative
   * @return the result, never null
   * @throws IllegalArgumentException if a count is negative
   */
  public static WinResult evaluate(
      boolean finalBossDefeated,
      Collection<String> killedEnemyIds,
      int questsCompleted,
      int tortoisesFound,
      int tortoisesTotal) {
    if (questsCompleted < 0) {
      throw new IllegalArgumentException("questsCompleted must not be negative");
    }
    Set<String> killed = new HashSet<>();
    if (killedEnemyIds != null) {
      killed.addAll(killedEnemyIds);
    }
    List<String> missing = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (boss.isRequired() && !killed.contains(boss.getId())) {
        missing.add(boss.getLabel());
      }
    }
    int required = BossRoster.getRequiredCount();

    WinTier tier = WinTier.NONE;
    if (finalBossDefeated) {
      tier = WinTier.VICTORY;
      if (missing.isEmpty()) {
        tier = questsCompleted >= QUESTS_REQUIRED_FOR_LEGEND ? WinTier.LEGEND : WinTier.GLORY;
      }
    }
    return new WinResult(
        tier,
        finalBossDefeated,
        required - missing.size(),
        required,
        missing,
        questsCompleted,
        QUESTS_REQUIRED_FOR_LEGEND,
        tortoisesFound,
        tortoisesTotal);
  }

  /**
   * Works out the tier from the live game state.
   *
   * @param finalBossDefeated whether Zeus in Level 3 has been defeated
   * @return the result for the kills in {@code EnemyRegistry}, the quests in {@link QuestLedger}
   *     and the tortoises in {@link TortoiseLedger}
   */
  public static WinResult evaluateNow(boolean finalBossDefeated) {
    return evaluate(
        finalBossDefeated,
        EnemyRegistry.exportAll(),
        QuestLedger.getCompletedCount(),
        TortoiseLedger.getFoundCount(),
        TortoiseLedger.getTotal());
  }
}
