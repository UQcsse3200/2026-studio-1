package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks temporary stat modifiers applied to an entity and reverts them automatically once their
 * duration elapses.
 *
 * <p>Expiry uses {@link GameTime} on the same schedule as {@code WaitTask}: an absolute end time is
 * computed on application and polled in {@link #update()}. No separate timer mechanism is
 * introduced.
 *
 * <p>Buff state is deliberately observable so a future buff timer UI can consume it without this
 * component knowing about the UI. Listen for {@code "buffApplied"} and {@code "buffExpired"} (each
 * carrying the {@link ActiveBuff}), or poll {@link #getActiveBuffs()}.
 */
public class PlayerBuffComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerBuffComponent.class);

  private final List<ActiveBuff> activeBuffs = new ArrayList<>();
  private GameTime timeSource;
  private float speedMultiplier = 1f;

  /**
   * Applies a temporary stat modifier that reverts after {@code durationSeconds}.
   *
   * <p>A non-positive duration or a magnitude of exactly 1.0 is rejected, since neither would
   * change anything observable.
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

    long endTime = getTimeSource().getTime() + (long) (durationSeconds * 1000);
    int appliedDelta = applyModifier(stat, magnitude);
    ActiveBuff buff = new ActiveBuff(stat, magnitude, durationSeconds, endTime, appliedDelta);
    activeBuffs.add(buff);

    logger.debug("Applied {} until {}", buff, endTime);
    if (entity != null) {
      entity.getEvents().trigger("buffApplied", buff);
    }
    return true;
  }

  /** Expires any buffs whose duration has elapsed, reverting their stat changes. */
  @Override
  public void update() {
    if (activeBuffs.isEmpty()) {
      return;
    }
    long now = getTimeSource().getTime();
    Iterator<ActiveBuff> iterator = activeBuffs.iterator();
    while (iterator.hasNext()) {
      ActiveBuff buff = iterator.next();
      if (now >= buff.getEndTime()) {
        iterator.remove();
        revertModifier(buff);
        logger.debug("Expired {}", buff);
        if (entity != null) {
          entity.getEvents().trigger("buffExpired", buff);
        }
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
   * Returns the combined movement speed multiplier from all active {@link BuffStat#SPEED} buffs.
   *
   * <p>Movement speed is owned by {@code PlayerActions}, which is not modified by this component. A
   * movement component should multiply its target speed by this value to make speed buffs take
   * effect in game.
   *
   * @return speed multiplier, where 1.0 is unbuffed
   */
  public float getSpeedMultiplier() {
    return speedMultiplier;
  }

  /**
   * Applies a stat modifier immediately.
   *
   * @param stat stat to modify
   * @param magnitude multiplier to apply
   * @return the absolute change made, so it can be reverted exactly on expiry
   */
  private int applyModifier(BuffStat stat, float magnitude) {
    if (stat == BuffStat.SPEED) {
      speedMultiplier *= magnitude;
      return 0;
    }

    CombatStatsComponent stats = getCombatStats();
    if (stats == null) {
      return 0;
    }
    int before = stats.getBaseAttack();
    int after = Math.round(before * magnitude);
    stats.setBaseAttack(after);
    return stats.getBaseAttack() - before;
  }

  /**
   * Reverts the stat change made by an expired buff.
   *
   * @param buff buff that has expired
   */
  private void revertModifier(ActiveBuff buff) {
    if (buff.getStat() == BuffStat.SPEED) {
      speedMultiplier /= buff.getMagnitude();
      if (!hasBuff(BuffStat.SPEED)) {
        speedMultiplier = 1f;
      }
      return;
    }

    CombatStatsComponent stats = getCombatStats();
    if (stats != null) {
      stats.setBaseAttack(stats.getBaseAttack() - buff.getAppliedDelta());
    }
  }

  /**
   * Returns the combat stats of the owning entity, if any.
   *
   * @return the entity's {@link CombatStatsComponent}, or {@code null} when unavailable
   */
  private CombatStatsComponent getCombatStats() {
    return entity == null ? null : entity.getComponent(CombatStatsComponent.class);
  }

  /**
   * Resolves the game clock lazily, so this component can be constructed before the time source is
   * registered with the {@link ServiceLocator}.
   *
   * @return the registered game time source
   */
  private GameTime getTimeSource() {
    if (timeSource == null) {
      timeSource = ServiceLocator.getTimeSource();
    }
    return timeSource;
  }
}
