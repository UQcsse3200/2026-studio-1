package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Deals melee damage and knockback to a target entity when triggered, provided the target is within
 * {@code range} and this component is off cooldown. Does not use physics-engine collision detection
 * — attacks are triggered externally (e.g. by an {@link
 * com.csse3200.game.ai.tasks.AITaskComponent}-driven attack task) via an event, and range is
 * checked as an explicit distance calculation between entity positions.
 *
 * <p>This design was chosen over extending {@link TouchAttackComponent} because: (1)
 * hitbox/collider size varies significantly between enemy types based on physical body size (e.g. a
 * giant vs. a skeleton), and should not be assumed to correspond to melee attack reach; (2) {@link
 * TouchAttackComponent}'s fields and collision handler are {@code private}, making cooldown/range
 * logic impossible to add via inheritance without fully duplicating its internals anyway.
 *
 * <p>Requires {@link CombatStatsComponent} on this entity. Damage is only applied if the target
 * entity also has a {@link CombatStatsComponent}. Knockback is only applied if the target entity
 * has a {@link PhysicsComponent}.
 *
 * <p><b>Limitation:</b> this class does not determine when an attack should be attempted — it
 * relies entirely on being triggered externally with a target entity. If nothing ever triggers the
 * configured event, this component will never attack.
 */
public class MeleeAttackComponent extends Component {
  private float range;
  private float cooldown;
  private float knockback;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;

  /**
   * Creates a melee attack component with configurable range, cooldown, and knockback.
   *
   * @param range melee reach, checked as a direct distance calculation between this entity's and
   *     the target's positions
   * @param cooldown minimum time, in seconds, between successive attacks
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback
   *     <p><b>Limitation:</b> no validation is performed on any parameter — negative values are
   *     accepted as-is.
   */
  public MeleeAttackComponent(float range, float cooldown, float knockback) {
    // TODO: store the three parameters and initialize the cooldown timer so the
    //  entity can attack immediately.
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
  }

  /**
   * Resolves this entity's {@link CombatStatsComponent} and registers a listener for the
   * attack-trigger event.
   *
   * <p><b>Limitation:</b> the event name used here must exactly match whatever name the triggering
   * AI task uses elsewhere — there is no compile-time link between them; a mismatch fails silently
   * (the listener simply never fires).
   */
  @Override
  public void create() {
    // register combat stats
    combatStats = entity.getComponent(CombatStatsComponent.class);
    // add melee attack listener
    entity.getEvents().addListener("meleeAttack", this::attemptAttack);
  }

  /** Advances the internal cooldown timer by the time elapsed since the last frame. */
  @Override
  public void update() {
    timeSinceLastAttack += ServiceLocator.getTimeSource().getDeltaTime();
  }

  /**
   * Returns the configured melee range.
   *
   * @return melee range
   */
  public float getRange() {
    return this.range;
  }

  /**
   * Updates the melee range.
   *
   * @param range new range value
   * @throws IllegalArgumentException if {@code range} is negative
   */
  public void setRange(float range) {
    if (range < 0) {
      throw new IllegalArgumentException("range must not be negative");
    }
    this.range = range;
  }

  /**
   * Returns the configured cooldown duration.
   *
   * @return cooldown, in seconds
   */
  public float getCooldown() {
    return this.cooldown;
  }

  /**
   * Updates the cooldown duration.
   *
   * @param cooldown new cooldown value, in seconds
   * @throws IllegalArgumentException if {@code cooldown} is zero or negative
   */
  public void setCooldown(float cooldown) {
    if (cooldown <= 0) {
      throw new IllegalArgumentException("Cooldown duration must be greater than zero.");
    }
    this.cooldown = cooldown;
  }

  /**
   * Returns the configured knockback magnitude.
   *
   * @return knockback magnitude
   */
  public float getKnockback() {
    return this.knockback;
  }

  /**
   * Updates the knockback magnitude. A value of {@code 0f} is valid and intentionally disables
   * knockback (see {@link #attemptAttack(Entity)}).
   *
   * @param knockback new knockback magnitude
   * @throws IllegalArgumentException if {@code knockback} is negative
   */
  public void setKnockback(float knockback) {
    if (knockback < 0) {
      throw new IllegalArgumentException("Knockback must not be negative");
    }
    this.knockback = knockback;
  }

  /**
   * Attempts to attack the given target entity: validates cooldown and range, then applies damage
   * and knockback if both checks pass and the target has the required component(s).
   *
   * @param target the entity being attacked
   *     <p><b>Limitation:</b> behaviour when {@code target} is {@code null} must be explicitly
   *     decided — either guard against it here, or document that callers must never trigger the
   *     event with a null target.
   */
  private void attemptAttack(Entity target) {}
}
