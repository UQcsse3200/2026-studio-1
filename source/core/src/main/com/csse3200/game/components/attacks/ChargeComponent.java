package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a temporary charge state. A charge has two phases: in the windup the entity stands
 * still, and in the rush it moves fast toward a snapshot of the target's position with its {@link
 * TouchAttackComponent} live. Touching the target during the rush deals the entity's base attack
 * times the damage multiplier, once per rush, and (by default) ends the rush so the entity does not
 * run through the player. The rush is the attack, so there is exactly one damage event per charge
 * and no double-dipping with a separate attack.
 *
 * <p>Attack-type-agnostic: Minotaur and Centaur both use it. If the entity has no {@link
 * TouchAttackComponent} the charge still moves the entity but deals no damage.
 *
 * <p>Fires {@code "chargeStart"}, {@code "chargeWindupStart"}, {@code "chargeRushStart"} and {@code
 * "chargeEnd"} on the entity.
 */
public class ChargeComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ChargeComponent.class);

  /** This entity's touch attack, found in create(); may be null. */
  private TouchAttackComponent touchAttack;

  /** True (the default) to end the rush as soon as it lands. */
  private boolean endOnHit = true;

  /** Set when a charge ends, cleared at the start of each update(); stops a double end. */
  private boolean finishedThisTick = false;

  private final float chargeDuration;
  private final float windupDuration;
  private final float cooldown;
  private final float damageMultiplier;
  private final float speedMultiplier;

  private Vector2 targetPosition;
  private float timeSinceLastCharge;
  private float chargeTimeRemaining;
  private float windupTimeRemaining;

  /**
   * Create a charge component without a wind-up phase — moves fast immediately.
   *
   * @param chargeDuration seconds a single charge lasts once started
   * @param cooldown minimum seconds between the end of one charge and the start of the next
   * @param damageMultiplier damage multiplier applied to the next attack while charging; must be
   *     greater than 1.0
   * @param speedMultiplier movement speed multiplier applied while charging; must be greater than
   *     1.0
   * @throws IllegalArgumentException if constraints are violated
   */
  public ChargeComponent(
      float chargeDuration, float cooldown, float damageMultiplier, float speedMultiplier)
      throws IllegalArgumentException {
    this(chargeDuration, 0f, cooldown, damageMultiplier, speedMultiplier);
  }

  /**
   * Create a charge component with an optional stationary wind-up phase before the fast rush.
   *
   * @param chargeDuration total seconds a charge lasts (including windupDuration)
   * @param windupDuration seconds the entity remains stationary winding up before moving fast
   * @param cooldown minimum seconds between the end of one charge and the start of the next
   * @param damageMultiplier damage multiplier applied to the next attack while charging; must be
   *     greater than 1.0
   * @param speedMultiplier movement speed multiplier applied while charging; must be greater than
   *     1.0
   * @throws IllegalArgumentException if constraints are violated
   */
  public ChargeComponent(
      float chargeDuration,
      float windupDuration,
      float cooldown,
      float damageMultiplier,
      float speedMultiplier)
      throws IllegalArgumentException {
    if (chargeDuration <= 0) {
      throw new IllegalArgumentException("chargeDuration must be positive.");
    }
    if (windupDuration < 0 || windupDuration >= chargeDuration) {
      throw new IllegalArgumentException(
          "windupDuration must be non-negative and strictly less than chargeDuration");
    }
    if (cooldown < 0) {
      throw new IllegalArgumentException("cooldown must not be negative.");
    }
    if (damageMultiplier <= 1.0) {
      throw new IllegalArgumentException("damageMultiplier must be greater than 1.0");
    }
    if (speedMultiplier <= 1.0) {
      throw new IllegalArgumentException(
          "to have any effect on speed, multiplier must be greater than 1.");
    }
    this.chargeDuration = chargeDuration;
    this.windupDuration = windupDuration;
    this.cooldown = cooldown;

    this.damageMultiplier = damageMultiplier;
    this.speedMultiplier = speedMultiplier;

    this.timeSinceLastCharge = cooldown;
    this.chargeTimeRemaining = 0;
    this.windupTimeRemaining = 0;
  }

  /**
   * Finds this entity's touch attack, switches it off, limits it to one hit per rush and listens
   * for it landing. With no touch attack the charge still moves the entity but deals no damage.
   */
  @Override
  public void create() {
    touchAttack = entity.getComponent(TouchAttackComponent.class);
    if (touchAttack == null) {
      logger.warn(
          "ChargeComponent on {} has no TouchAttackComponent: the charge deals no damage", entity);
      return;
    }
    touchAttack.setActive(false);
    touchAttack.setMaxHitsPerActivation(1);
    entity.getEvents().addListener("touchAttackHit", this::onTouchHit);
  }

  /** Switches the touch attack off when this component is disposed. */
  @Override
  public void dispose() {
    if (touchAttack != null) {
      touchAttack.setActive(false);
    }
  }

  /**
   * Sets whether a landed hit ends the rush.
   *
   * @param endOnHit true (the default) to end the rush when the touch attack lands; false to let it
   *     run for its full duration
   */
  public void setEndOnHit(boolean endOnHit) {
    this.endOnHit = endOnHit;
  }

  /**
   * Returns whether a landed hit ends the rush.
   *
   * @return true if the rush ends when the touch attack lands
   */
  public boolean isEndOnHit() {
    return endOnHit;
  }

  /**
   * Ends the current charge now, exactly as if its duration had run out. Does nothing if no charge
   * is in progress.
   */
  public void endCharge() {
    if (!isCharging()) {
      return;
    }
    finishCharge();
  }

  /** Makes the touch attack live for the rush, scaled by this charge's damage multiplier. */
  private void beginTouchDamage() {
    if (touchAttack == null) {
      return;
    }
    touchAttack.setDamageMultiplier(damageMultiplier);
    touchAttack.activate();
  }

  /**
   * The touch attack landed. Ends the rush if configured to, and only during the rush itself (a hit
   * during the stationary windup must not end it).
   *
   * @param target the entity that was hit
   */
  private void onTouchHit(Entity target) {
    if (endOnHit && isRushing()) {
      finishCharge();
    }
  }

  /** The one place a charge ends, whether it ran out, was ended early, or landed. */
  private void finishCharge() {
    if (touchAttack != null) {
      touchAttack.deactivate();
      touchAttack.setDamageMultiplier(1.0f);
    }
    chargeTimeRemaining = 0;
    windupTimeRemaining = 0;
    timeSinceLastCharge = 0;
    PhysicsMovementComponent movement =
        this.getEntity().getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setSpeedMultiplier(1.0f);
      movement.setMoving(false);
    }
    finishedThisTick = true;
    this.getEntity().getEvents().trigger("chargeEnd");
  }

  /**
   * Advances the charge/cooldown timers by one tick, transitioning from stationary windup to fast
   * movement and restoring normal movement speed when a charge ends.
   */
  @Override
  public void update() {
    finishedThisTick = false;
    float dt = ServiceLocator.getTimeSource().getDeltaTime();
    if (isCharging()) {
      chargeTimeRemaining -= dt;
      if (windupTimeRemaining > 0) {
        windupTimeRemaining -= dt;
        if (windupTimeRemaining <= 0.0001f) {
          windupTimeRemaining = 0;
          PhysicsMovementComponent movement =
              this.getEntity().getComponent(PhysicsMovementComponent.class);
          if (movement != null && targetPosition != null) {
            movement.setTarget(targetPosition);
            movement.setSpeedMultiplier(this.speedMultiplier);
            movement.setMoving(true);
          }
          this.getEntity().getEvents().trigger("chargeRushStart");
          beginTouchDamage();
        }
      }
      if (finishedThisTick) {
        // A hit that landed on the first frame of the rush already ended the charge.
        return;
      }
      if (chargeTimeRemaining <= 0.0001f) {
        finishCharge();
      }
    } else {
      timeSinceLastCharge += dt;
    }
  }

  /**
   * Checks whether a new charge may begin right now.
   *
   * @return true if not currently charging and the cooldown has fully elapsed
   */
  public boolean canCharge() {
    return (!isCharging()) && (timeSinceLastCharge >= cooldown);
  }

  /**
   * @return true if a charge is currently in progress (either in windup or rush phase)
   */
  public boolean isCharging() {
    return chargeTimeRemaining > 0;
  }

  /**
   * @return true if the entity is currently in the stationary windup phase of the charge
   */
  public boolean isWindup() {
    return isCharging() && windupTimeRemaining > 0;
  }

  /**
   * @return true if the entity is currently in the fast movement rush phase of the charge
   */
  public boolean isRushing() {
    return isCharging() && windupTimeRemaining <= 0;
  }

  /**
   * @return the snapshot target position being charged toward, or null if not charging
   */
  public Vector2 getTargetPosition() {
    return targetPosition != null ? targetPosition.cpy() : null;
  }

  public float getWindupDuration() {
    return windupDuration;
  }

  public float getChargeDuration() {
    return chargeDuration;
  }

  /**
   * Begins a charge toward the given target position if {@link #canCharge()} is true; otherwise a
   * no-op. If windupDuration > 0, the entity remains stationary during the windup phase before
   * moving fast toward the target position.
   *
   * @param targetPosition the world position to charge toward
   */
  public void startCharge(Vector2 targetPosition) {
    if (!canCharge()) {
      return;
    }
    this.targetPosition = targetPosition.cpy();
    chargeTimeRemaining = chargeDuration;
    windupTimeRemaining = windupDuration;
    PhysicsMovementComponent movement =
        this.getEntity().getComponent(PhysicsMovementComponent.class);

    if (windupDuration > 0) {
      if (movement != null) {
        movement.setMoving(false);
      }
      this.getEntity().getEvents().trigger("chargeWindupStart");
    } else {
      if (movement != null) {
        movement.setTarget(targetPosition);
        movement.setSpeedMultiplier(this.speedMultiplier);
        movement.setMoving(true);
      }
      this.getEntity().getEvents().trigger("chargeRushStart");
    }
    this.getEntity().getEvents().trigger("chargeStart");
    if (windupDuration <= 0) {
      // After the start events, so a hit on the very first frame ends the charge after them.
      beginTouchDamage();
    }
  }

  /**
   * Returns the damage multiplier to apply to the next attack. Safe to call unconditionally —
   * returns 1.0f (no change) whenever not currently charging, so callers never need to check {@link
   * #isCharging()} first.
   *
   * @return damageMultiplier while charging, otherwise 1.0f. No damage is dealt by this method.
   */
  public float getDamageMultiplier() {
    if (isCharging()) {
      return this.damageMultiplier;
    }
    return 1.0f;
  }
}
