package com.csse3200.game.components.player;

import com.csse3200.game.services.GameTime;

/**
 * A shield whose state can be saved and restored by save/load: whether one is held but not used
 * yet, and how long an active one has left. Implemented by {@link ShieldComponent} and {@link
 * BallisticShieldComponent}, so save/load can treat both the same way.
 *
 * <p>Time left is saved instead of the game clock time, because the game clock starts again from
 * zero on every new screen.
 */
public interface RestorableShield {

  /**
   * @return true if a shield is held but has not been used yet
   */
  boolean hasShield();

  /**
   * @return the time left on the active shield in milliseconds, or 0 if none is active
   */
  long getRemainingMillis();

  /**
   * Puts back a shield that was held when the game was saved. This is not a new pickup, so it is
   * not counted by quests.
   *
   * @param held whether a shield was held
   */
  void restoreHeldShield(boolean held);

  /**
   * Puts back an active shield with the time it had left. Zero or less is ignored.
   *
   * @param remainingMillis the time the shield had left, in milliseconds
   */
  void restoreActive(long remainingMillis);

  /**
   * Shared time left calculation for every shield.
   *
   * @param active whether the shield is active
   * @param timeSource the game clock, or null if there is none
   * @param activeUntil the game clock time the shield runs out at
   * @return the milliseconds left, never negative, or 0 if the shield is not active
   */
  static long remainingMillis(boolean active, GameTime timeSource, long activeUntil) {
    if (!active || timeSource == null) {
      return 0L;
    }
    return Math.max(0L, activeUntil - timeSource.getTime());
  }
}
