package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

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
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 1f);

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
    PlatformWanderTask wanderTask = new PlatformWanderTask(Vector2Utils.ONE, 0.01f);
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

  Entity createEntity(PlatformWanderTask platformWanderTask) {
    AITaskComponent aiTaskComponent =
        new AITaskComponent()
            .addTask(platformWanderTask);
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