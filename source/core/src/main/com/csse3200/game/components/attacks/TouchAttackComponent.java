package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import java.lang.reflect.Array;

/**
 * When this entity touches a valid enemy's hitbox, deal damage to them and apply a knockback.
 *
 * <p>Requires CombatStatsComponent, HitboxComponent on this entity.
 *
 * <p>Damage is only applied if target entity has a CombatStatsComponent. Knockback is only applied
 * if target entity has a PhysicsComponent.
 */
public class TouchAttackComponent extends Component {
  private short targetLayer;
  private float knockbackForce = 0f;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;

  // always enabled
  private boolean active = true;
  // 0 means unlimited
  private int maxHitsPerActivation = 0;
  private int hitsThisActivation = 0;
  // 1.0 means plan base attack
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

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      // Not triggered by hitbox, ignore
      return;
    }

    if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
      // Doesn't match our target layer, ignore
      return;
    }

    // Try to attack target.
    Entity target = ((BodyUserData) other.getBody().getUserData()).entity;
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats != null) {
      targetStats.hit(combatStats);
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

  /**
   * Switches touch damage on or off without resetting the hit counter. A charging entity is built
   * with this set to false so it is harmless until its rush begins.
   *
   * @param active true to allow damage on contact, false to ignore all contacts.
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * @return true if contacts currently cause damage
   */
  public boolean isActive() {
    return this.active;
  }

  /**
   * Starts a fresh activation: turns damage on, clears teh hit counter and treats any target that
   * is ALREADY touching this entity's hit box as a new contact ( a collision-start event is only
   * raised when two shapes begin to overlap, so a target standing inside the entity when the rush
   * starts would otherwise never be hit.
   */
  public void activate() {
    // turn damage on
    setActive(true);
    // set hits this activation to zero
    this.hitsThisActivation = 0;
    // retrieve every contact currently touching this entity's hit box
    // loop through above list
    // if the contact is touching and the other shape is on the target layer, try to hit the entity
    // that owns that shape.
    Fixture myFixture = hitboxComponent.getFixture();
    // Copy the list: it is rebuilt on every getContactList() call, so a listener that asks the
    // same body for its contacts during a hit must not disturb this loop.
    Array<Contact> contacts = new Array<>(myFixture.getBody().getContactList());
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

  /** Turns damage off. Safe to call when already off. */
}
