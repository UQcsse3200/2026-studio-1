package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;

/**
 * Handles player projectile collisions and damage.
 *
 * <p>Hits on living NPCs emit {@code playerAttackHit(Entity target)} on the owner after damage is
 * resolved, including shielded and lethal hits. Listeners must check whether the target survived.
 * This event runs inside the collision callback: queue responses and create physics bodies later.
 * Companion projectiles must use their own hit handler so their hits cannot trigger more assists.
 */
public class PlayerProjectileHitComponent extends Component {
  private final int damage;
  private final Entity owner;

  private CombatStatsComponent ownerCombatStats;
  private HitboxComponent hitboxComponent;
  private boolean collided;

  public PlayerProjectileHitComponent(int damage, Entity owner) {
    if (damage < 0) {
      throw new IllegalArgumentException("Projectile damage must not be negative.");
    }
    if (owner == null) {
      throw new IllegalArgumentException("Projectile owner must not be null.");
    }

    this.damage = damage;
    this.owner = owner;
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    ownerCombatStats = owner.getComponent(CombatStatsComponent.class);
    if (ownerCombatStats == null) {
      throw new IllegalStateException("Projectile owner must have combat stats.");
    }
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (collided || hitboxComponent == null || hitboxComponent.getFixture() != me) {
      return;
    }

    Object userData = other.getBody().getUserData();

    if (!(userData instanceof BodyUserData bodyUserData) || bodyUserData.entity == null) {
      removeProjectile();
      return;
    }

    Entity target = bodyUserData.entity;

    // Do not damage the entity that fired the projectile.
    if (target == owner) {
      return;
    }

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    if (!target.isDisposed() && targetStats != null && !targetStats.isDead()) {
      // Mark this projectile resolved before notifying any hit listeners.
      removeProjectile();
      targetStats.hit(ownerCombatStats, damage);
      if (PhysicsLayer.contains(PhysicsLayer.NPC, other.getFilterData().categoryBits)) {
        owner.getEvents().trigger("playerAttackHit", target);
      }
      return;
    }

    removeProjectile();
  }

  private void removeProjectile() {
    if (collided) {
      return;
    }

    collided = true;
    Gdx.app.postRunnable(entity::dispose);
  }
}
