package com.csse3200.game.components.player;

/**
 * A stat modifier that is currently applied to an entity and will revert once its duration elapses.
 *
 * <p>Instances are created by {@link PlayerBuffComponent} and are exposed read-only so a future UI
 * component (for example a buff timer) can render them without being able to mutate buff state.
 */
public class ActiveBuff {
  private final BuffStat stat;
  private final float magnitude;
  private final float durationSeconds;
  private final long endTime;
  private final int appliedDelta;

  /**
   * Creates an active buff.
   *
   * @param stat stat being modified
   * @param magnitude multiplier applied to the stat, where 1.0 is no change
   * @param durationSeconds how long the buff lasts, in seconds
   * @param endTime game time in milliseconds at which this buff expires
   * @param appliedDelta absolute change applied to the stat, used to revert it exactly
   */
  public ActiveBuff(
      BuffStat stat, float magnitude, float durationSeconds, long endTime, int appliedDelta) {
    this.stat = stat;
    this.magnitude = magnitude;
    this.durationSeconds = durationSeconds;
    this.endTime = endTime;
    this.appliedDelta = appliedDelta;
  }

  /**
   * Returns the stat this buff modifies.
   *
   * @return modified stat
   */
  public BuffStat getStat() {
    return stat;
  }

  /**
   * Returns the multiplier applied to the stat.
   *
   * @return multiplier, where 1.0 is no change
   */
  public float getMagnitude() {
    return magnitude;
  }

  /**
   * Returns the configured lifetime of this buff.
   *
   * @return duration in seconds
   */
  public float getDurationSeconds() {
    return durationSeconds;
  }

  /**
   * Returns the game time at which this buff expires.
   *
   * @return expiry time in milliseconds
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Returns the absolute change this buff made to the stat, used to revert it on expiry.
   *
   * @return applied delta; {@code 0} for stats that are not stored as an integer
   */
  public int getAppliedDelta() {
    return appliedDelta;
  }

  /**
   * Returns the time left before this buff expires. Intended for a buff timer UI.
   *
   * @param currentTime current game time in milliseconds
   * @return remaining seconds, clamped to a minimum of 0
   */
  public float getRemainingSeconds(long currentTime) {
    return Math.max(0f, (endTime - currentTime) / 1000f);
  }

  @Override
  public String toString() {
    return String.format(
        "ActiveBuff{stat=%s, magnitude=%.2f, duration=%.1fs}", stat, magnitude, durationSeconds);
  }
}
