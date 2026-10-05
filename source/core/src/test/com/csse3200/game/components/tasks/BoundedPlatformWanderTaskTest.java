package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link BoundedPlatformWanderTask}: a platform wander that remembers where it first started
 * and never picks a wander target further than {@code radius} from that point, however often it
 * restarts and whatever the parent's ledge or falling logic does to its own centre.
 *
 * <p>Fixture: real physics service (so the parent's ground raycasts work), a mocked debug renderer,
 * an entity with physics, collider and movement, and the task owned by an AI component. The
 * protected {@code getRandomPosInRange()} is called directly: the test is in the same package. The
 * entity's position is its bottom-left corner, which is what the task measures.
 */
@ExtendWith(GameExtension.class)
class BoundedPlatformWanderTaskTest {
  private static final float TOLERANCE = 1e-4f;
  private static final int DRAWS = 400;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerResourceService(mock(ResourceService.class));
  }

  // ---------- constructor ----------

  @Test
  void shouldStoreTheRadius() {
    assertEquals(2f, new BoundedPlatformWanderTask(2f, 1f, 0f).getRadius(), 1e-6f);
  }

  @Test
  void shouldAcceptAVerySmallPositiveRadius() {
    assertDoesNotThrow(() -> new BoundedPlatformWanderTask(0.001f, 1f, 0f));
  }

  @ParameterizedTest(name = "radius {0} is rejected")
  @ValueSource(floats = {0f, -0.5f, -2f})
  void shouldRejectZeroOrNegativeRadius(float radius) {
    assertThrows(
        IllegalArgumentException.class, () -> new BoundedPlatformWanderTask(radius, 1f, 0f));
  }

  @Test
  void shouldRejectNotANumberRadius() {
    assertThrows(
        IllegalArgumentException.class, () -> new BoundedPlatformWanderTask(Float.NaN, 1f, 0f));
  }

  @Test
  void shouldRejectInfiniteRadius() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BoundedPlatformWanderTask(Float.POSITIVE_INFINITY, 1f, 0f));
  }

  @Test
  void shouldBeALowPriorityWanderLikeItsParent() {
    assertEquals(1, new BoundedPlatformWanderTask(2f, 1f, 0f).getPriority());
  }

  // ---------- anchoring ----------

  @Test
  void shouldNotBeAnchoredBeforeItStarts() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);

    assertFalse(task.isAnchored());
  }

  @Test
  void shouldAnchorToTheEntitysPositionWhenItFirstStarts() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);

    task.start();

    assertTrue(task.isAnchored());
    assertEquals(10f, task.getAnchorX(), TOLERANCE);
    assertEquals(entity.getPosition().x, task.getAnchorX(), TOLERANCE);
  }

  @Test
  void shouldKeepTheOriginalAnchorWhenRestarted() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    task.start();

    task.stop();
    entity.setPosition(25f, 1f);
    task.start();

    assertEquals(10f, task.getAnchorX(), TOLERANCE, "restarting must not re-anchor");
  }

  @Test
  void shouldKeepTheOriginalAnchorAcrossManyRestarts() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    task.start();

    for (int i = 1; i <= 5; i++) {
      task.stop();
      entity.setPosition(10f + i * 7f, 1f);
      task.start();
    }

    assertEquals(10f, task.getAnchorX(), TOLERANCE);
  }

  @Test
  void shouldStillAnnounceWanderStartWhenItStarts() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    EventListener0 callback = mock(EventListener0.class);
    entity.getEvents().addListener("wanderStart", callback);

    task.start();

    verify(callback).handle();
  }

  // ---------- the bound ----------

  @Test
  void shouldNeverPickATargetBeyondTheRadius() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    createEntity(task, 10f, 1f);
    task.start();

    for (int i = 0; i < DRAWS; i++) {
      float x = task.getRandomPosInRange().x;
      assertTrue(x >= 8f - TOLERANCE && x <= 12f + TOLERANCE, "target x " + x + " outside [8, 12]");
    }
  }

  @ParameterizedTest(name = "radius {0}")
  @ValueSource(floats = {0.5f, 1f, 2f, 5f})
  void shouldKeepEveryTargetWithinWhateverRadiusItWasGiven(float radius) {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(radius, 1f, 0f);
    createEntity(task, 30f, 1f);
    task.start();

    for (int i = 0; i < DRAWS; i++) {
      float x = task.getRandomPosInRange().x;
      assertTrue(Math.abs(x - 30f) <= radius + TOLERANCE, "target x " + x + " too far from 30");
    }
  }

  @Test
  void shouldWanderOnBothSidesOfTheAnchor() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    createEntity(task, 10f, 1f);
    task.start();
    float lowest = Float.MAX_VALUE;
    float highest = -Float.MAX_VALUE;

    for (int i = 0; i < DRAWS; i++) {
      float x = task.getRandomPosInRange().x;
      lowest = Math.min(lowest, x);
      highest = Math.max(highest, x);
    }

    assertTrue(lowest < 10f, "some targets must be left of the anchor");
    assertTrue(highest > 10f, "some targets must be right of the anchor");
  }

  @Test
  void shouldLeaveTheVerticalTargetAsTheParentChoseIt() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    createEntity(task, 10f, 1.2f);
    task.start();

    for (int i = 0; i < 50; i++) {
      assertEquals(1.2f, task.getRandomPosInRange().y, TOLERANCE);
    }
  }

  // ---------- the drift this class exists to prevent ----------

  @Test
  void shouldNotDriftWhenTheEntityIsMovedAndTheTaskRestarts() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    task.start();

    task.stop();
    entity.setPosition(20f, 1f); // the parent's own centre would now be 20
    task.start();

    for (int i = 0; i < DRAWS; i++) {
      // the parent alone would pick x in [18, 22]; every one must be pulled back to the bound
      assertEquals(12f, task.getRandomPosInRange().x, TOLERANCE);
    }
  }

  @Test
  void shouldWalkBackInsideWhenPushedFarBelowTheAnchor() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    task.start();

    task.stop();
    entity.setPosition(-50f, 1f);
    task.start();

    for (int i = 0; i < DRAWS; i++) {
      assertEquals(8f, task.getRandomPosInRange().x, TOLERANCE);
    }
  }

  @Test
  void shouldNotDriftWhenTheParentShiftsItsCentreWhileAirborne() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 1f, 0f);
    Entity entity = createEntity(task, 10f, 1f);
    task.start();

    // No ground exists in this world, so the parent treats the entity as airborne and moves its
    // wander centre to wherever the entity is.
    entity.setPosition(30f, 1f);
    task.update();

    for (int i = 0; i < DRAWS; i++) {
      float x = task.getRandomPosInRange().x;
      assertTrue(x <= 12f + TOLERANCE, "target x " + x + " escaped past the bound");
    }
  }

  @Test
  void shouldKeepWorkingThroughRepeatedUpdates() {
    BoundedPlatformWanderTask task = new BoundedPlatformWanderTask(2f, 0.01f, 0f);
    createEntity(task, 10f, 1f);
    task.start();

    assertDoesNotThrow(
        () -> {
          for (int i = 0; i < 100; i++) {
            task.update();
          }
        });
    assertEquals(10f, task.getAnchorX(), TOLERANCE);
  }

  @Test
  void shouldKeepTwoTasksAnchoredIndependently() {
    BoundedPlatformWanderTask first = new BoundedPlatformWanderTask(2f, 1f, 0f);
    BoundedPlatformWanderTask second = new BoundedPlatformWanderTask(2f, 1f, 0f);
    createEntity(first, 10f, 1f);
    createEntity(second, 40f, 1f);

    first.start();
    second.start();

    assertEquals(10f, first.getAnchorX(), TOLERANCE);
    assertEquals(40f, second.getAnchorX(), TOLERANCE);
  }

  // ---------- helpers ----------

  private Entity createEntity(BoundedPlatformWanderTask task, float x, float y) {
    AITaskComponent ai = new AITaskComponent().addTask(task);
    Entity entity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(ai);
    entity.create();
    entity.setPosition(new Vector2(x, y));
    return entity;
  }
}
