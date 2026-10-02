package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Represents a temporary charge state: a burst of movement speed toward a target, and a short-lived
 * damage multiplier applied to whichever attack (melee or ranged) resolves next — NOT a separate
 * damage-dealing hit of its own. Attack-type-agnostic: Minotaur pairs this with
 * MeleeAttackComponent, Centaur pairs it with RangedAttackComponent; neither needs to know charging
 * exists beyond checking for this component and reading getDamageMultiplier(). A separate
 * damage-on-collision design was considered and rejected: it risks double-dipping if the entity's
 * normal attack also fires around the same time as the collision. A pure multiplier on the next
 * real attack avoids that — exactly one damage-dealing event per charge.
 */
public class ChargeComponent extends Component {
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
   * Advances the charge/cooldown timers by one tick, transitioning from stationary windup to fast
   * movement and restoring normal movement speed when a charge ends.
   */
  @Override
  public void update() {
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
        }
      }

      if (chargeTimeRemaining <= 0.0001f) {
        chargeTimeRemaining = 0;
        windupTimeRemaining = 0;
        timeSinceLastCharge = 0;
        PhysicsMovementComponent movement =
            this.getEntity().getComponent(PhysicsMovementComponent.class);
        if (movement != null) {
          movement.setSpeedMultiplier(1.0f);
          movement.setMoving(false);
        }
        this.getEntity().getEvents().trigger("chargeEnd");
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
