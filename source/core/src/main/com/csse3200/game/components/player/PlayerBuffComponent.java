package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks temporary stat modifiers applied to an entity and reverts them once their duration has
 * passed.
 *
 * <p>Timing follows the same approach as WaitTask: an end time is worked out from {@link GameTime}
 * when the buff starts, and update() checks whether that time has been reached.
 *
 * <p>Stats are handled by remembering the entity's normal values in create(), then recalculating
 * them as "normal value multiplied by every active buff". Applying and expiring a buff both use
 * that one rule, so a buff never has to work out how to undo itself.
 *
 * <p>Buff state is readable so a buff timer UI can be added later without changing this class.
 * Listen for the "buffApplied" and "buffExpired" events, or call {@link #getActiveBuffs()}.
 */
public class PlayerBuffComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerBuffComponent.class);

  private final List<ActiveBuff> activeBuffs = new ArrayList<>();
  private GameTime timeSource;
  private int normalBaseAttack;
  private float speedMultiplier = 1f;

  /** Stores the game clock and the entity's unbuffed stats. */
  @Override
  public void create() {
    timeSource = ServiceLocator.getTimeSource();
    CombatStatsComponent stats = getCombatStats();
    if (stats != null) {
      normalBaseAttack = stats.getBaseAttack();
    }
  }

  /**
   * Applies a temporary stat modifier that reverts after {@code durationSeconds}.
   *
   * <p>A buff that would change nothing is rejected, which covers a duration of zero and a
   * magnitude of exactly 1.0.
   *
   * @param stat stat to modify
   * @param magnitude multiplier to apply, where 1.0 is no change
   * @param durationSeconds how long the buff lasts, in seconds; must be {@code > 0}
   * @return {@code true} if the buff was applied
   */
  public boolean applyBuff(BuffStat stat, float magnitude, float durationSeconds) {
    if (stat == null || durationSeconds <= 0f || magnitude <= 0f || magnitude == 1f) {
      logger.debug("Rejecting buff {} magnitude {} duration {}", stat, magnitude, durationSeconds);
      return false;
    }

    long endTime = timeSource.getTime() + (long) (durationSeconds * 1000);
    ActiveBuff buff = new ActiveBuff(stat, magnitude, durationSeconds, endTime);
    activeBuffs.add(buff);
    recalculateStats();

    logger.debug("Applied {} until {}", buff, endTime);
    if (entity != null) {
      entity.getEvents().trigger("buffApplied", buff);
    }
    return true;
  }

  /** Removes any buffs whose duration has passed and puts the affected stats back. */
  @Override
  public void update() {
    if (activeBuffs.isEmpty()) {
      return;
    }

    long currentTime = timeSource.getTime();
    List<ActiveBuff> expired = new ArrayList<>();
    for (ActiveBuff buff : activeBuffs) {
      if (currentTime >= buff.getEndTime()) {
        expired.add(buff);
      }
    }

    if (expired.isEmpty()) {
      return;
    }

    activeBuffs.removeAll(expired);
    recalculateStats();

    for (ActiveBuff buff : expired) {
      logger.debug("Expired {}", buff);
      if (entity != null) {
        entity.getEvents().trigger("buffExpired", buff);
      }
    }
  }

  /**
   * Returns the buffs currently applied to this entity, for display or inspection.
   *
   * @return unmodifiable view of the active buffs
   */
  public List<ActiveBuff> getActiveBuffs() {
    return Collections.unmodifiableList(activeBuffs);
  }

  /**
   * Returns whether a buff on the given stat is currently active.
   *
   * @param stat stat to check
   * @return {@code true} if at least one active buff modifies {@code stat}
   */
  public boolean hasBuff(BuffStat stat) {
    for (ActiveBuff buff : activeBuffs) {
      if (buff.getStat() == stat) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the combined movement speed multiplier from all active speed buffs.
   *
   * <p>Movement speed belongs to PlayerActions, which this component does not modify. A movement
   * component should multiply its target speed by this value for speed buffs to take effect in
   * game.
   *
   * @return speed multiplier, where 1.0 is unbuffed
   */
  public float getSpeedMultiplier() {
    return speedMultiplier;
  }

  /**
   * Sets every buffed stat back to its normal value multiplied by all active buffs on that stat.
   *
   * <p>Called whenever a buff starts or expires, so both cases share the same logic.
   */
  private void recalculateStats() {
    float damageMultiplier = 1f;
    float speed = 1f;

    for (ActiveBuff buff : activeBuffs) {
      if (buff.getStat() == BuffStat.DAMAGE) {
        damageMultiplier *= buff.getMagnitude();
      } else {
        speed *= buff.getMagnitude();
      }
    }

    speedMultiplier = speed;

    CombatStatsComponent stats = getCombatStats();
    if (stats != null) {
      stats.setBaseAttack(Math.round(normalBaseAttack * damageMultiplier));
    }
  }

  /**
   * Returns the combat stats of the owning entity, if it has any.
   *
   * @return the entity's {@link CombatStatsComponent}, or {@code null} when unavailable
   */
  private CombatStatsComponent getCombatStats() {
    return entity == null ? null : entity.getComponent(CombatStatsComponent.class);
  }
}
