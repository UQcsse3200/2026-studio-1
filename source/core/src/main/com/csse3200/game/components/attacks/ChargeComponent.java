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
  private final float cooldown;
  private final float damageMultiplier;
  private final float speedMultiplier;

  private float timeSinceLastCharge;
  private float chargeTimeRemaining;

  /**
   * Create a charge component - increases the speed and attack damage from the entity.
   *
   * @param chargeDuration seconds a single charge lasts once started
   * @param cooldown minimum seconds between the end of one charge and the start of the next
   * @param damageMultiplier damage multiplier applied to the next attack while charging; must be
   *     greater than 1.0
   * @param speedMultiplier movement speed multiplier applied while charging; must be greater than
   *     1.0
   * @throws IllegalArgumentException if damageMultiplier or speedMultiplier is not greater than
   *     1.0, if chargeDuration is not positive, or if cooldown is negative
   */
  public ChargeComponent(
      float chargeDuration, float cooldown, float damageMultiplier, float speedMultiplier)
      throws IllegalArgumentException {
    if (chargeDuration <= 0) {
      throw new IllegalArgumentException("chargeDuration must be positive.");
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
    this.cooldown = cooldown;

    this.damageMultiplier = damageMultiplier;
    this.speedMultiplier = speedMultiplier;

    this.timeSinceLastCharge = cooldown;
    this.chargeTimeRemaining = 0;
  }

  /**
   * Advances the charge/cooldown timers by one tick, restoring normal movement speed when a charge
   * ends.
   */
  @Override
  public void update() {
    if (isCharging()) {
      chargeTimeRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      if (chargeTimeRemaining <= 0) {
        chargeTimeRemaining = 0;
        timeSinceLastCharge = 0;
        this.getEntity().getComponent(PhysicsMovementComponent.class).setSpeedMultiplier(1.0f);
      } else {
        timeSinceLastCharge += ServiceLocator.getTimeSource().getDeltaTime();
      }
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
   * @return true if a charge is currently in progress
   */
  public boolean isCharging() {
    return chargeTimeRemaining > 0;
  }

  /**
   * Begins a charge toward the given target position if {@link #canCharge()} is true; otherwise a
   * no-op. A one-time snapshot of the target's position, not tracked continuously — the entity
   * keeps moving toward this point even if the target later moves.
   *
   * @param targetPosition the world position to charge toward
   */
  public void startCharge(Vector2 targetPosition) {
    if (!canCharge()) {
      return;
    }
    chargeTimeRemaining = chargeDuration;
    this.getEntity().getComponent(PhysicsMovementComponent.class).setTarget(targetPosition);
    this.getEntity()
        .getComponent(PhysicsMovementComponent.class)
        .setSpeedMultiplier(this.speedMultiplier);
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
