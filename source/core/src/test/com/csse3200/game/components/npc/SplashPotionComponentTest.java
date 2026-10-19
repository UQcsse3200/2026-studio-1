package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link SplashPotionComponent}: a thrown potion that is launched on an arc, smashes on the
 * ground, a wall or the player once armed, and removes itself after a maximum flight time.
 *
 * <p>Time is stepped by hand in exact quarter-second steps: two updates arm the potion (0.5s) and
 * sixteen make it expire (4s). The physics world is real but never stepped.
 */
@ExtendWith(GameExtension.class)
class SplashPotionComponentTest {
  private static final float STEP = 0.25f;
  private static final float DELTA = 1e-6f;
  private static final int UPDATES_TO_ARM = 2;
  private static final int UPDATES_TO_EXPIRE = 16;

  private Application originalApp;
  private EntityService entityService;
  private List<Vector2> smashes;
  private Entity potion;
  private SplashPotionComponent flight;

  @BeforeEach
  void beforeEach() {
    originalApp = Gdx.app;
    Gdx.app = mock(Application.class);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(STEP);
    ServiceLocator.registerTimeSource(gameTime);
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    smashes = new ArrayList<>();
    flight = new SplashPotionComponent(new Vector2(3f, 4f), smashes::add);
    potion = new Entity().addComponent(new PhysicsComponent()).addComponent(flight);
    potion.setPosition(1f, 2f);
    potion.setScale(0.5f, 0.5f);
    potion.create();
  }

  @AfterEach
  void afterEach() {
    Gdx.app = originalApp;
  }

  // ---------- launch ----------

  @Test
  void shouldLaunchWithItsLaunchVelocityAndNoDrag() {
    Body body = potion.getComponent(PhysicsComponent.class).getBody();

    assertEquals(3f, body.getLinearVelocity().x, DELTA);
    assertEquals(4f, body.getLinearVelocity().y, DELTA);
    assertEquals(0f, body.getLinearDamping(), DELTA);
  }

  // ---------- smashing ----------

  @Test
  void shouldIgnoreAHitBeforeItIsArmed() {
    update(1);

    hit(PhysicsLayer.OBSTACLE);

    assertTrue(smashes.isEmpty());
    verify(Gdx.app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldSmashAtItsCentreOnceItHitsTheGroundWhileArmed() {
    update(UPDATES_TO_ARM);

    hit(PhysicsLayer.OBSTACLE);

    assertEquals(1, smashes.size());
    assertEquals(1.25f, smashes.get(0).x, DELTA);
    assertEquals(2.25f, smashes.get(0).y, DELTA);
  }

  @Test
  void shouldSmashOnThePlayer() {
    update(UPDATES_TO_ARM);

    hit(PhysicsLayer.PLAYER);

    assertEquals(1, smashes.size());
  }

  @Test
  void shouldFlyThroughOtherPhysicsLayers() {
    update(UPDATES_TO_ARM);

    hit(PhysicsLayer.NPC);

    assertTrue(smashes.isEmpty());
    verify(Gdx.app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldSmashOnlyOnce() {
    update(UPDATES_TO_ARM);

    hit(PhysicsLayer.OBSTACLE);
    hit(PhysicsLayer.PLAYER);

    assertEquals(1, smashes.size());
  }

  @Test
  void shouldRemoveThePotionAfterItSmashes() {
    update(UPDATES_TO_ARM);
    hit(PhysicsLayer.OBSTACLE);

    runScheduledRemoval();

    verify(entityService).unregister(potion);
  }

  // ---------- expiry ----------

  @Test
  void shouldKeepFlyingBeforeItsMaximumFlightTime() {
    update(UPDATES_TO_EXPIRE - 1);

    verify(Gdx.app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldRemoveThePotionWithoutSmashingAfterItsMaximumFlightTime() {
    update(UPDATES_TO_EXPIRE);
    runScheduledRemoval();

    assertTrue(smashes.isEmpty());
    verify(entityService).unregister(potion);
  }

  @Test
  void shouldScheduleItsRemovalOnlyOnceWhenItKeepsUpdating() {
    update(UPDATES_TO_EXPIRE + 2);

    ArgumentCaptor<Runnable> removal = ArgumentCaptor.forClass(Runnable.class);
    verify(Gdx.app).postRunnable(removal.capture());
  }

  @Test
  void shouldNotSmashAfterItHasExpired() {
    update(UPDATES_TO_EXPIRE);

    hit(PhysicsLayer.OBSTACLE);

    assertTrue(smashes.isEmpty());
  }

  // ---------- helpers ----------

  private void update(int steps) {
    for (int i = 0; i < steps; i++) {
      flight.update();
    }
  }

  private void hit(short layer) {
    Filter filter = new Filter();
    filter.categoryBits = layer;
    Fixture other = mock(Fixture.class);
    when(other.getFilterData()).thenReturn(filter);
    potion.getEvents().trigger("collisionStart", mock(Fixture.class), other);
  }

  /** Runs the removal that the component handed to the (mocked) Gdx application. */
  private void runScheduledRemoval() {
    ArgumentCaptor<Runnable> removal = ArgumentCaptor.forClass(Runnable.class);
    verify(Gdx.app).postRunnable(removal.capture());
    removal.getValue().run();
  }
}
