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
 * range and this component is off cooldown.
 */
public class MeleeAttackComponent extends Component {
  private static final long BRIBE_DURATION_MILLIS = 20000L;

  private float range;
  private float cooldown;
  private float knockback;
  private float damage;
  private WeaponItem weapon;
  private float windupDuration;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;
  private Entity pendingTarget;
  private float windupTimeRemaining;

  /** Difficulty's enemy damage multiplier, applied on top of weapon (and charge) damage. */
  private float damageMultiplier = 1f;

  private float windupMultiplier = 1f;

  private boolean bribed = false;
  private long bribedUntil = 0L;

  private static final Logger logger = LoggerFactory.getLogger(MeleeAttackComponent.class);

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

  public MeleeAttackComponent(float range, float cooldown, float knockback) {
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    this.timeSinceLastAttack = cooldown;
    this.damage = this.getEntity().getComponent(CombatStatsComponent.class).getBaseAttack();
  }

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("meleeAttack", this::attemptAttack);
  }

  @Override
  public void update() {
    timeSinceLastAttack += ServiceLocator.getTimeSource().getDeltaTime();

    if (bribed && ServiceLocator.getTimeSource().getTime() >= bribedUntil) {
      bribed = false;
      bribedUntil = 0L;
      logger.info("Enemy {} is no longer bribed", entity.getId());
    }

    if (pendingTarget != null) {
      if (bribed) {
        pendingTarget = null;
        windupTimeRemaining = 0f;
        return;
      }

      windupTimeRemaining -= ServiceLocator.getTimeSource().getDeltaTime();

      if (windupTimeRemaining <= 0) {
        resolveAttack();
      }
    }
  }

  public float getRange() {
    return this.range;
  }

  public void setRange(float range) throws IllegalArgumentException {
    if (range < 0) {
      throw new IllegalArgumentException("range must not be negative");
    }
    this.range = range;
  }

  public float getCooldown() {
    return this.cooldown;
  }

  public void setCooldown(float cooldown) throws IllegalArgumentException {
    if (cooldown <= 0) {
      throw new IllegalArgumentException("Cooldown duration must be greater than zero.");
    }
    this.cooldown = cooldown;
  }

  public float getKnockback() {
    return this.knockback;
  }

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

  public boolean canAttack() {
    return timeSinceLastAttack >= this.getCooldown();
  }

  /**
   * Bribes this enemy for 20 seconds.
   *
   * @return true when the bribe was applied
   */
  public boolean bribe() {
    bribed = true;
    bribedUntil = ServiceLocator.getTimeSource().getTime() + BRIBE_DURATION_MILLIS;

    pendingTarget = null;
    windupTimeRemaining = 0f;

    logger.info("Enemy {} bribed for {}ms", entity.getId(), BRIBE_DURATION_MILLIS);

    entity.getEvents().trigger("enemyBribed", BRIBE_DURATION_MILLIS);

    return true;
  }

  public boolean isBribed() {
    return bribed;
  }

  private void attemptAttack(Entity target) {
    if (target == null || bribed) {
      return;
    }

    if (this.timeSinceLastAttack < this.getCooldown()) {
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

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats == null) {
      return;
    }

    this.pendingTarget = target;
    this.windupTimeRemaining = this.windupDuration * this.windupMultiplier;
    timeSinceLastAttack = 0;

    entity.getEvents().trigger("meleeAttackWindup", this.pendingTarget);
  }

  private void resolveAttack() {
    Entity target = this.pendingTarget;
    this.pendingTarget = null;
    this.windupTimeRemaining = 0;

    if (bribed || target == null) {
      return;
    }

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

    int finalDamage = this.getDamage();

    ChargeComponent chargeComponent = entity.getComponent(ChargeComponent.class);

    if (chargeComponent != null) {
      finalDamage = (int) (finalDamage * chargeComponent.getDamageMultiplier());
    }

    if (weapon != null && damageMultiplier != 1f) {
      finalDamage = Math.max(1, Math.round(finalDamage * damageMultiplier));
    }

    combatStats.setBaseAttack(finalDamage);

    targetStats.hit(combatStats);

    entity.getEvents().trigger("meleeAttackHit", target);

    this.timeSinceLastAttack = 0;

    PhysicsComponent targetPhysics = target.getComponent(PhysicsComponent.class);

    if (targetPhysics != null && this.getKnockback() > 0) {
      Body targetBody = targetPhysics.getBody();

      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());

      Vector2 impulse = direction.setLength(this.getKnockback());

      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }
  }
}
