package com.csse3200.game.components.loot;

import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.entities.Entity;

/**
 * Applies a temporary stat modifier that reverts automatically after a fixed duration.
 *
 * <p>The timing and reverting are owned by {@link PlayerBuffComponent}; this effect only describes
 * what to apply.
 */
public class BuffEffect implements ConsumableEffect {
  private final BuffStat stat;
  private final float magnitude;
  private final float durationSeconds;

  /**
   * Creates a buff effect.
   *
   * @param stat stat to modify
   * @param magnitude multiplier to apply, where 1.0 is no change; must be {@code > 0}
   * @param durationSeconds how long the buff lasts, in seconds; must be {@code > 0}
   * @throws IllegalArgumentException if any argument is null or out of range
   */
  public BuffEffect(BuffStat stat, float magnitude, float durationSeconds) {
    if (stat == null) {
      throw new IllegalArgumentException("BuffStat must not be null.");
    }
    if (magnitude <= 0f) {
      throw new IllegalArgumentException("magnitude must be greater than 0.");
    }
    if (durationSeconds <= 0f) {
      throw new IllegalArgumentException("durationSeconds must be greater than 0.");
    }
    this.stat = stat;
    this.magnitude = magnitude;
    this.durationSeconds = durationSeconds;
  }

  /**
   * Returns the stat this effect modifies.
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
   * Returns how long the buff lasts.
   *
   * @return duration in seconds
   */
  public float getDurationSeconds() {
    return durationSeconds;
  }

  /**
   * Starts the buff on the entity.
   *
   * @param entity entity to buff
   * @return {@code false} if the entity cannot hold buffs
   */
  @Override
  public boolean apply(Entity entity) {
    if (entity == null) {
      return false;
    }
    PlayerBuffComponent buffs = entity.getComponent(PlayerBuffComponent.class);
    if (buffs == null) {
      return false;
    }
    return buffs.applyBuff(stat, magnitude, durationSeconds);
  }
}
