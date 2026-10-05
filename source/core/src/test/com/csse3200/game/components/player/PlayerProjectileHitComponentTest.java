package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerProjectileHitComponentTest {
  private Application previousApp;
  private Entity owner;
  private final List<Entity> hitTargets = new ArrayList<>();

  @BeforeEach
  void setUp() {
    // Capture scheduled disposal without running callbacks on the headless application's thread.
    previousApp = Gdx.app;
    Gdx.app = mock(Application.class);
    ServiceLocator.registerEntityService(new EntityService());
    owner = new Entity().addComponent(new CombatStatsComponent(100, 10));
    owner.create();
    EventListener1<Entity> hitTargetListener = hitTargets::add;
    owner.getEvents().addListener("playerAttackHit", hitTargetListener);
  }

  @AfterEach
  void tearDown() {
    Gdx.app = previousApp;
  }

  @Test
  void shouldRespectTargetShieldAndAvoidDamage() {
    CombatStatsComponent targetStats = new CombatStatsComponent(100, 0);
    targetStats.setShieldHits(1);
    Entity target = new Entity().addComponent(targetStats);
    target.create();

    Entity projectile = createProjectile(owner);
    fireCollision(projectile, target);

    assertEquals(100, targetStats.getHealth());
    assertEquals(0, targetStats.getShieldHits());
    assertEquals(List.of(target), hitTargets);
  }

  @Test
  void shouldTriggerOwnerKillEventWhenProjectileKillsTarget() {
    int[] killEvents = {0};
    owner.getEvents().addListener("enemyKilled", () -> killEvents[0]++);

    CombatStatsComponent targetStats = new CombatStatsComponent(5, 0);
    Entity target = new Entity().addComponent(targetStats);
    target.create();

    Entity projectile = createProjectile(owner);
    fireCollision(projectile, target);

    assertEquals(0, targetStats.getHealth());
    assertEquals(1, killEvents[0]);
    assertEquals(List.of(target), hitTargets);
  }

  @Test
  void shouldReportTargetAfterDamageOnlyOnceWithRepeatedContacts() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 0));
    Entity projectile = createProjectile(owner);
    List<Integer> healthAtNotification = new ArrayList<>();
    owner
        .getEvents()
        .addListener(
            "playerAttackHit",
            (Entity hitTarget) -> {
              healthAtNotification.add(
                  hitTarget.getComponent(CombatStatsComponent.class).getHealth());
            });

    fireCollision(projectile, target);
    fireCollision(projectile, target);

    assertEquals(List.of(target), hitTargets);
    assertEquals(List.of(90), healthAtNotification);
    assertEquals(90, target.getComponent(CombatStatsComponent.class).getHealth());
    verify(Gdx.app).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldNotReportOwnerOrObstacleContacts() {
    Entity projectile = createProjectile(owner);

    fireCollision(projectile, owner);
    fireCollision(projectile, new Entity(), PhysicsLayer.OBSTACLE);

    assertTrue(hitTargets.isEmpty());
    assertEquals(100, owner.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotReportNonNpcTargetsAsEnemyHits() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 0));

    fireCollision(createProjectile(owner), target, PhysicsLayer.PLAYER);

    assertTrue(hitTargets.isEmpty());
  }

  @Test
  void shouldNotReportDeadOrDisposedEnemies() {
    Entity deadTarget = new Entity().addComponent(new CombatStatsComponent(0, 0));
    Entity disposedTarget = new Entity().addComponent(new CombatStatsComponent(100, 0));
    disposedTarget.create();
    disposedTarget.dispose();

    fireCollision(createProjectile(owner), deadTarget);
    fireCollision(createProjectile(owner), disposedTarget);

    assertTrue(hitTargets.isEmpty());
    assertEquals(100, disposedTarget.getComponent(CombatStatsComponent.class).getHealth());
  }

  private Entity createProjectile(Entity owner) {
    HitboxComponent hitbox = mock(HitboxComponent.class);
    Fixture fixture = mock(Fixture.class);
    when(hitbox.getFixture()).thenReturn(fixture);

    Entity projectile =
        new Entity().addComponent(hitbox).addComponent(new PlayerProjectileHitComponent(10, owner));
    projectile.create();
    return projectile;
  }

  private void fireCollision(Entity projectile, Entity target) {
    fireCollision(projectile, target, PhysicsLayer.NPC);
  }

  private void fireCollision(Entity projectile, Entity target, short layer) {
    Fixture targetFixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData userData = new BodyUserData();
    userData.entity = target;
    Filter filter = new Filter();
    filter.categoryBits = layer;
    when(targetFixture.getFilterData()).thenReturn(filter);
    when(targetFixture.getBody()).thenReturn(body);
    when(body.getUserData()).thenReturn(userData);

    projectile
        .getEvents()
        .trigger(
            "collisionStart",
            projectile.getComponent(HitboxComponent.class).getFixture(),
            targetFixture);
  }
}
