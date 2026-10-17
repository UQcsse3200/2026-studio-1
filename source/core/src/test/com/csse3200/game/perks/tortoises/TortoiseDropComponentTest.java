package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link TortoiseDropComponent}: it validates its id, spawns one tortoise at the enemy's feet
 * when the enemy's {@code "death"} event fires, never twice, and never for a tortoise that has
 * already been found.
 *
 * <p>The entity service is a mock that records what is registered, so no game is running. The
 * render-thread hop is checked both ways: with no application the work runs straight away, and with
 * one it is posted.
 */
@ExtendWith(GameExtension.class)
class TortoiseDropComponentTest {
  private static final String ID = "zeus_tortoise";

  private EntityService entityService;

  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoiseFactory.resetAll();
    TortoiseTestSupport.registerFactoryServices();
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
  }

  @AfterEach
  void tearDown() {
    TortoiseFactory.resetAll();
  }

  /** An enemy at (4, 7), scale (2, 3), carrying the tortoise; created so it listens for death. */
  private Entity carrier(String id) {
    Entity enemy = new Entity().addComponent(new TortoiseDropComponent(id));
    enemy.setPosition(4f, 7f);
    enemy.setScale(2f, 3f);
    enemy.create();
    return enemy;
  }

  /** Fires the death event with no application, so the spawn runs on the spot. */
  private void die(Entity enemy) {
    TortoiseTestSupport.withApp(null, () -> enemy.getEvents().trigger("death"));
  }

  private Entity onlyRegisteredEntity() {
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService).register(captor.capture());
    return captor.getValue();
  }

  // ---------- construction ----------

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void shouldRejectANullOrBlankId(String id) {
    assertThrows(IllegalArgumentException.class, () -> new TortoiseDropComponent(id));
  }

  @Test
  void shouldReportTheIdItWasGiven() {
    assertEquals(ID, new TortoiseDropComponent(ID).getTortoiseId());
  }

  // ---------- death ----------

  @Test
  void shouldNotSpawnBeforeTheEnemyDies() {
    carrier(ID);

    verify(entityService, never()).register(any());
  }

  @Test
  void shouldSpawnOneTortoiseOnDeath() {
    die(carrier(ID));

    Entity tortoise = onlyRegisteredEntity();
    assertNotNull(tortoise.getComponent(TortoiseComponent.class));
  }

  @Test
  void shouldSpawnAtTheEnemysFeetInTheMiddleOfItsWidth() {
    die(carrier(ID));

    Vector2 spot = onlyRegisteredEntity().getPosition();
    assertEquals(5f, spot.x, 0.001f);
    assertEquals(7f, spot.y, 0.001f);
  }

  @Test
  void shouldGiveTheTortoiseTheConfiguredId() {
    die(carrier(ID));

    Object id =
        TortoiseTestSupport.readField(
            onlyRegisteredEntity().getComponent(TortoiseComponent.class), "tortoiseId");
    assertEquals(ID, id);
  }

  @Test
  void shouldSpawnOnlyOnceIfDeathFiresTwice() {
    Entity enemy = carrier(ID);

    die(enemy);
    die(enemy);

    verify(entityService, times(1)).register(any());
  }

  @Test
  void shouldNotSpawnATortoiseThatWasAlreadyFound() {
    TortoiseRegistry.markFound(ID);

    die(carrier(ID));

    verify(entityService, never()).register(any());
  }

  @Test
  void shouldNotSpawnAgainAfterAFoundTortoiseIsClearedAndDeathFiresAgain() {
    Entity enemy = carrier(ID);
    TortoiseRegistry.markFound(ID);
    die(enemy);
    TortoiseRegistry.resetAll();

    die(enemy);

    verify(entityService, never()).register(any());
  }

  @Test
  void shouldPostTheSpawnWhenAnApplicationIsRunning() {
    Application app = mock(Application.class);
    Entity enemy = carrier(ID);

    TortoiseTestSupport.withApp(app, () -> enemy.getEvents().trigger("death"));

    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(app).postRunnable(task.capture());
    verify(entityService, never()).register(any());

    task.getValue().run();

    assertNotNull(onlyRegisteredEntity().getComponent(TortoiseComponent.class));
  }

  // ---------- spawnAt and the render-thread hop ----------

  @Test
  void spawnAtShouldRegisterAPositionedTortoiseAndReturnIt() {
    Entity tortoise = TortoiseDropComponent.spawnAt("rnd_hound_s_den_1", new Vector2(3f, 2f));

    verify(entityService).register(tortoise);
    assertEquals(3f, tortoise.getPosition().x, 0.001f);
    assertEquals(2f, tortoise.getPosition().y, 0.001f);
    assertEquals(
        "rnd_hound_s_den_1",
        TortoiseTestSupport.readField(
            tortoise.getComponent(TortoiseComponent.class), "tortoiseId"));
  }

  @Test
  void runOnRenderThreadShouldRunTheTaskStraightAwayWithoutAnApplication() throws Exception {
    AtomicBoolean ran = new AtomicBoolean(false);

    TortoiseTestSupport.withApp(null, () -> invokeRunOnRenderThread(() -> ran.set(true)));

    assertTrue(ran.get());
  }

  @Test
  void runOnRenderThreadShouldPostTheTaskWhenAnApplicationIsRunning() {
    Application app = mock(Application.class);
    AtomicBoolean ran = new AtomicBoolean(false);
    Runnable task = () -> ran.set(true);

    TortoiseTestSupport.withApp(app, () -> invokeRunOnRenderThread(task));

    verify(app).postRunnable(task);
    assertEquals(false, ran.get());
  }

  @Test
  void shouldReturnTheSameTaskObjectItWasGiven() {
    Application app = mock(Application.class);
    Runnable task = () -> {};

    TortoiseTestSupport.withApp(app, () -> invokeRunOnRenderThread(task));

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(app).postRunnable(captor.capture());
    assertSame(task, captor.getValue());
  }

  private static void invokeRunOnRenderThread(Runnable task) {
    try {
      Method method =
          TortoiseDropComponent.class.getDeclaredMethod("runOnRenderThread", Runnable.class);
      method.setAccessible(true);
      method.invoke(null, task);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
