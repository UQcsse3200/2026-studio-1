package com.csse3200.game.components.enemies.attacks;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.enemies.attacks.PetrifyConfig;

/**
 * Petrifies (fully immobilises) whatever an attack hits, for a configured time, by triggering the
 * target's own {@code "applySpeedEffect"} event with a {@code 0f} speed multiplier. That is the
 * same mechanism {@link com.csse3200.game.components.effects.SpeedEffectComponent} uses to revert
 * itself once the duration elapses. All petrify logic lives here: the factory only creates the
 * component and adds it.
 *
 * <p>It can listen in two places, chosen by how it is built:
 *
 * <ul>
 *   <li><b>On the shooter</b> ({@link #forShooter}): listens for the shooter's {@code
 *       "rangedAttackHit"} event, which {@code RangedAttackComponent} fires when its projectile
 *       lands. This is how Medusa uses it, with the ordinary arrow. It reads the config in seconds,
 *       converts to ticks, and enforces the team rule that the attack's cooldown is at least {@link
 *       #COOLDOWN_EFFECT_MULTIPLIER} times the petrify duration.
 *   <li><b>On a projectile</b> ({@link #PetrifyEffectComponent(int)}): listens for that
 *       projectile's own {@code "projectileHit"} event, mirroring {@link LightningFreezeComponent}.
 *       Nothing uses this yet; it is kept for a future gaze projectile.
 * </ul>
 *
 * <p>A target without a {@code SpeedEffectComponent} is unaffected, because nothing listens for the
 * event on it.
 */
public class PetrifyEffectComponent extends Component {
  /** The event a projectile fires on its own entity when it hits something. */
  public static final String PROJECTILE_HIT_EVENT = "projectileHit";

  /** The event {@code RangedAttackComponent} fires on the shooter when its projectile lands. */
  public static final String RANGED_ATTACK_HIT_EVENT = "rangedAttackHit";

  /** The event the target's {@code SpeedEffectComponent} listens for. */
  public static final String APPLY_SPEED_EFFECT_EVENT = "applySpeedEffect";

  /** Speed multiplier while petrified: none. */
  public static final float PETRIFY_SPEED_MULTIPLIER = 0f;

  /**
   * The speed effect counts down once per frame, so seconds are converted at 60 frames a second.
   */
  public static final int TICKS_PER_SECOND = 60;

  /**
   * Team rule: the attack that petrifies must wait at least this many times the petrify duration.
   */
  public static final float COOLDOWN_EFFECT_MULTIPLIER = 2f;

  private final int petrifyTicks;
  private final String hitEvent;

  /**
   * Builds a petrify that listens on a projectile for that projectile's own {@code "projectileHit"}
   * event.
   *
   * @param petrifyTicks number of ticks the hit target is petrified for; {@code 0} applies the
   *     effect instantly with no duration (matches {@code SpeedEffectComponent}'s "instant,
   *     permanent" convention for a duration of zero).
   * @throws IllegalArgumentException if {@code petrifyTicks} is negative
   */
  public PetrifyEffectComponent(int petrifyTicks) {
    this(petrifyTicks, PROJECTILE_HIT_EVENT);
  }

  /**
   * @param petrifyTicks ticks to petrify for; not negative
   * @param hitEvent the event on this component's own entity that carries the entity hit
   * @throws IllegalArgumentException if petrifyTicks is negative
   */
  private PetrifyEffectComponent(int petrifyTicks, String hitEvent) {
    if (petrifyTicks < 0) {
      throw new IllegalArgumentException("petrifyTicks must not be negative.");
    }
    this.petrifyTicks = petrifyTicks;
    this.hitEvent = hitEvent;
  }

  /**
   * Builds the petrify an enemy carries so its ranged attack petrifies what it hits. Checks the
   * config and the design rule first, so a bad value fails when the enemy is built, not mid-game.
   *
   * @param petrify the config; its duration is in seconds and must be above zero
   * @param attackCooldown the cooldown in seconds of the attack that petrifies
   * @return a petrify that listens for the enemy's {@code RANGED_ATTACK_HIT_EVENT}
   * @throws IllegalArgumentException if petrify is null, the duration is not above zero or does not
   *     round to at least one tick, or the cooldown is below {@code COOLDOWN_EFFECT_MULTIPLIER}
   *     times the duration
   */
  public static PetrifyEffectComponent forShooter(PetrifyConfig petrify, float attackCooldown) {
    if (petrify == null) {
      throw new IllegalArgumentException("petrify config is required");
    }
    // NaN is checked explicitly because it fails every comparison; zero ticks would mean
    // "permanent".
    if (Float.isNaN(petrify.duration)
        || petrify.duration <= 0
        || Float.isInfinite(petrify.duration)) {
      throw new IllegalArgumentException("petrify duration must be above zero");
    }
    requireCooldownCoversEffect(attackCooldown, petrify.duration);
    int ticks = toTicks(petrify.duration);
    if (ticks < 1) {
      throw new IllegalArgumentException("petrify duration is shorter than one tick");
    }
    return new PetrifyEffectComponent(ticks, RANGED_ATTACK_HIT_EVENT);
  }

  /**
   * Converts seconds to frames.
   *
   * @param seconds a duration in seconds
   * @return the duration in ticks, rounded to the nearest whole tick
   */
  public static int toTicks(float seconds) {
    return Math.round(seconds * TICKS_PER_SECOND);
  }

  /**
   * Team design rule: an attack that applies a timed effect must have a cooldown of at least {@code
   * COOLDOWN_EFFECT_MULTIPLIER} times the effect's duration, so the player always gets a window to
   * act. A NaN cooldown or effect duration is rejected too.
   *
   * @param cooldown the attack's cooldown in seconds
   * @param effectSeconds the effect's duration in seconds
   * @throws IllegalArgumentException if the cooldown is below the minimum; exactly the minimum is
   *     allowed
   */
  static void requireCooldownCoversEffect(float cooldown, float effectSeconds) {
    float minimum = COOLDOWN_EFFECT_MULTIPLIER * effectSeconds;
    if (Float.isNaN(cooldown) || Float.isNaN(minimum) || cooldown < minimum) {
      throw new IllegalArgumentException("cooldown " + cooldown + " must be at least " + minimum);
    }
  }

  /** Subscribes to the configured hit event on this component's own entity. */
  @Override
  public void create() {
    entity.getEvents().addListener(hitEvent, this::onHit);
  }

  /**
   * Petrifies {@code target} by triggering its {@code APPLY_SPEED_EFFECT_EVENT}, after telling it
   * the cause ({@link FrozenCause#PETRIFIED}) so the screen can show it; a no-op if the target is
   * null or has no {@code SpeedEffectComponent} listening for it.
   *
   * @param target the entity that was hit
   */
  private void onHit(Entity target) {
    if (target == null) {
      return;
    }
    target.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    target.getEvents().trigger(APPLY_SPEED_EFFECT_EVENT, petrifyTicks, PETRIFY_SPEED_MULTIPLIER);
  }
}
