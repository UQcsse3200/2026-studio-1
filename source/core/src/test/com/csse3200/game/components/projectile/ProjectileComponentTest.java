package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ProjectileComponentTest {
  private ProjectileMovementStrategy movementStrategy;
  private Application originalApp;

  @BeforeEach
  void beforeEach() {
    originalApp = Gdx.app;
    Gdx.app = mock(Application.class);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);

    movementStrategy = mock(ProjectileMovementStrategy.class);
  }

  @AfterEach
  void afterEach() {
    Gdx.app = originalApp;
  }

  @Test
  void shouldRejectNonPositiveMaxRange() {
    assertThrows(
        IllegalArgumentException.class, () -> new ProjectileComponent(movementStrategy, 0f));
    assertThrows(
        IllegalArgumentException.class, () -> new ProjectileComponent(movementStrategy, -1f));
  }

  @Test
  void shouldStartMovementStrategyOnCreate() {
    Entity projectile = createProjectile(10f);
    verify(movementStrategy).start(projectile);
  }

  @Test
  void shouldNotBeExpiredInitially() {
    Entity projectile = createProjectile(10f);
    assertFalse(projectile.getComponent(ProjectileComponent.class).isExpired());
  }

  @Test
  void shouldDelegateMovementToStrategyEachFrameWhileNotExpired() {
    Entity projectile = createProjectile(10f);
    projectile.update();
    projectile.update();

    verify(movementStrategy, times(2)).update(eq(projectile), anyFloat());
  }

  @Test
  void shouldDespawnOnceMaxRangeIsReached() {
    Entity projectile = createProjectile(5f);
    // The mock movement strategy doesn't actually move anything, so move the projectile
    // ourselves to simulate it having travelled past its max range.
    projectile.setPosition(6f, 0f);

    projectile.update();

    assertTrue(projectile.getComponent(ProjectileComponent.class).isExpired());
    assertEquals(-1000f, projectile.getPosition().x, 0.001f);
    assertEquals(-1000f, projectile.getPosition().y, 0.001f);
  }

  @Test
  void shouldNotDespawnBeforeMaxRangeIsReached() {
    Entity projectile = createProjectile(5f);
    projectile.setPosition(3f, 0f);

    projectile.update();

    assertFalse(projectile.getComponent(ProjectileComponent.class).isExpired());
  }

  @Test
  void shouldStopUpdatingMovementStrategyAfterDespawn() {
    Entity projectile = createProjectile(5f);
    projectile.setPosition(6f, 0f);

    projectile.update(); // travels past max range and despawns here
    projectile.update(); // should now be a no-op - the entity itself is disabled
    projectile.update();

    verify(movementStrategy, times(1)).update(eq(projectile), anyFloat());
  }

  @Test
  void shouldDespawnWhenProjectileExpiredEventFires() {
    // Simulates ProjectileHitComponent (or anything else) ending the projectile's flight early,
    // well before it would otherwise reach maxRange.
    Entity projectile = createProjectile(100f);

    projectile.getEvents().trigger("projectileExpired");

    // Hits arrive from inside Box2D's physics step, so the despawn must be deferred, not immediate
    assertFalse(projectile.getComponent(ProjectileComponent.class).isExpired());

    projectile.update();

    assertTrue(projectile.getComponent(ProjectileComponent.class).isExpired());
    verify(movementStrategy, never()).update(eq(projectile), anyFloat());
    verify(Gdx.app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void projectileExpiredEventShouldBeIdempotent() {
    Entity projectile = createProjectile(100f);
    List<Vector2> relocations = new ArrayList<>();
    projectile
        .getEvents()
        .addListener("setPosition", (Vector2 position) -> relocations.add(position));

    assertDoesNotThrow(
        () -> {
          projectile.getEvents().trigger("projectileExpired");
          projectile.getEvents().trigger("projectileExpired");
          projectile.update();
          projectile.update(); // repeated requests must still relocate the projectile only once
        });
    assertTrue(projectile.getComponent(ProjectileComponent.class).isExpired());
    assertEquals(1, relocations.size());
  }

  @Test
  void shouldDeferDespawnUntilUpdateAfterARealPhysicsContact() {
    PhysicsService physicsService = new PhysicsService();
    EntityService entities = new EntityService();
    ServiceLocator.registerPhysicsService(physicsService);
    ServiceLocator.registerEntityService(entities);
    var physics = physicsService.getPhysics();
    try {
      Entity projectile =
          new Entity()
              .addComponent(new PhysicsComponent())
              .addComponent(new HitboxComponent())
              .addComponent(new ProjectileComponent(movementStrategy, 100f));
      Entity wall =
          new Entity()
              .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
              .addComponent(new HitboxComponent());
      entities.register(projectile);
      entities.register(wall);
      ProjectileComponent lifecycle = projectile.getComponent(ProjectileComponent.class);
      Thread simulationThread = Thread.currentThread();
      List<Thread> contacts = new ArrayList<>();
      List<Vector2> relocations = new ArrayList<>();
      projectile
          .getEvents()
          .addListener(
              "collisionStart",
              (Fixture me, Fixture other) -> {
                assertTrue(physics.getWorld().isLocked());
                projectile.getEvents().trigger("projectileExpired");
                assertFalse(lifecycle.isExpired());
                contacts.add(Thread.currentThread());
              });
      projectile
          .getEvents()
          .addListener(
              "setPosition",
              (Vector2 position) -> {
                assertFalse(physics.getWorld().isLocked());
                assertSame(simulationThread, Thread.currentThread());
                relocations.add(position.cpy());
              });

      physics.update();

      assertEquals(List.of(simulationThread), contacts);
      assertFalse(lifecycle.isExpired());
      assertTrue(relocations.isEmpty());
      verify(Gdx.app, never()).postRunnable(any(Runnable.class));

      projectile.earlyUpdate();
      projectile.update();

      assertTrue(lifecycle.isExpired());
      assertEquals(List.of(new Vector2(-1000f, -1000f)), relocations);
      assertEquals(
          Vector2.Zero,
          projectile.getComponent(PhysicsComponent.class).getBody().getLinearVelocity());
      verify(movementStrategy, never()).update(eq(projectile), anyFloat());
    } finally {
      entities.dispose();
      physics.dispose();
    }
  }

  private Entity createProjectile(float maxRange) {
    Entity entity = new Entity().addComponent(new ProjectileComponent(movementStrategy, maxRange));
    entity.create();
    return entity;
  }
}
