package com.csse3200.game.components.loot;

import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.entities.Entity;

/**
 * Heals the drinker gradually rather than all at once.
 *
 * <p>Like {@link BuffEffect}, this class only describes the effect. The ticking and the stopping
 * are owned by {@link PlayerRegenComponent} on the entity, so the item stays a plain data object.
 */
public class RegenerationEffect implements ConsumableEffect {
  private final int healPerTick;
  private final float durationSeconds;

  /**
   * Creates a regeneration effect.
   *
   * @param healPerTick health restored each tick; must be {@code > 0}
   * @param durationSeconds how long the regeneration lasts; must be {@code > 0}
   * @throws IllegalArgumentException if either value is not positive
   */
  public RegenerationEffect(int healPerTick, float durationSeconds) {
    if (healPerTick <= 0) {
      throw new IllegalArgumentException("healPerTick must be greater than 0.");
    }

    if (durationSeconds <= 0f) {
      throw new IllegalArgumentException("durationSeconds must be greater than 0.");
    }

    this.healPerTick = healPerTick;
    this.durationSeconds = durationSeconds;
  }

  /**
   * Returns the health restored on each tick.
   *
   * @return heal per tick
   */
  public int getHealPerTick() {
    return healPerTick;
  }

  /**
   * Returns how long the regeneration lasts.
   *
   * @return duration in seconds
   */
  public float getDurationSeconds() {
    return durationSeconds;
  }

  /**
   * Starts regenerating health on the entity.
   *
   * @param entity entity drinking the potion
   * @return {@code false} if the entity cannot regenerate or a stronger regeneration is active
   */
  @Override
  public boolean apply(Entity entity) {
    if (entity == null) {
      return false;
    }

    PlayerRegenComponent regen = entity.getComponent(PlayerRegenComponent.class);
    if (regen == null) {
      return false;
    }

    return regen.startRegen(healPerTick, durationSeconds);
  }
}
