package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * When this entity touches a valid enemy's hitbox, deal damage to them and apply a knockback.
 *
 * <p>Requires CombatStatsComponent, HitboxComponent on this entity.
 *
 * <p>Damage is only applied if target entity has a CombatStatsComponent. Knockback is only applied
 * if target entity has a PhysicsComponent.
 *
 * <p>By default the component is always on, unlimited and unscaled, so every existing NPC behaves
 * exactly as before. A {@link ChargeComponent} switches it off, limits it to one hit per activation
 * and scales its damage, so that a charge is the attack: touching the target during the rush deals
 * base attack times the multiplier, once.
 *
 * <p>Fires {@code "touchAttackHit"} (the entity that was hit) each time a hit damages a target.
 */
public class TouchAttackComponent extends Component {
  private static final long BRIBE_DURATION_MILLIS = 20000L;

  private short targetLayer;
  private float knockbackForce = 0f;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private boolean bribed = false;
  private long bribedUntil = 0L;

  /** True while contacts cause damage. Defaults to true, as before. */
  private boolean active = true;

  /** Most hits allowed per activation; 0 means unlimited, as before. */
  private int maxHitsPerActivation = 0;

  /** Hits that have landed since the last activation. */
  private int hitsThisActivation = 0;

  /** Scales the base attack of each hit. 1.0 means plain base attack, as before. */
  private float damageMultiplier = 1.0f;

  /**
   * Create a component which attacks entities on collision, without knockback.
   *
   * @param targetLayer The physics layer of the target's collider.
   */
  public TouchAttackComponent(short targetLayer) {
    this.targetLayer = targetLayer;
  }

  /**
   * Create a component which attacks entities on collision, with knockback.
   *
   * @param targetLayer The physics layer of the target's collider.
   * @param knockback The magnitude of the knockback applied to the entity.
   */
  public TouchAttackComponent(short targetLayer, float knockback) {
    this.targetLayer = targetLayer;
    this.knockbackForce = knockback;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  @Override
  public void update() {
    if (bribed && ServiceLocator.getTimeSource().getTime() >= bribedUntil) {
      bribed = false;
      bribedUntil = 0L;
    }
  }

  /**
   * Bribes this enemy for 20 seconds.
   *
   * @return true when the bribe was applied
   */
  public boolean bribe() {
    bribed = true;
    bribedUntil = ServiceLocator.getTimeSource().getTime() + BRIBE_DURATION_MILLIS;

    entity.getEvents().trigger("enemyBribed", BRIBE_DURATION_MILLIS);

    return true;
  }

  public boolean isBribed() {
    return bribed;
  }

  /**
   * Switches touch damage on or off without resetting the hit counter.
   *
   * @param active true to allow damage on contact, false to ignore all contacts
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Returns whether touch damage is currently on.
   *
   * @return true if contacts currently cause damage
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Starts a fresh activation: damage on, hit counter cleared, and any target that is ALREADY
   * touching this entity's hitbox is treated as a new contact. A collision-start event only fires
   * when two shapes begin to overlap, so a target standing inside the entity when the rush starts
   * would otherwise never be hit.
   */
  public void activate() {
    active = true;
    hitsThisActivation = 0;
    if (hitboxComponent == null) {
      return;
    }
    Fixture myFixture = hitboxComponent.getFixture();
    World world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    // Copy the list: the world rebuilds it on every getContactList() call, so a listener that
    // asks for contacts during a hit must not disturb this loop.
    Array<Contact> contacts = new Array<>(world.getContactList());
    for (Contact contact : contacts) {
      if (!contact.isTouching()) {
        continue;
      }
      Fixture other;
      if (contact.getFixtureA() == myFixture) {
        other = contact.getFixtureB();
      } else if (contact.getFixtureB() == myFixture) {
        other = contact.getFixtureA();
      } else {
        continue;
      }
      if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
        continue;
      }
      Entity target = ((BodyUserData) other.getBody().getUserData()).entity;
      tryHit(target);
    }
  }

  /** Turns touch damage off. Safe to call when already off. */
  public void deactivate() {
    active = false;
  }

  /**
   * Sets how many hits one activation may land.
   *
   * @param maxHits most hits allowed per activation; 0 means unlimited
   * @throws IllegalArgumentException if maxHits is negative
   */
  public void setMaxHitsPerActivation(int maxHits) throws IllegalArgumentException {
    if (maxHits < 0) {
      throw new IllegalArgumentException("maxHits must not be negative");
    }
    maxHitsPerActivation = maxHits;
  }

  /**
   * Returns the per-activation hit limit.
   *
   * @return the limit (0 means unlimited)
   */
  public int getMaxHitsPerActivation() {
    return maxHitsPerActivation;
  }

  /**
   * Returns how many hits have landed since the last activation.
   *
   * @return hits landed since the last activation
   */
  public int getHitsThisActivation() {
    return hitsThisActivation;
  }

  /**
   * Sets the factor applied to this entity's base attack for each hit.
   *
   * @param multiplier scales the entity's base attack; must be greater than zero and finite
   * @throws IllegalArgumentException if zero, negative, NaN or infinite
   */
  public void setDamageMultiplier(float multiplier) throws IllegalArgumentException {
    if (multiplier <= 0 || Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
      throw new IllegalArgumentException("damageMultiplier must be greater than zero");
    }
    damageMultiplier = multiplier;
  }

  /**
   * Returns the current damage multiplier.
   *
   * @return the current multiplier (1.0 unless a charge has raised it)
   */
  public float getDamageMultiplier() {
    return damageMultiplier;
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me || bribed) {
      // Not triggered by hitbox, ignore
      return;
    }

    if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
      // Doesn't match our target layer, ignore
      return;
    }

    // Try to attack target.
    Entity target = ((BodyUserData) other.getBody().getUserData()).entity;

    tryHit(target);
  }

  /**
   * Applies one touch hit to a target: the checks, the damage, the event and the knockback.
   *
   * @param target the entity that was touched
   */
  private void tryHit(Entity target) {
    if (!active) {
      return;
    }
    if (maxHitsPerActivation > 0 && hitsThisActivation >= maxHitsPerActivation) {
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats != null) {
      // The (int) cast rounds down. The two-argument hit takes the amount directly, so this
      // entity's own base attack is never changed, even temporarily.
      int scaledDamage = (int) (combatStats.getBaseAttack() * damageMultiplier);
      targetStats.hit(combatStats, scaledDamage);
      // Only a hit that could hurt uses up the allowance.
      hitsThisActivation++;
      entity.getEvents().trigger("touchAttackHit", target);
    }

    // Apply knockback
    PhysicsComponent physicsComponent = target.getComponent(PhysicsComponent.class);

    if (physicsComponent != null && knockbackForce > 0f) {
      Body targetBody = physicsComponent.getBody();

      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());

      Vector2 impulse = direction.setLength(knockbackForce);

      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }
  }
}
