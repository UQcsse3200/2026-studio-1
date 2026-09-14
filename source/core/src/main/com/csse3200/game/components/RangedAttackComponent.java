package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;

/**
 * Fires a real arrow projectile at a target entity when triggered, provided the target is within
 * {@code range} and this component is off cooldown. Attacks are triggered externally (e.g. by a
 * {@link com.csse3200.game.components.tasks.RangedAttackTask}) via an event; range is checked as an
 * explicit distance calculation between entity positions, same as before, but that check now only
 * gates whether an arrow is <i>fired</i> - whether it actually lands is resolved later and
 * separately, by the arrow's own Box2D collision (see {@link
 * com.csse3200.game.components.projectile.ProjectileHitComponent}), once it has actually travelled
 * to (or past, or been blocked before reaching) the target.
 *
 * <p><b>Cooldown now resets on firing, not on a landed hit.</b> This is a deliberate change from
 * this class's previous (and {@link MeleeAttackComponent}'s current) behaviour of only resetting
 * the cooldown after a successful hit. That worked for melee because the attack and its resolution
 * happen in the same instant; a fired arrow resolves asynchronously, some time later, and may miss
 * entirely (out of range, blocked by an obstacle, target moved) - if cooldown only reset on a
 * landed hit, a miss would leave the cooldown gate open and this component would fire a new arrow
 * every single frame {@link com.csse3200.game.components.tasks.RangedAttackTask} re-triggers the
 * event while still in range, instead of at most once per {@code cooldown} seconds.
 *
 * <p>Requires {@link CombatStatsComponent} on this entity - its {@code baseAttack} becomes the
 * fired arrow's damage.
 *
 * <p><b>Limitation:</b> this class does not determine when an attack should be attempted - it
 * relies entirely on being triggered externally with a target entity (see {@link
 * com.csse3200.game.components.tasks.RangedAttackTask}).
 */
public class RangedAttackComponent extends Component {
  // How far in front of the shooter's centre the arrow spawns, so it doesn't spawn inside the
  // shooter's own collider and immediately register a (harmless, but pointless) self-collision.
  private static final float SPAWN_OFFSET = 0.3f;
  // Default speed for a fired arrow when the caller doesn't override it via
  // setProjectileSpeed(...) - matches RangedAttackConfig's own previous default.
  private static final float DEFAULT_PROJECTILE_SPEED = 8f;

  private float range;
  private float cooldown;
  private float knockback;
  private float projectileSpeed;
  private float timeSinceLastAttack;
  private CombatStatsComponent combatStats;

  /**
   * Creates a ranged attack component with configurable range, cooldown, knockback, and projectile
   * speed.
   *
   * @param range attack reach, checked as a direct distance calculation between this entity's and
   *     the target's positions; also used as the fired arrow's maximum flight distance.
   * @param cooldown minimum time, in seconds, between successive shots being fired.
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback.
   * @param projectileSpeed speed, in world units/second, the fired arrow travels at.
   */
  public RangedAttackComponent(float range, float cooldown, float knockback) {
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    setProjectileSpeed(DEFAULT_PROJECTILE_SPEED);
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
   * knockback (see {@link com.csse3200.game.components.projectile.ProjectileHitComponent}).
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
   * Returns the configured projectile speed.
   *
   * @return projectile speed, in world units/second
   */
  public float getProjectileSpeed() {
    return this.projectileSpeed;
  }

  /**
   * Updates the projectile speed.
   *
   * @param projectileSpeed new projectile speed, in world units/second
   * @throws IllegalArgumentException if {@code projectileSpeed} is not positive
   */
  public void setProjectileSpeed(float projectileSpeed) {
    if (projectileSpeed <= 0) {
      throw new IllegalArgumentException("projectileSpeed must be positive");
    }
    this.projectileSpeed = projectileSpeed;
  }

  /**
   * Attempts to fire an arrow at the given target entity: validates cooldown and range, then spawns
   * a real projectile aimed at it if both checks pass and the target has the required component(s).
   * Whether the shot actually connects is resolved later by the arrow itself.
   *
   * @param target the entity being aimed at
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

    // handle whether target has a combat stats component - if it can't take damage, don't bother
    // firing at it at all
    if (target.getComponent(CombatStatsComponent.class) == null) {
      return;
    }

    // The shot is being fired regardless of whether the arrow eventually connects - see this
    // class's javadoc for why cooldown has to gate firing rather than landing now.
    this.timeSinceLastAttack = 0;

    boolean movingRight = target.getPosition().x >= entity.getPosition().x;
    Vector2 spawnPosition =
        entity.getCenterPosition().add(movingRight ? SPAWN_OFFSET : -SPAWN_OFFSET, 0f);

    Entity arrow =
        ArrowFactory.createRangedArrow(
            spawnPosition,
            movingRight,
            projectileSpeed,
            range,
            combatStats.getBaseAttack(),
            knockback,
            PhysicsLayer.PLAYER);

    // Re-fire "rangedAttackHit" on the shooter (this entity) if the arrow lands - preserves the
    // event for anything already listening for it there (e.g. OnHitEffectComponent), without
    // ProjectileHitComponent needing to know it's specifically a "ranged attack" that fired it.
    arrow
        .getEvents()
        .addListener(
            "projectileHit",
            (Entity hitTarget) -> entity.getEvents().trigger("rangedAttackHit", hitTarget));

    ServiceLocator.getEntityService().register(arrow);

    // announce that a shot was fired - useful for triggering the attack animation (see
    // RangedAttackTask, which already fires "rangedAttackStart" once when the AI task begins, but
    // nothing currently listens for it)
    entity.getEvents().trigger("rangedAttackFired", target);
  }
}
