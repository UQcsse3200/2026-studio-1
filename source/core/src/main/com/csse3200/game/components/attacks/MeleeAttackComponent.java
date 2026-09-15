package com.csse3200.game.components.attacks;

import static java.awt.geom.Point2D.distance;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private WeaponItem weapon;
  /* This is set in the {@link WeaponItem} creation rather than here as animation is per weapon
   * Adjustments can be made as public setter and getter for the value is avaliable.
   */
  private float windupDuration;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;
  private Entity pendingTarget;
  private float windupTimeRemaining;
  private static final Logger logger = LoggerFactory.getLogger(MeleeAttackComponent.class);

  /**
   * @param range melee reach — this wielder's own property, not the weapon's
   * @param cooldown minimum time, in seconds, between attacks
   * @param knockback knockback magnitude on a successful hit — this wielder's own property
   * @param weapon supplies damage only
   * @throws IllegalArgumentException if weapon is null, range/cooldown are non-positive, knockback
   *     is negative, or windupDuration is negative or &gt;= cooldown
   */
  public MeleeAttackComponent(float range, float cooldown, float knockback, WeaponItem weapon)
      throws IllegalArgumentException {
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    if (weapon == null) {
      throw new IllegalArgumentException("weapon cannot be null");
    }
    this.weapon = weapon;
    if (weapon.getWeaponType() == WeaponType.BOW) {
      throw new IllegalArgumentException("Melee Attack cannot use a Bow Weapon.");
    }
    if (weapon.getWindupDuration() < 0) {
      throw new IllegalArgumentException("windupDuration must not be negative.");
    }
    if (weapon.getWindupDuration() >= getCooldown()) {
      throw new IllegalArgumentException("windupDuration must be less than cooldown.");
    }
    this.windupDuration = weapon.getWindupDuration();
    this.timeSinceLastAttack = cooldown;
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
    if (pendingTarget != null) {
      windupTimeRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      if (windupTimeRemaining <= 0) {
        resolveAttack();
      }
    }
  }

  /**
   * Returns the configured melee range.
   *
   * @return melee range, in world units
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
  public void setRange(float range) throws IllegalArgumentException {
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
  public void setCooldown(float cooldown) throws IllegalArgumentException {
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
  public void setKnockback(float knockback) throws IllegalArgumentException {
    if (knockback < 0) {
      throw new IllegalArgumentException("Knockback must not be negative");
    }
    this.knockback = knockback;
  }

  /**
   * Returns the equipped weapon's damage. The only stat this component reads from the weapon —
   * range and knockback are this wielder's own properties, not the weapon's.
   *
   * @return the equipped weapon's damage, sourced from {@code weapon.getDamage()}
   * @throws NullPointerException if no weapon has been set
   */
  public float getDamage() throws NullPointerException {
    if (this.weapon == null) {
      throw new NullPointerException("weapon cannot be null");
    }
    return this.weapon.getDamage();
  }

  /**
   * Checks whether enough time has elapsed since the last attack for a new one to be attempted.
   *
   * @return true if the cooldown has fully elapsed
   */
  public boolean canAttack() {
    return timeSinceLastAttack >= this.getCooldown();
  }

  /**
   * Attempts to attack the given target entity: validates cooldown and range, then applies damage
   * and knockback if both checks pass and the target has the required component(s).
   *
   * @param target the entity being attacked
   *     <p>Expected effect: may reduce target's health and/or apply an impulse to target's physics
   *     body.
   *     <p><b>Limitation:</b> behaviour when {@code target} is {@code null} must be explicitly
   *     decided — either guard against it here, or document that callers must never trigger the
   *     event with a null target.
   */
  private void attemptAttack(Entity target) {
    // guarding against a malformed trigger i.e. considering when target is null
    if (target == null) {
      return;
    }
    // cooldown check
    if (this.timeSinceLastAttack < this.getCooldown()) {
      return;
    }
    // range check
    float distance =
        (float)
            distance(
                entity.getPosition().x,
                entity.getPosition().y,
                target.getPosition().x,
                target.getPosition().y);

    if (distance > this.getRange()) {
      return;
    }
    // handle whether target has a combat stats component
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null) {
      return;
    }
    this.pendingTarget = target;
    this.windupTimeRemaining = this.windupDuration;
    timeSinceLastAttack = 0;
    entity.getEvents().trigger("meleeAttackWindup", this.pendingTarget);
  }

  /**
   * Called once the windup timer elapses. Re-validates the pending target is still alive and in
   * range (it may have died or moved away during the windup), and if so, applies weapon damage
   * (multiplied by any active {@link ChargeComponent} bonus), fires {@code "meleeAttackHit"}, and
   * applies knockback. A no-op (a "whiff") if the target is no longer valid.
   */
  private void resolveAttack() {
    Entity target = this.pendingTarget;
    this.pendingTarget = null;
    this.windupTimeRemaining = 0;
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null || targetStats.getHealth() <= 0) {
      return;
    }
    float distance =
        (float)
            distance(
                entity.getPosition().x,
                entity.getPosition().y,
                target.getPosition().x,
                target.getPosition().y);
    if (distance > this.getRange()) {
      return;
    }
    int finalDamage = weapon.getDamage();
    // retrieve damage stats from weapon
    ChargeComponent chargeComponent = entity.getComponent(ChargeComponent.class);
    if (chargeComponent != null) {
      // if not charging then 1.0f is the mutiplier
      finalDamage = (int) (finalDamage * chargeComponent.getDamageMultiplier());
    }
    combatStats.setBaseAttack(finalDamage);
    targetStats.hit(combatStats);
    // announce a successful hit - useful for triggering special effects
    entity.getEvents().trigger("meleeAttackHit", target);
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
