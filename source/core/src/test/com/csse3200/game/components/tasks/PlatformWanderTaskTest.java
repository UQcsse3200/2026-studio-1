package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class PlatformWanderTaskTest {

  @BeforeEach
  void beforeEach() {
    // Mock rendering, physics, game time
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    GameTime gameTime = mock(GameTime.class);

    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    ResourceService resourceService = mock(ResourceService.class);

    ServiceLocator.registerResourceService(resourceService);
  }

  @Test
  void shouldTriggerEvent() {
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 1f, 0f);

    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(wanderTask);
    Entity entity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(aiTaskComponent);
    entity.create();

    // Register callbacks
    EventListener0 callback = mock(EventListener0.class);
    entity.getEvents().addListener("wanderStart", callback);

    wanderTask.start();

    verify(callback).handle();
  }

  @Test
  void shouldFallWithNoPlatform() {
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 0.01f, 0f);
    Entity dummy = createEntity(wanderTask);
    Vector2 initialPosition = new Vector2(1f, 1.2f);
    dummy.setPosition(initialPosition);

    for (int i = 0; i < 24; i++) {
      dummy.earlyUpdate();
      dummy.update();
      ServiceLocator.getPhysicsService().getPhysics().update();
      if (initialPosition.y < dummy.getPosition().y) {
        fail("In area with no collision, owner should free fall.");
      }
      initialPosition = dummy.getPosition();
    }
  }

  @Test
  void shouldTurnBackAtTheRightEdgeOfAPlatform() {
    addPlatform(-5f, 6.5f); // spans x = -5 to 1.5
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE.cpy().scl(2f), 1f, 0f);
    Entity walker = createEntity(wanderTask);
    walker.setPosition(1f, 0f); // left foot on the platform, right foot (x = 2) over the edge
    wanderTask.start();

    wanderTask.update();

    assertEquals(0f, wanderTask.startPos.x, 1e-5f, "the wander range should shift back left");
    assertSame(wanderTask.movementTask, wanderTask.currentTask);
  }

  @Test
  void shouldTurnBackAtTheLeftEdgeOfAPlatform() {
    addPlatform(1.5f, 6.5f); // spans x = 1.5 to 8
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE.cpy().scl(2f), 1f, 0f);
    Entity walker = createEntity(wanderTask);
    walker.setPosition(1f, 0f); // right foot (x = 2) on the platform, left foot over the edge
    wanderTask.start();

    wanderTask.update();

    assertEquals(2f, wanderTask.startPos.x, 1e-5f, "the wander range should shift back right");
    assertSame(wanderTask.movementTask, wanderTask.currentTask);
  }

  @Test
  void shouldAnchorItsRangeToWhereItLandsWhenAirborne() {
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 1f, 0f);
    Entity walker = createEntity(wanderTask);
    walker.setPosition(7f, 3f);
    wanderTask.start();
    walker.setPosition(9f, 4f); // no platform anywhere, so it is still in the air

    wanderTask.update();

    assertEquals(9f, wanderTask.startPos.x, 1e-5f);
    assertEquals(4f, wanderTask.startPos.y, 1e-5f);
  }

  @Test
  void shouldStartMovingAgainAfterWaitingOnAPlatform() {
    AtomicLong now = new AtomicLong(0L);
    when(ServiceLocator.getTimeSource().getTime()).thenAnswer(invocation -> now.get());
    addPlatform(-5f, 20f); // under both feet
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 0.5f, 0f);
    Entity walker = createEntity(wanderTask);
    walker.setPosition(1f, 0f);
    wanderTask.start();
    assertSame(wanderTask.waitTask, wanderTask.currentTask, "it waits first so it can land");

    now.set(1000L);
    wanderTask.update(); // the wait finishes
    wanderTask.update(); // so it moves

    assertSame(wanderTask.movementTask, wanderTask.currentTask);
  }

  /** A static platform whose top surface is just below y = 0, where the walker's feet are. */
  private void addPlatform(float x, float width) {
    Entity platform =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    platform.setScale(width, 1f);
    platform.setPosition(x, -1.05f);
    platform.create();
  }

  Entity createEntity(PlatformWanderTask platformWanderTask) {
    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(platformWanderTask);
    Entity dummy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(aiTaskComponent);
    dummy.create();
    return dummy;
  }
}
