package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class WanderTaskTest {
  @Mock GameTime gameTime;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldTriggerEvent() {
    WanderTask wanderTask = new WanderTask(Vector2Utils.ONE, 1f);

    AITaskComponent aiTaskComponent = new AITaskComponent().addTask(wanderTask);
    Entity entity =
        new Entity().addComponent(aiTaskComponent).addComponent(new PhysicsMovementComponent());
    entity.create();

    // Register callbacks
    EventListener0 callback = mock(EventListener0.class);
    entity.getEvents().addListener("wanderStart", callback);

    wanderTask.start();

    verify(callback).handle();
  }

  @Test
  void shouldBeALowPriorityTask() {
    assertEquals(1, new WanderTask(Vector2Utils.ONE, 1f).getPriority());
  }

  @Test
  void shouldAlternateBetweenMovingAndWaiting() {
    AtomicLong now = new AtomicLong(0L);
    when(gameTime.getTime()).thenAnswer(invocation -> now.get());
    // A zero range means the random target is the start position, so the move ends at once.
    WanderTask wanderTask = new WanderTask(new Vector2(0f, 0f), 1f);
    Entity wanderer = createWanderer();
    wanderTask.create(() -> wanderer);

    wanderTask.start();
    assertSame(wanderTask.movementTask, wanderTask.currentTask);

    wanderTask.update(); // the move finishes
    wanderTask.update(); // so it starts waiting
    assertSame(wanderTask.waitTask, wanderTask.currentTask);

    now.set(2000L);
    wanderTask.update(); // the wait finishes
    wanderTask.update(); // so it moves again
    assertSame(wanderTask.movementTask, wanderTask.currentTask);
  }

  @Test
  void shouldPickTargetsWithinItsWanderRangeOfWhereItStarted() {
    WanderTask wanderTask = new WanderTask(new Vector2(2f, 2f), 1f);
    Entity wanderer = createWanderer();
    wanderer.setPosition(5f, 5f);
    wanderTask.create(() -> wanderer);
    wanderTask.start();

    for (int i = 0; i < 50; i++) {
      Vector2 target = wanderTask.getRandomPosInRange();
      assertTrue(target.x >= 4f && target.x <= 6f, "x out of range: " + target.x);
      assertTrue(target.y >= 4f && target.y <= 6f, "y out of range: " + target.y);
    }
  }

  private Entity createWanderer() {
    Entity wanderer =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent());
    wanderer.create();
    return wanderer;
  }
}
