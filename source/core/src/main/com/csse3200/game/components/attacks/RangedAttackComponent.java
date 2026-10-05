package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p><b>Windup.</b> If the weapon has a windup above zero, an accepted attack first winds up
 * ({@code "rangedAttackWindup"}), then fires ({@code "rangedAttackFired"}) or, if the target died
 * meanwhile, is cancelled ({@code "rangedAttackCancelled"}). A windup of zero announces the windup
 * and fires straight away.
 *
 * <p><b>Event prefix.</b> The names above use the default prefix {@code "rangedAttack"}. A subclass
 * may choose another prefix through the protected constructor, in which case the trigger event is
 * the prefix itself and the others add {@code Windup}, {@code Fired}, {@code Hit} and {@code
 * Cancelled}. Subclasses change only {@link #createProjectile(Entity, ProjectileType)}.
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

  /** The prefix used by the plain component. Existing enemies keep it. */
  private static final String DEFAULT_EVENT_PREFIX = "rangedAttack";

  /** Lightning freeze length used unless the shooter says otherwise. */
  private static final int DEFAULT_LIGHTNING_FREEZE_TICKS = 120;

  private final String eventPrefix;
  private float range;
  private float cooldown;
  private float knockback;
  private float projectileSpeed;
  private float damage;
  private WeaponItem weapon;
  private float windupDuration;
  private float windupTimeRemaining;
  private float timeSinceLastAttack;
  private int lightningFreezeTicks = DEFAULT_LIGHTNING_FREEZE_TICKS;
  // The shot waiting out its windup; null when no windup is in progress.
  private Entity pendingTarget;
  private ProjectileType pendingProjectile;
  // When true, arrows fly straight at the target's centre instead of along the x axis.
  private boolean aimed = false;
  private CombatStatsComponent combatStats;
  private static final Logger logger = LoggerFactory.getLogger(RangedAttackComponent.class);

  /**
   * Creates a ranged attack component with configurable range, cooldown, knockback, and projectile
   * speed.
   *
   * @param range attack reach, checked as a direct distance calculation between this entity's and
   *     the target's positions; also used as the fired arrow's maximum flight distance.
   * @param cooldown minimum time, in seconds, between successive shots being fired.
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback.
   * @param weapon weapon in entity's inventory
   */
  public RangedAttackComponent(float range, float cooldown, float knockback, WeaponItem weapon) {
    this(range, cooldown, knockback, weapon, DEFAULT_EVENT_PREFIX);
  }

  /**
   * Creates a ranged attack that listens for and announces events under a custom prefix. The
   * trigger event is the prefix itself, for example {@code "rockAttack"}, and every other event
   * adds a suffix: {@code Windup}, {@code Fired}, {@code Hit}, {@code Cancelled}.
   *
   * <p>Two ranged attacks on one entity must not both answer to {@code "rangedAttack"}, or one
   * trigger would start both. The animation controller also needs the prefix to tell a rock from a
   * laser.
   *
   * @param range maximum distance to the target, in world units
   * @param cooldown seconds between shots
   * @param knockback knockback applied on a hit
   * @param weapon the weapon this attack uses (may be a natural weapon)
   * @param eventPrefix prefix for every event this attack listens for or announces; not blank
   * @throws IllegalArgumentException if the prefix is null or blank, plus every rule of the
   *     four-argument constructor
   */
  protected RangedAttackComponent(
      float range, float cooldown, float knockback, WeaponItem weapon, String eventPrefix) {
    if (eventPrefix == null || eventPrefix.isBlank()) {
      throw new IllegalArgumentException("eventPrefix must not be blank");
    }
    this.eventPrefix = eventPrefix;
    setProjectileSpeed(DEFAULT_PROJECTILE_SPEED);
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    if (weapon == null) {
      throw new IllegalArgumentException("weapon cannot be null");
    }
    this.weapon = weapon;
    if (weapon.getWeaponType() == WeaponType.DAGGER
        || weapon.getWeaponType() == WeaponType.SWORD
        || weapon.getWeaponType() == WeaponType.AXE) {
      throw new IllegalArgumentException("Ranged Attack cannot use a dagger or sword weapon");
    }

    if (weapon.getWindupDuration() < 0) {
      throw new IllegalArgumentException("windupDuration must not be negative.");
    }

    if (weapon.getWindupDuration() >= this.getCooldown()) {
      throw new IllegalArgumentException("windupDuration must be less than cooldown");
    }

    this.windupDuration = weapon.getWindupDuration();
    this.timeSinceLastAttack = cooldown;
    this.damage = this.weapon.getDamage();
  }

  /**
   * Creates a ranged attack component with configurable range, cooldown, knockback, with no weapon
   * for any attacks i.e. the rocks where no weapons are in the inventory.
   *
   * @param range attack reach, checked as a direct distance calculation between this entity's and
   *     the target's positions; also used as the fired arrow's maximum flight distance.
   * @param cooldown minimum time, in seconds, between successive shots being fired.
   * @param knockback knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback.
   */
  public RangedAttackComponent(float range, float cooldown, float knockback) {
    this.eventPrefix = DEFAULT_EVENT_PREFIX;
    setRange(range);
    setKnockback(knockback);
    setCooldown(cooldown);
    if (cooldown <= 1) {
      throw new IllegalArgumentException(
          "Windupduration for attack must be less than cooldown and must be positive.");
    }
    this.setWindupDuration(this.getCooldown() - 1);
    this.timeSinceLastAttack = cooldown;
    setProjectileSpeed(DEFAULT_PROJECTILE_SPEED);
  }

  /**
   * Resolves this entity's {@link CombatStatsComponent} and registers a listener for the
   * attack-trigger event, which is the event prefix.
   */
  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener(eventPrefix, this::attemptAttack);
  }

  /** Advances the internal cooldown timer by the time elapsed since the last frame. */
  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    timeSinceLastAttack += delta;
    if (pendingTarget != null) {
      windupTimeRemaining -= delta;
      if (windupTimeRemaining <= 0) {
        resolveShot();
      }
    }
  }

  /**
   * Returns the event prefix this attack uses.
   *
   * @return the event prefix
   */
  public String getEventPrefix() {
    return eventPrefix;
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
   * Returns the damage for this attack, either the configured base attack from the config file or
   * the equipped weapon's damage. The only stat this component reads from the weapon: range and
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
   * Returns how many ticks a lightning bolt from this shooter freezes its victim for.
   *
   * @return freeze length in ticks, 120 unless changed
   */
  public int getLightningFreezeTicks() {
    return lightningFreezeTicks;
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
   * Returns the windup length.
   *
   * @return windup duration in seconds
   */
  public float getWindupDuration() {
    return windupDuration;
  }

  /**
   * Returns how much of the current windup is left.
   *
   * @return seconds remaining, or {@code 0f} when no windup is in progress
   */
  public float getWindupTimeRemaining() {
    return pendingTarget == null ? 0f : Math.max(0f, windupTimeRemaining);
  }

  /**
   * Reports whether a shot is currently winding up.
   *
   * @return true between an accepted trigger and the shot resolving
   */
  public boolean isWindingUp() {
    return pendingTarget != null;
  }

  /**
   * Reports whether arrows are aimed at the target's centre.
   *
   * @return true if aimed; false (the default) for a straight horizontal shot
   */
  public boolean isAimed() {
    return aimed;
  }

  /**
   * Chooses between a straight horizontal arrow (false) and an arrow flown along the line to the
   * target's centre (true). Only affects {@link ProjectileType#ARROW}; lightning keeps its own
   * spawn.
   *
   * @param aimed true to aim arrows at the target
   */
  public void setAimed(boolean aimed) {
    this.aimed = aimed;
  }

  public void setWindupDuration(float windupDuration) {
    this.windupDuration = windupDuration;
  }

  /**
   * Attempts to attack the given target: validates the target, cooldown and range, spends the
   * cooldown, then either fires straight away (windup of zero) or starts a windup. During a windup
   * it announces {@code prefix + "Windup"} (target); the shot itself happens in {@link
   * #resolveShot()} once the windup ends. The cooldown is spent when the attack is accepted, so a
   * cancelled windup is not refunded.
   *
   * @param target the entity being aimed at
   * @param projectile the type of projectile being launched
   */
  private void attemptAttack(Entity target, ProjectileType projectile) {
    if (target == null || projectile == null) {
      return;
    }
    // cooldown check (a pending windup has already spent it, so this also blocks a second windup)
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
    // The shot is being committed regardless of whether the arrow eventually connects - see this
    // class's javadoc for why cooldown has to gate firing rather than landing.
    this.timeSinceLastAttack = 0;
    this.pendingTarget = target;
    this.pendingProjectile = projectile;
    this.windupTimeRemaining = this.windupDuration;
    entity.getEvents().trigger(eventPrefix + "Windup", target);
    if (this.windupDuration <= 0f) {
      resolveShot();
    }
  }

  /**
   * Called once the windup timer elapses. Re-checks that the target is still alive. If so the shot
   * is fired, aimed at the target's CURRENT position; if not, {@code prefix + "Cancelled"} (target)
   * is announced and no projectile is created. Range is checked at commit only, so a target that
   * leaves range during the windup is still shot at (the projectile's own range decides whether it
   * can land).
   */
  private void resolveShot() {
    Entity target = this.pendingTarget;
    ProjectileType projectile = this.pendingProjectile;
    this.pendingTarget = null;
    this.pendingProjectile = null;
    this.windupTimeRemaining = 0;
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null || targetStats.getHealth() <= 0) {
      entity.getEvents().trigger(eventPrefix + "Cancelled", target);
      return;
    }
    fireShot(target, projectile);
  }

  /**
   * Builds the projectile for a resolved shot. The default builds the arrow or the lightning bolt.
   * Subclasses override only this. Aimed arrows fly along the line to the target's centre; unaimed
   * arrows fly along the x axis towards the target's side.
   *
   * @param target the entity being shot at (alive and valid when this is called)
   * @param projectile the projectile type requested by the task
   * @return the projectile entity, not yet registered; the caller registers it, wires the hit event
   *     and announces the shot
   */
  protected Entity createProjectile(Entity target, ProjectileType projectile) {
    boolean movingRight = target.getPosition().x >= entity.getPosition().x;
    Vector2 aimDirection = target.getCenterPosition().sub(entity.getCenterPosition());
    if (aimDirection.isZero()) {
      aimDirection.set(1f, 0f);
    }
    Vector2 spawnPosition =
        aimed && projectile == ProjectileType.ARROW
            ? entity.getCenterPosition().mulAdd(aimDirection.cpy().nor(), SPAWN_OFFSET)
            : entity.getCenterPosition().add(movingRight ? SPAWN_OFFSET : -SPAWN_OFFSET, 0f);
    return switch (projectile) {
      case ARROW ->
          aimed
              ? ArrowFactory.createAimedArrow(
                  spawnPosition,
                  aimDirection,
                  projectileSpeed,
                  range,
                  this.getDamage(),
                  knockback,
                  PhysicsLayer.PLAYER)
              : ArrowFactory.createRangedArrow(
                  spawnPosition,
                  movingRight,
                  projectileSpeed,
                  range,
                  this.getDamage(),
                  knockback,
                  PhysicsLayer.PLAYER);
      case LIGHTNING ->
          ArrowFactory.createLightning(
              target.getCenterPosition(),
              8f,
              12f,
              this.getDamage(),
              this.getLightningFreezeTicks(),
              PhysicsLayer.PLAYER);
    };
  }

  /**
   * Registers the projectile built by {@link #createProjectile(Entity, ProjectileType)} and
   * announces the shot.
   *
   * @param target the entity being shot at
   * @param projectile the type of projectile to launch
   */
  private void fireShot(Entity target, ProjectileType projectile) {
    Entity shot = createProjectile(target, projectile);
    // Re-announce the hit on the shooter (this entity) if the projectile lands - preserves the
    // event for anything already listening for it there (e.g. OnHitEffectComponent), without
    // ProjectileHitComponent needing to know which attack fired it.
    shot.getEvents()
        .addListener(
            "projectileHit",
            (Entity hitTarget) -> entity.getEvents().trigger(eventPrefix + "Hit", hitTarget));
    ServiceLocator.getEntityService().register(shot);
    // announce that a shot was fired - useful for triggering the attack animation
    entity.getEvents().trigger(eventPrefix + "Fired", target);
  }
}
