package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/** When this entity touches a valid target's hitbox, deal damage to them and apply a knockback. */
public class TouchAttackComponent extends Component {
  private static final long BRIBE_DURATION_MILLIS = 20000L;

  private short targetLayer;
  private float knockbackForce = 0f;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;

  private boolean bribed = false;
  private long bribedUntil = 0L;

  public TouchAttackComponent(short targetLayer) {
    this.targetLayer = targetLayer;
  }

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

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (bribed) {
      return;
    }

    if (!PhysicsLayer.contains(targetLayer, other.getFilterData().categoryBits)) {
      return;
    }

    Entity target = ((BodyUserData) other.getBody().getUserData()).entity;

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    if (targetStats != null) {
      targetStats.hit(combatStats);
    }

    PhysicsComponent physicsComponent = target.getComponent(PhysicsComponent.class);

    if (physicsComponent != null && knockbackForce > 0f) {
      Body targetBody = physicsComponent.getBody();

      Vector2 direction = target.getCenterPosition().sub(entity.getCenterPosition());

      Vector2 impulse = direction.setLength(knockbackForce);

      targetBody.applyLinearImpulse(impulse, targetBody.getWorldCenter(), true);
    }
  }
}
