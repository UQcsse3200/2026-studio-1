package com.csse3200.game.perks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Tracks which hidden tortoises have already been found - independent of the current save file, the
 * same {@link Preferences}-based persistence {@link PerkService} itself uses, not the save-file
 * system. Survives death/revive (this class is never touched there), but is cleared by {@link
 * #resetAll()} on every fresh game start (Start or Restart, not Load) - see {@code
 * MainGameScreen}'s constructor - the same way {@link PerkService} resets.
 *
 * <p>Package-private: {@link TortoiseFactory} is the public surface other packages (e.g. {@code
 * LevelGameArea}) use to check this before deciding whether to spawn a given tortoise at all.
 */
final class TortoiseRegistry {
  private static final String PREFS_NAME = "perks_tortoises";
  private static final String FOUND_SUFFIX = ".found";
  private static final Preferences prefs =
      (Gdx.app != null) ? Gdx.app.getPreferences(PREFS_NAME) : null;

  /**
   * @param tortoiseId stable id for one specific hidden tortoise (e.g. {@code "level1_a"})
   * @return {@code true} if that tortoise has already been found
   */
  static boolean isFound(String tortoiseId) {
    return prefs != null && prefs.getBoolean(tortoiseId + FOUND_SUFFIX, false);
  }

  /**
   * Marks one specific tortoise as found, forever.
   *
   * @param tortoiseId stable id for the tortoise that was just found
   */
  static void markFound(String tortoiseId) {
    if (prefs == null) {
      return;
    }
    prefs.putBoolean(tortoiseId + FOUND_SUFFIX, true);
    prefs.flush();
  }

  /**
   * Clears every tortoise's found state. Called on a fresh game start (not a loaded save, not a
   * death/revive) - see {@code MainGameScreen}'s constructor.
   */
  static void resetAll() {
    if (prefs == null) {
      return;
    }
    prefs.clear();
    prefs.flush();
  }

  private TortoiseRegistry() {}
}
