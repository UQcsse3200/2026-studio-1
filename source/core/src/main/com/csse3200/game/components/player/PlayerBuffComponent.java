package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks temporary stat modifiers applied to an entity and reverts them once their duration has
 * passed.
 *
 * <p>Timing follows the same approach as WaitTask: an end time is worked out from {@link GameTime}
 * when the buff starts, and update() checks whether that time has been reached.
 *
 * <p><b>Stacking rule:</b> at most one buff is active per stat. Drinking a stronger potion replaces
 * the weaker buff, drinking the same potion again refreshes its timer, and drinking a weaker potion
 * while a stronger buff is active is rejected so the item is not wasted. Buffs on different stats
 * are independent and apply together. Multiplying two potions of the same stat together produced
 * speeds and damage the game was never balanced for, so that behaviour was deliberately removed.
 *
 * <p>Stats are handled by recalculating them as "unbuffed value multiplied by the active buff on
 * that stat". Applying and expiring a buff both use that one rule, so a buff never has to work out
 * how to undo itself.
 *
 * <p>Buff state is readable so a buff timer UI can be added later without changing this class.
 * Listen for the "buffApplied" and "buffExpired" events, or call {@link #getActiveBuffs()}.
 */
public class PlayerBuffComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerBuffComponent.class);

  private final Map<BuffStat, ActiveBuff> activeBuffs = new EnumMap<>(BuffStat.class);
  private GameTime timeSource;
  private int unbuffedBaseAttack;

  /** Stores the game clock and the entity's unbuffed stats. */
  @Override
  public void create() {
    timeSource = ServiceLocator.getTimeSource();
    rememberUnbuffedBaseAttack();
  }

  /**
   * Applies a temporary stat modifier that reverts after {@code durationSeconds}.
   *
   * <p>A buff that would change nothing is rejected, which covers a duration of zero and a
   * magnitude of exactly 1.0. A buff weaker than the one already running on that stat is also
   * rejected, so {@code ConsumableUseComponent} leaves the potion in the inventory.
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

    ActiveBuff current = activeBuffs.get(stat);
    if (isWeakerThan(stat, magnitude, current)) {
      logger.debug("Rejecting {} buff of {}, a stronger one is active", stat, magnitude);
      return false;
    }

    // Read the unbuffed value now rather than in create(), so a base attack changed elsewhere
    // (a level up, a different weapon) is not overwritten when this buff expires.
    if (stat == BuffStat.DAMAGE && current == null) {
      rememberUnbuffedBaseAttack();
    }

    long endTime = timeSource.getTime() + (long) (durationSeconds * 1000);
    ActiveBuff buff = new ActiveBuff(stat, magnitude, durationSeconds, endTime);
    activeBuffs.put(stat, buff);
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
    for (ActiveBuff buff : activeBuffs.values()) {
      if (currentTime >= buff.getEndTime()) {
        expired.add(buff);
      }
    }

    if (expired.isEmpty()) {
      return;
    }

    for (ActiveBuff buff : expired) {
      activeBuffs.remove(buff.getStat());
    }
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
   * @return unmodifiable snapshot of the active buffs, at most one per stat
   */
  public List<ActiveBuff> getActiveBuffs() {
    return List.copyOf(activeBuffs.values());
  }

  /**
   * Returns whether a buff on the given stat is currently active.
   *
   * @param stat stat to check
   * @return {@code true} if an active buff modifies {@code stat}
   */
  public boolean hasBuff(BuffStat stat) {
    return activeBuffs.containsKey(stat);
  }

  /**
   * Returns the movement speed multiplier from the active speed buff.
   *
   * <p>Movement speed belongs to PlayerActions, which this component does not modify. A movement
   * component should multiply its target speed by this value for speed buffs to take effect in
   * game.
   *
   * @return speed multiplier, where 1.0 is unbuffed
   */
  public float getSpeedMultiplier() {
    return multiplierFor(BuffStat.SPEED);
  }

  /**
   * Returns the damage multiplier from the active damage buff.
   *
   * <p>This is the value the weapon damage pipeline should multiply {@code weapon.getDamage()} by.
   * Reading it here keeps the Strength potion working once melee damage comes from the weapon
   * rather than from {@code CombatStatsComponent.baseAttack}.
   *
   * @return damage multiplier, where 1.0 is unbuffed
   */
  public float getDamageMultiplier() {
    return multiplierFor(BuffStat.DAMAGE);
  }

  /**
   * Returns the multiplier applied to incoming damage.
   *
   * <p>This is where Resistance meets the damage pipeline: whatever applies damage to the player
   * should multiply it by this value. Karan's shield cancels damage at the same point, so a blocked
   * hit stays blocked and an unblocked hit is still reduced.
   *
   * @return incoming damage multiplier, where 1.0 is unreduced
   */
  public float getIncomingDamageMultiplier() {
    return multiplierFor(BuffStat.RESISTANCE);
  }

  /**
   * Returns whether a candidate buff is weaker than the one already running on that stat.
   *
   * <p>A bigger multiplier is stronger for an increase such as damage, and a smaller one is
   * stronger for a reduction such as resistance, so the comparison flips per stat.
   *
   * @param stat stat being buffed
   * @param magnitude magnitude of the candidate buff
   * @param current the active buff on that stat, or {@code null} when there is none
   * @return {@code true} if the candidate should be rejected
   */
  private boolean isWeakerThan(BuffStat stat, float magnitude, ActiveBuff current) {
    if (current == null) {
      return false;
    }
    return stat.isStrongerWhenLower()
        ? magnitude > current.getMagnitude()
        : magnitude < current.getMagnitude();
  }

  /**
   * Returns the active multiplier for a stat.
   *
   * @param stat stat to look up
   * @return the active buff's magnitude, or 1.0 when that stat is unbuffed
   */
  private float multiplierFor(BuffStat stat) {
    ActiveBuff buff = activeBuffs.get(stat);
    return buff == null ? 1f : buff.getMagnitude();
  }

  /**
   * Sets every buffed stat back to its unbuffed value multiplied by the active buff on that stat.
   *
   * <p>Called whenever a buff starts or expires, so both cases share the same logic.
   *
   * <p>The write to {@code CombatStatsComponent} is a temporary bridge: melee damage still comes
   * from {@code baseAttack} today, so without it the Strength potion would do nothing in the
   * current build. Once weapon damage is routed through {@code weapon.getDamage()}, that pipeline
   * should read {@link #getDamageMultiplier()} and this write can be deleted.
   */
  private void recalculateStats() {
    CombatStatsComponent stats = getCombatStats();
    if (stats != null) {
      stats.setBaseAttack(Math.round(unbuffedBaseAttack * getDamageMultiplier()));
    }
  }

  /** Records the entity's current base attack as the value to return to when buffs expire. */
  private void rememberUnbuffedBaseAttack() {
    CombatStatsComponent stats = getCombatStats();
    if (stats != null) {
      unbuffedBaseAttack = stats.getBaseAttack();
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
