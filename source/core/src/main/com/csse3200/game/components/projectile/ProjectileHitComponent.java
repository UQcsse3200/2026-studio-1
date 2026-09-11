package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;

/**
 * Resolves a projectile's real (Box2D) collisions, replacing a blind distance check: deals damage
 * (and optional knockback) to whatever it actually makes contact with on {@code targetLayer}, and
 * stops it dead - no damage - against anything on {@code blockingLayer} (e.g. a wall), so the
 * projectile can no longer land a hit "through" something solid in the way. Either outcome ends the
 * projectile's flight by firing a {@code "projectileExpired"} event on this entity, which {@link
 * ProjectileComponent} listens for to actually despawn it.
 *
 * <p>This intentionally duplicates, rather than reuses, {@link TouchAttackComponent}'s collision
 * handling: that class's fields and handler are {@code private} (so it can't be extended without
 * re-declaring them anyway - see {@link MeleeAttackComponent}'s javadoc for the same reasoning
 * applied to melee/ranged attack components), and a projectile also needs behaviour {@link
 * TouchAttackComponent} doesn't have: it must stop on non-target ("blocking") contact too, and it
 * must only ever resolve once, since a fast-moving fixture can generate more than one collision
 * callback in a single physics step.
 *
 * <p>Requires {@link CombatStatsComponent} and {@link HitboxComponent} on this entity - the
 * projectile's own damage and hitbox respectively. Damage is only applied if the entity it hits
 * also has a {@link CombatStatsComponent}. Knockback is only applied if that entity has a {@link
 * PhysicsComponent} and {@code knockbackForce > 0}.
 *
 * <p>This component only knows that it hit <i>something</i> on {@code targetLayer} - it does not
 * know or care who fired it, or what a successful hit should be called from the shooter's point of
 * view. It fires a generic {@code "projectileHit"} event (payload: the entity that was hit) for
 * whoever spawned the projectile to react to - e.g. re-firing a {@code "rangedAttackHit"} event on
 * the shooter's own entity, to preserve that event for anything already listening for it (see
 * {@link com.csse3200.game.components.attacks.RangedAttackComponent}).
 */
public class ProjectileHitComponent extends Component {
  private final short targetLayer;
  private final short blockingLayer;
  private final float knockbackForce;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private boolean resolved = false;

  /**
   * Creates a projectile hit component with no blocking layer and no knockback - it only ever
   * resolves against {@code targetLayer}.
   *
   * @param targetLayer the physics layer this projectile deals damage to on contact.
   */
  public ProjectileHitComponent(short targetLayer) {
    this(targetLayer, PhysicsLayer.NONE, 0f);
  }

  /**
   * Creates a projectile hit component with no knockback.
   *
   * @param targetLayer the physics layer this projectile deals damage to on contact.
   * @param blockingLayer physics layer(s) that stop the projectile without dealing damage (e.g.
   *     {@link PhysicsLayer#OBSTACLE}); pass {@link PhysicsLayer#NONE} to disable blocking
   *     entirely.
   */
  public ProjectileHitComponent(short targetLayer, short blockingLayer) {
    this(targetLayer, blockingLayer, 0f);
  }

  /**
   * @param targetLayer the physics layer this projectile deals damage to on contact.
   * @param blockingLayer physics layer(s) that stop the projectile without dealing damage; pass
   *     {@link PhysicsLayer#NONE} to disable blocking entirely.
   * @param knockbackForce knockback magnitude applied to the target on a successful hit; {@code 0f}
   *     results in no knockback.
   */
  public ProjectileHitComponent(short targetLayer, short blockingLayer, float knockbackForce) {
    this.targetLayer = targetLayer;
    this.blockingLayer = blockingLayer;
    this.knockbackForce = knockbackForce;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (resolved || hitboxComponent.getFixture() != me) {
      // Already resolved this projectile's flight, or this callback isn't about our own hitbox
      // (an entity can have more than one fixture) - ignore either way.
      return;
    }

    short otherLayer = other.getFilterData().categoryBits;

    if (PhysicsLayer.contains(targetLayer, otherLayer)) {
      resolveHit(other);
    } else if (blockingLayer != PhysicsLayer.NONE
        && PhysicsLayer.contains(blockingLayer, otherLayer)) {
      resolveBlocked();
    }
    // Anything else (e.g. the shooter's own body, another NPC) is neither a valid target nor a
    // blocker, so the projectile just keeps flying through it - matches how every other collider
    // in this codebase ignores layers it wasn't configured to care about.
  }

  private void resolveHit(Fixture otherFixture) {
    Entity target = ((BodyUserData) otherFixture.getBody().getUserData()).entity;
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats != null) {
      targetStats.hit(combatStats);
      entity.getEvents().trigger("projectileHit", target);
    }

    PhysicsComponent targetPhysics = target.getComponent(PhysicsComponent.class);
    if (targetPhysics != null && knockbackForce > 0f) {
      Body targetBody = targetPhysics.getBody();
      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());
      Vector2 impulse = direction.setLength(knockbackForce);
      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }

    resolve();
  }

  private void resolveBlocked() {
    resolve();
  }

  private void resolve() {
    resolved = true;
    entity.getEvents().trigger("projectileExpired");
  }
}
