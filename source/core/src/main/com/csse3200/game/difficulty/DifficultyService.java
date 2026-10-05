package com.csse3200.game.difficulty;

/**
 * Holds the player's currently selected {@link Difficulty}.
 *
 * <p>A plain static holder, not a {@code ServiceLocator} service - {@code ServiceLocator.clear()}
 * runs on every screen change, which would silently reset the player's choice back to NORMAL
 * mid-game. This class outlives that clearing, the same way {@link
 * com.csse3200.game.perks.PerkService} outlives it for perk progress.
 */
public final class DifficultyService {
  private static Difficulty current = Difficulty.NORMAL;

  private DifficultyService() {}

  /**
   * @return the currently selected difficulty; NORMAL until setCurrent() is called.
   */
  public static Difficulty getCurrent() {
    return current;
  }

  /** Sets the player's selected difficulty, e.g. from a difficulty-select menu. */
  public static void setCurrent(Difficulty difficulty) {
    current = difficulty;
  }
}
