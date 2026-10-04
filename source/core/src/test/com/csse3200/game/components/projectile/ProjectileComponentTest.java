package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

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

    runPostedRunnables();

    assertTrue(projectile.getComponent(ProjectileComponent.class).isExpired());
  }

  @Test
  void projectileExpiredEventShouldBeIdempotent() {
    Entity projectile = createProjectile(100f);

    assertDoesNotThrow(
        () -> {
          projectile.getEvents().trigger("projectileExpired");
          projectile.getEvents().trigger("projectileExpired");
          runPostedRunnables(); // both queued despawns run; the second must be a no-op
        });
    assertTrue(projectile.getComponent(ProjectileComponent.class).isExpired());
  }

  private void runPostedRunnables() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(Gdx.app, atLeastOnce()).postRunnable(captor.capture());
    captor.getAllValues().forEach(Runnable::run);
  }

  private Entity createProjectile(float maxRange) {
    Entity entity = new Entity().addComponent(new ProjectileComponent(movementStrategy, maxRange));
    entity.create();
    return entity;
  }
}