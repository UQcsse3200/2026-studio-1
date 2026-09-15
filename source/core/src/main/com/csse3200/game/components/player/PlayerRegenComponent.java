package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heals the owning entity gradually while a regeneration potion is active.
 *
 * <p>Regeneration is not a stat multiplier, so it is kept out of {@link PlayerBuffComponent} and
 * handled here instead: health is restored one tick at a time until the duration runs out. Ticking
 * is driven by {@link GameTime} rather than by counting frames, matching how buffs are timed, so a
 * slow frame heals the ticks it skipped instead of losing them.
 *
 * <p>The same "strongest wins" rule as buffs applies: a stronger potion replaces a weaker
 * regeneration, and a weaker one is rejected so {@code ConsumableUseComponent} leaves it in the
 * inventory.
 *
 * <p>Healing is capped at the entity's maximum health, which is read from {@link
 * ConsumableUseComponent} because {@code CombatStatsComponent} is shared with other teams and has
 * no maximum health field.
 */
public class PlayerRegenComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerRegenComponent.class);

  /** How often a regeneration heals, in milliseconds. */
  private static final long TICK_MILLIS = 1000L;

  private GameTime timeSource;
  private boolean regenerating;
  private int healPerTick;
  private long nextTickTime;
  private long endTime;

  /** Stores the game clock used to time ticks. */
  @Override
  public void create() {
    timeSource = ServiceLocator.getTimeSource();
  }

  /**
   * Starts regenerating health, replacing any weaker regeneration already running.
   *
   * <p>A regeneration is allowed at full health: the healing arrives over several seconds, so it is
   * still worth drinking before a fight rather than being wasted.
   *
   * @param healPerTick health restored each tick; must be {@code > 0}
   * @param durationSeconds how long regeneration lasts, in seconds; must be {@code > 0}
   * @return {@code true} if regeneration started
   */
  public boolean startRegen(int healPerTick, float durationSeconds) {
    if (healPerTick <= 0 || durationSeconds <= 0f) {
      logger.debug("Rejecting regen of {} per tick for {}s", healPerTick, durationSeconds);
      return false;
    }

    if (regenerating && healPerTick < this.healPerTick) {
      logger.debug("Rejecting regen of {}, a stronger one is active", healPerTick);
      return false;
    }

    long now = timeSource.getTime();
    this.healPerTick = healPerTick;
    this.nextTickTime = now + TICK_MILLIS;
    this.endTime = now + (long) (durationSeconds * 1000);
    this.regenerating = true;

    logger.debug("Regenerating {} health per tick until {}", healPerTick, endTime);
    if (entity != null) {
      entity.getEvents().trigger("regenStarted", healPerTick);
    }
    return true;
  }

  /** Applies any ticks that have come due, then stops once the duration has passed. */
  @Override
  public void update() {
    if (!regenerating) {
      return;
    }

    long now = timeSource.getTime();
    while (nextTickTime <= now && nextTickTime <= endTime) {
      heal();
      nextTickTime += TICK_MILLIS;
    }

    if (now >= endTime) {
      stopRegen();
    }
  }

  /**
   * Returns whether regeneration is currently running.
   *
   * @return {@code true} while a regeneration is active
   */
  public boolean isRegenerating() {
    return regenerating;
  }

  /**
   * Returns how much health each tick restores.
   *
   * @return heal per tick, or 0 when nothing is regenerating
   */
  public int getHealPerTick() {
    return regenerating ? healPerTick : 0;
  }

  /** Restores one tick of health, stopping at the entity's maximum health. */
  private void heal() {
    CombatStatsComponent stats =
        entity == null ? null : entity.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }

    int maxHealth = ConsumableUseComponent.maxHealthOf(entity);
    int healed = Math.min(stats.getHealth() + healPerTick, maxHealth);
    if (healed > stats.getHealth()) {
      stats.setHealth(healed);
    }
  }

  /** Ends the current regeneration and tells listeners it is over. */
  private void stopRegen() {
    regenerating = false;
    healPerTick = 0;
    logger.debug("Regeneration finished");
    if (entity != null) {
      entity.getEvents().trigger("regenExpired");
    }
  }
}
