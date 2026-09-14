package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Deals ranged damage and knockback to a target entity when triggered, provided the target is
 * within {@code range} and this component is off cooldown. Does not use physics-engine collision
 * detection — attacks are triggered externally (e.g. by a {@link
 * com.csse3200.game.components.tasks.RangedAttackTask}) via an event, and range is checked as an
 * explicit distance calculation between entity positions.
 *
 * <p>This is mechanically identical to {@link MeleeAttackComponent} — the only difference is the
 * event name it listens for, and typically a much larger configured range. It's kept as its own,
 * independent component rather than a subclass of {@link MeleeAttackComponent} since that class's
 * fields and attack-resolution method are {@code private}, so reuse via inheritance would need
 * those to change anyway — see {@link MeleeAttackComponent}'s own javadoc, which gives the same
 * reasoning for not extending {@link TouchAttackComponent}.
 *
 * <p>Requires {@link CombatStatsComponent} on this entity. Damage is only applied if the target
 * entity also has a {@link CombatStatsComponent}. Knockback is only applied if the target entity
 * has a {@link PhysicsComponent}.
 *
 * <p><b>Limitation:</b> this class does not determine when an attack should be attempted — it
 * relies entirely on being triggered externally with a target entity (see {@link
 * com.csse3200.game.components.tasks.RangedAttackTask}).
 */
public class RangedAttackComponent extends Component {
  private float range;
  private float cooldown;
  private float knockback;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;

  /**
   * Creates a ranged attack component with configurable range, cooldown, and knockback.
   *
   * @param range attack reach, checked as a direct distance calculation between this entity's and
   *     the target's positions
   * @param cooldown minimum time, in seconds, between successive attacks
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback
   */
  public RangedAttackComponent(float range, float cooldown, float knockback) {
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    this.timeSinceLastAttack = cooldown;
  }

  /**
   * Resolves this entity's {@link CombatStatsComponent} and registers a listener for the
   * attack-trigger event.
   */
  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("rangedAttack", this::attemptAttack);
  }

  /** Advances the internal cooldown timer by the time elapsed since the last frame. */
  @Override
  public void update() {
    timeSinceLastAttack += ServiceLocator.getTimeSource().getDeltaTime();
  }

  /**
   * Returns the configured attack range.
   *
   * @return attack range
   */
  public float getRange() {
    return this.range;
  }

  /**
   * Updates the attack range.
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
   */
  private void attemptAttack(Entity target) {
    if (target == null) {
      return;
    }

    // cooldown check
    if (this.timeSinceLastAttack < this.getCooldown()) {
      return;
    }

    // range check
    float distance = entity.getPosition().dst(target.getPosition());
    if (distance > this.getRange()) {
      return;
    }

    // handle whether target has a combat stats component
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null) {
      return;
    }

    // apply damage
    targetStats.hit(combatStats);

    // announce a successful hit - useful for triggering special effects
    entity.getEvents().trigger("rangedAttackHit", target);
    // reset cooldown, since an attack just succeeded
    this.timeSinceLastAttack = 0;

    // check whether knockback = 0 --> knockback is disabled
    PhysicsComponent targetPhysics = target.getComponent(PhysicsComponent.class);
    if (targetPhysics != null && this.getKnockback() > 0) {
      Body targetBody = targetPhysics.getBody();
      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
      Vector2 impulse = direction.setLength(this.getKnockback());
      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }
  }
}
