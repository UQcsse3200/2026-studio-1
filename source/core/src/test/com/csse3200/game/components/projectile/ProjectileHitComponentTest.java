package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileHitComponentTest {
  private static final short TARGET_LAYER = (1 << 3);
  private static final short BLOCKING_LAYER = (1 << 2);
  private static final short UNRELATED_LAYER = (1 << 5);

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldDealDamageOnTargetLayerContact() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity target = createLayered(TARGET_LAYER);

    fireCollision(projectile, target);

    assertEquals(5, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldFireProjectileExpiredOnTargetHit() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity target = createLayered(TARGET_LAYER);
    List<Object> expired = new ArrayList<>();
    projectile.getEvents().addListener("projectileExpired", () -> expired.add(new Object()));

    fireCollision(projectile, target);

    assertEquals(1, expired.size());
  }

  @Test
  void shouldFireProjectileHitEventWithTheEntityThatWasHit() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity target = createLayered(TARGET_LAYER);
    List<Entity> hits = new ArrayList<>();
    projectile.getEvents().addListener("projectileHit", (Entity hit) -> hits.add(hit));

    fireCollision(projectile, target);

    assertEquals(1, hits.size());
    assertEquals(target, hits.get(0));
  }

  @Test
  void shouldIgnoreContactOnAnUnrelatedLayer() {
    Entity projectile = createProjectile(TARGET_LAYER, BLOCKING_LAYER, 0f);
    Entity other = createLayered(UNRELATED_LAYER);

    assertDoesNotThrow(() -> fireCollision(projectile, other));

    assertEquals(10, other.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldStopWithoutDealingDamageOnBlockingLayerContact() {
    Entity projectile = createProjectile(TARGET_LAYER, BLOCKING_LAYER, 0f);
    Entity wall = createLayered(BLOCKING_LAYER);
    List<Object> expired = new ArrayList<>();
    projectile.getEvents().addListener("projectileExpired", () -> expired.add(new Object()));

    fireCollision(projectile, wall);

    assertEquals(10, wall.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, expired.size());
  }

  @Test
  void shouldNotBlockWhenNoBlockingLayerIsConfigured() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity other = createLayered(BLOCKING_LAYER);

    assertDoesNotThrow(() -> fireCollision(projectile, other));

    assertEquals(10, other.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldOnlyEverResolveOnce() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity firstTarget = createLayered(TARGET_LAYER);
    Entity secondTarget = createLayered(TARGET_LAYER);

    fireCollision(projectile, firstTarget);
    fireCollision(projectile, secondTarget);

    assertEquals(5, firstTarget.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(
        10,
        secondTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "A projectile should not be able to land a second hit after already resolving.");
  }

  @Test
  void shouldNotAttackWhenTargetHasNoCombatStatsComponent() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(TARGET_LAYER));
    target.create();

    assertDoesNotThrow(() -> fireCollision(projectile, target));
  }

  @Test
  void shouldApplyKnockbackWhenPositive() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 3f);
    Entity target = createLayered(TARGET_LAYER);
    projectile.setPosition(0, 0);
    target.setPosition(1, 0);

    fireCollision(projectile, target);

    float speed = target.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().len();
    assertTrue(speed > 0f, "Expected knockback to give the target some velocity");
  }

  @Test
  void shouldNotApplyKnockbackWhenZero() {
    Entity projectile = createProjectile(TARGET_LAYER, PhysicsLayer.NONE, 0f);
    Entity target = createLayered(TARGET_LAYER);
    projectile.setPosition(0, 0);
    target.setPosition(1, 0);

    fireCollision(projectile, target);

    float speed = target.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().len();
    assertEquals(0f, speed, 0.001f);
  }

  /** Fires the projectile's own "collisionStart" event, exactly as PhysicsContactListener would. */
  private void fireCollision(Entity projectile, Entity other) {
    Fixture projectileFixture = projectile.getComponent(HitboxComponent.class).getFixture();
    Fixture otherFixture = other.getComponent(HitboxComponent.class).getFixture();
    projectile.getEvents().trigger("collisionStart", projectileFixture, otherFixture);
  }

  private Entity createProjectile(short targetLayer, short blockingLayer, float knockback) {
    Entity projectile =
        new Entity()
            .addComponent(new ProjectileHitComponent(targetLayer, blockingLayer, knockback))
            .addComponent(new CombatStatsComponent(1, 5))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    projectile.create();
    return projectile;
  }

  private Entity createLayered(short layer) {
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    entity.create();
    return entity;
  }
}
