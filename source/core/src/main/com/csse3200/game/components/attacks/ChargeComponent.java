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

  private float timeSinceLastCharge;
  private float chargeTimeRemaining;

  /**
   * Creates a charge component.
   *
   * @param chargeDuration seconds a single charge lasts once started
   * @param cooldown minimum seconds between the end of one charge and the start of the next
   * @param damageMultiplier damage multiplier applied to the next attack while charging; must be
   *     greater than 1.0
   * @throws IllegalArgumentException if speedMultiplier or damageMultiplier is not greater than
   *     1.0, if chargeDuration is not positive, or if cooldown is negative
   */
  public ChargeComponent(float chargeDuration, float cooldown, float damageMultiplier) {
    if (chargeDuration <= 0) {
      throw new IllegalArgumentException("chargeDuration must be positive.");
    }
    if (cooldown < 0) {
      throw new IllegalArgumentException("cooldown must not be negative.");
    }
    if (damageMultiplier <= 1.0) {
      throw new IllegalArgumentException("damageMultiplier must be greater than 1.0");
    }
    this.chargeDuration = chargeDuration;
    this.cooldown = cooldown;
    this.damageMultiplier = damageMultiplier;
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
   * Begins a charge if {@link #canCharge()} is true; otherwise a no-op. Starts the charge duration
   * countdown only — no movement effect. Fires {@code "chargeStart"} on owning entity, an animation
   * controller can listen for this to switch to a charging sprite, giving the player visual
   * feedback even though there's no actual movement change.
   *
   * @param direction direction the charge is aimed toward - represents the target being charged at
   */
  public void startCharge(Vector2 direction) {
    if (!canCharge()) {
      return;
    }
    chargeTimeRemaining = chargeDuration;
    entity.getComponent(PhysicsMovementComponent.class).setTarget(direction);
    entity.getEvents().trigger("chargeStart");
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
