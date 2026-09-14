package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;

/** Handles projectile collisions and damage. */
public class ProjectileHitComponent extends Component {
  private final int damage;
  private final Entity owner;

  private HitboxComponent hitboxComponent;
  private boolean collided;

  public ProjectileHitComponent(int damage, Entity owner) {
    if (damage < 0) {
      throw new IllegalArgumentException("Projectile damage must not be negative.");
    }

    this.damage = damage;
    this.owner = owner;
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
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

    if (targetStats != null) {
      targetStats.addHealth(-damage);
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
