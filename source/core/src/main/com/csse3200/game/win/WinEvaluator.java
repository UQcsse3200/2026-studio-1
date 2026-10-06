package com.csse3200.game.win;

import java.util.Collection;

/**
 * The win class. It decides the SCALE of a win: given what has happened in the game, it says which
 * of the three {@link WinTier}s the player has earned, or that they have not won.
 *
 * <p><b>The rules, in order (each one is tested):</b>
 *
 * <ol>
 *   <li>If the final boss has not been defeated, the tier is {@link WinTier#NONE}. Nothing else can
 *       win the game: killing every mini boss does not end it. The counts are still worked out, so
 *       a screen could show progress.
 *   <li>Otherwise the tier is at least {@link WinTier#VICTORY}.
 *   <li>If every required mini boss in {@link BossRoster} appears among the killed enemy ids, the
 *       tier is at least {@link WinTier#GLORY}.
 *   <li>If the tier is Glory AND at least {@link #QUESTS_REQUIRED_FOR_LEGEND} quests have been
 *       completed, the tier is {@link WinTier#LEGEND}. Quests alone never lift a Victory: Legend
 *       needs Glory first.
 * </ol>
 *
 * <p><b>Why Zeus must die, and the mini bosses only scale the win.</b> Making the mini bosses an
 * alternative way to win would let a player skip the final fight and would make the side rooms
 * mandatory, which the doors do not enforce. As a scale they are optional, they pay off, and the
 * game still has one clear ending.
 *
 * <p><b>Pure logic.</b> {@link #evaluate} takes everything as arguments and touches no static
 * state, no UI and no physics, so it is tested without a game. {@link #evaluateNow} is the thin
 * wrapper that reads the live registries.
 *
 * <p><b>Why the final boss arrives as a boolean.</b> A kill is saved when the enemy entity is
 * disposed, which happens just after it dies. Zeus's own id may therefore not be in the registry
 * yet at the instant the win is checked, so the caller says whether he has fallen.
 *
 * <p><b>Limitations:</b> it does not look at deaths, time or difficulty; it does not know which
 * quests exist, only how many were completed; extra ids in the killed set (any enemy in the game)
 * are ignored.
 */
public final class WinEvaluator {

  /**
   * Quests that must be completed for {@link WinTier#LEGEND}. Set to 0 to make Legend depend only
   * on the mini bosses, for example while no NPC gives quests yet.
   */
  public static final int QUESTS_REQUIRED_FOR_LEGEND = 3;

  private WinEvaluator() {
    // utility class
  }

  /**
   * Works out the tier from explicit inputs.
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
    // BEGIN evaluate
    //   IF questsCompleted is negative THEN reject with "questsCompleted must not be negative"
    //   killed <- the killed ids as a set (empty if the argument is missing)
    //   missing <- the label of every required mini boss, in roster order, whose id is NOT in
    // killed
    //   defeated <- the required count minus the number missing
    //   tier <- NONE
    //   IF finalBossDefeated THEN
    //     tier <- VICTORY
    //     IF nothing is missing THEN
    //       tier <- GLORY
    //       IF questsCompleted is at least QUESTS_REQUIRED_FOR_LEGEND THEN tier <- LEGEND
    //     END IF
    //   END IF
    //   give back a result holding the tier, the flag, defeated, the required count, missing,
    //     questsCompleted and QUESTS_REQUIRED_FOR_LEGEND
    // END evaluate
    return new WinResult(1);
  }

  /**
   * Works out the tier from the live game state.
   *
   * @param finalBossDefeated whether Zeus in Level 3 has just been defeated
   * @return the result for the kills in {@code EnemyRegistry} and the quests in {@link QuestLedger}
   */
  public static WinResult evaluateNow(boolean finalBossDefeated) {
    // BEGIN evaluateNow
    //   killed <- every id the enemy registry has exported
    //   completed <- the ledger's completed count
    //   give back evaluate(finalBossDefeated, killed, completed)
    // END evaluateNow
    return new WinResult(1);
  }
}
