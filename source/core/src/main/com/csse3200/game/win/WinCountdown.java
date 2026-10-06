package com.csse3200.game.win;

/** Waits a short time after the final boss dies before the win screen is shown. */
public final class WinCountdown {
  private final float delaySeconds;
  private float remaining;
  private boolean started = false;
  private boolean finished = false;

  public WinCountdown(float delaySeconds) {
    if (delaySeconds < 0f || Float.isNaN(delaySeconds) || Float.isInfinite(delaySeconds)) {
      throw new IllegalArgumentException("delaySeconds must be zero or more and finite");
    }
    this.delaySeconds = delaySeconds;
  }

  /** Returns true exactly once: on the frame the delay runs out. */
  public boolean update(boolean finalBossDefeated, float deltaSeconds) {
    if (finished || !finalBossDefeated) {
      return false;
    }
    if (!started) {
      started = true;
      remaining = delaySeconds;
    } else {
      remaining -= Math.max(0f, deltaSeconds);
    }
    if (remaining <= 0f) {
      finished = true;
      return true;
    }
    return false;
  }

  public boolean isPending() {
    return started && !finished;
  }

  public boolean isFinished() {
    return finished;
  }

  public float getDelaySeconds() {
    return delaySeconds;
  }
}