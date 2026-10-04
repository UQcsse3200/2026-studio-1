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
  private float damage;
  private WeaponItem weapon;
  /* This is set in the {@link WeaponItem} creation rather than here as animation is per weapon
   * Adjustments can be made as public setter and getter for the value is avaliable.
   */
  private float windupDuration;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;
  private Entity pendingTarget;
  private float windupTimeRemaining;

  /** Difficulty's enemy damage multiplier, applied on top of weapon (and charge) damage. */
  private float damageMultiplier = 1f;

  private float windupMultiplier = 1f;

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

    if (weapon.getWindupDuration() >= this.getCooldown()) {
      throw new IllegalArgumentException("windupDuration must be less than cooldown");
    }

    this.windupDuration = weapon.getWindupDuration();
    this.timeSinceLastAttack = cooldown;
  }

  /**
   * Creates a melee attack component with configurable range, cooldown, knockback, with no weapon
   * for any attacks i.e. the rocks where no weapons are in the inventory.
   *
   * @param range attack reach, checked as a direct distance calculation between this entity's and
   *     the target's positions; also used as the fired arrow's maximum flight distance.
   * @param cooldown minimum time, in seconds, between successive shots being fired.
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback.
   */
  public MeleeAttackComponent(float range, float cooldown, float knockback) {
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    if (cooldown <= 1) {
      throw new IllegalArgumentException(
          "WindupDuration for attack must be less than cooldown and must be positive.");
    }
    this.windupDuration = this.getCooldown() - 1;
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
   * 3 3 Updates the melee range.
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
   * Returns the difficulty damage multiplier applied to this melee attacker's damage.
   *
   * @return damage multiplier; {@code 1f} by default (no change)
   */
  public float getDamageMultiplier() {
    return this.damageMultiplier;
  }

  /**
   * Updates the difficulty damage multiplier applied to this melee attacker's damage. Set by {@link
   * com.csse3200.game.difficulty.DifficultyScaler} at spawn - not intended to be called directly by
   * gameplay code.
   *
   * @param damageMultiplier new multiplier value
   * @throws IllegalArgumentException if {@code damageMultiplier} is zero or negative
   */
  public void setDamageMultiplier(float damageMultiplier) throws IllegalArgumentException {
    if (damageMultiplier <= 0) {
      throw new IllegalArgumentException("Damage multiplier must be greater than zero.");
    }
    this.damageMultiplier = damageMultiplier;
  }

  /**
   * Returns the difficulty multiplier applied to this melee attacker's wind-up duration.
   *
   * @return wind-up multiplier; {@code 1f} by default (no change)
   */
  public float getWindupMultiplier() {
    return this.windupMultiplier;
  }

  /**
   * Updates the difficulty multiplier applied to this melee attacker's wind-up duration. Set by
   * {@link com.csse3200.game.difficulty.DifficultyScaler} at spawn - not intended to be called
   * directly by gameplay code.
   *
   * @param windupMultiplier new multiplier value
   * @throws IllegalArgumentException if {@code windupMultiplier} is zero or negative
   */
  public void setWindupMultiplier(float windupMultiplier) throws IllegalArgumentException {
    if (windupMultiplier <= 0) {
      throw new IllegalArgumentException("Windup multiplier must be greater than zero.");
    }
    this.windupMultiplier = windupMultiplier;
  }

  /**
   * Returns the damage for this attack, either the configured base attack from the config file or
   * the equipped weapon's damage. The only stat this component reads from the weapon — range and
   * knockback are this wielder's own properties, not the weapon's.
   *
   * @return the equipped weapon's damage, sourced from {@code weapon.getDamage()}
   */
  public int getDamage() {
    if (this.weapon == null) {
      return this.combatStats.getBaseAttack();
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
   * Returns the windup length before difficulty scaling.
   *
   * @return windup duration in seconds, taken from the weapon (or {@code cooldown - 1} unarmed)
   */
  public float getWindupDuration() {
    return this.windupDuration;
  }

  /**
   * Returns how much of the current windup is left.
   *
   * @return seconds remaining, or {@code 0f} when no windup is in progress
   */
  public float getWindupTimeRemaining() {
    return this.pendingTarget == null ? 0f : Math.max(0f, this.windupTimeRemaining);
  }

  /**
   * Reports whether a swing is currently winding up.
   *
   * @return true between an accepted {@code "meleeAttack"} trigger and the swing resolving
   */
  public boolean isWindingUp() {
    return this.pendingTarget != null;
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
    this.windupTimeRemaining = this.windupDuration * this.windupMultiplier;
    timeSinceLastAttack = 0;
    entity.getEvents().trigger("meleeAttackWindup", this.pendingTarget);
  }

  /**
   * Called once the windup timer elapses. Re-validates the pending target is still alive and in
   * range (it may have died or moved away during the windup). If so, applies weapon damage
   * (multiplied by any active {@link ChargeComponent} bonus and the difficulty multiplier), fires
   * {@code "meleeAttackHit"}, and applies knockback. If not, fires {@code "meleeAttackWhiff"} with
   * the intended target and does nothing else (the cooldown stays spent).
   *
   * <p>The attacker's own base attack is restored after the hit, so repeated hits never drift.
   */
  private void resolveAttack() {
    Entity target = this.pendingTarget;
    this.pendingTarget = null;
    this.windupTimeRemaining = 0;
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null || targetStats.getHealth() <= 0) {
      entity.getEvents().trigger("meleeAttackWhiff", target);
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
      entity.getEvents().trigger("meleeAttackWhiff", target);
      return;
    }
    // retrieve damage stats from weapon
    int finalDamage = this.getDamage();

    ChargeComponent chargeComponent = entity.getComponent(ChargeComponent.class);
    if (chargeComponent != null) {
      // if not charging then 1.0f is the mutiplier
      finalDamage = (int) (finalDamage * chargeComponent.getDamageMultiplier());
    }
    if (weapon != null && damageMultiplier != 1f) {
      finalDamage = Math.max(1, Math.round(finalDamage * damageMultiplier));
    }
    // CombatStatsComponent#hit reads the attacker's base attack, so set it for the hit and put
    // the attacker's own value back straight afterwards.
    int originalBaseAttack = combatStats.getBaseAttack();
    combatStats.setBaseAttack(finalDamage);
    targetStats.hit(combatStats);
    combatStats.setBaseAttack(originalBaseAttack);
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
