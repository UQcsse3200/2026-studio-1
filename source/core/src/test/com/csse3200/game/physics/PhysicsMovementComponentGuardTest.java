package com.csse3200.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.MovementGuard;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the movement guard hook on {@link PhysicsMovementComponent}: an optional veto over the
 * walking direction that must leave every existing behaviour alone when no guard is set.
 *
 * <p>Fixture: one entity with physics and a movement controller, gravity off, grounded movement on,
 * moving towards a target 20 units to its right. The guard is a lambda, so the hook is tested
 * without any hazards.
 */
@ExtendWith(GameExtension.class)
class PhysicsMovementComponentGuardTest {
  private static final float FRAME = 0.02f;

  private Entity entity;
  private PhysicsMovementComponent movement;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(FRAME);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());

    entity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new PhysicsMovementComponent());
    entity.create();
    movement = entity.getComponent(PhysicsMovementComponent.class);
    movement.setGroundedMovement(true);
    body().setGravityScale(0f);
    entity.setPosition(0f, 5f);
    movement.setTarget(new Vector2(20f, 5f));
  }

  @Test
  void shouldHaveNoGuardByDefault() {
    assertNull(movement.getMovementGuard());
  }

  @Test
  void shouldWalkNormallyWithNoGuard() {
    step(5);

    assertTrue(body().getLinearVelocity().x > 0.5f);
  }

  @Test
  void shouldStoreAndReturnTheGuard() {
    MovementGuard guard = direction -> false;

    movement.setMovementGuard(guard);

    assertSame(guard, movement.getMovementGuard());
  }

  @Test
  void shouldReplaceAnEarlierGuard() {
    MovementGuard first = direction -> false;
    MovementGuard second = direction -> true;
    movement.setMovementGuard(first);

    movement.setMovementGuard(second);

    assertSame(second, movement.getMovementGuard());
  }

  @Test
  void shouldClearTheGuardWhenGivenNull() {
    movement.setMovementGuard(direction -> true);

    movement.setMovementGuard(null);

    assertNull(movement.getMovementGuard());
    step(5);
    assertTrue(body().getLinearVelocity().x > 0.5f, "with no guard the walk is free again");
  }

  @Test
  void shouldWalkNormallyWhenTheGuardAllowsIt() {
    movement.setMovementGuard(direction -> false);

    step(5);

    assertTrue(body().getLinearVelocity().x > 0.5f);
  }

  @Test
  void shouldBringTheSidewaysVelocityToZeroWhenTheGuardBlocks() {
    step(5); // get up to speed first
    assertTrue(body().getLinearVelocity().x > 0.5f);

    movement.setMovementGuard(direction -> true);
    step(5);

    assertEquals(0f, body().getLinearVelocity().x, 1e-3f);
  }

  @Test
  void shouldAnnounceMovementBlockedOnEveryBlockedFrame() {
    AtomicInteger blocked = new AtomicInteger();
    entity.getEvents().addListener("movementBlocked", blocked::incrementAndGet);
    movement.setMovementGuard(direction -> true);

    step(3);

    assertEquals(3, blocked.get());
  }

  @Test
  void shouldNotAnnounceAnythingWhenTheGuardAllowsTheWalk() {
    AtomicInteger blocked = new AtomicInteger();
    entity.getEvents().addListener("movementBlocked", blocked::incrementAndGet);
    movement.setMovementGuard(direction -> false);

    step(5);

    assertEquals(0, blocked.get());
  }

  @Test
  void shouldAskTheGuardWithAUnitDirectionTowardsTheTarget() {
    List<Vector2> seen = new ArrayList<>();
    movement.setMovementGuard(
        direction -> {
          seen.add(direction.cpy());
          return false;
        });

    step(1);

    assertEquals(1, seen.size());
    assertEquals(1f, seen.get(0).len(), 1e-4f);
    assertEquals(1f, seen.get(0).x, 1e-4f);
    assertEquals(0f, seen.get(0).y, 1e-4f, "grounded movement drops the vertical part");
  }

  @Test
  void shouldGiveTheGuardACopyNotTheInternalVector() {
    List<Vector2> seen = new ArrayList<>();
    movement.setMovementGuard(
        direction -> {
          seen.add(direction);
          direction.set(-1f, 0f); // a careless guard changes what it was given
          return false;
        });

    step(5);

    assertTrue(
        body().getLinearVelocity().x > 0.5f, "the walk must not be reversed by a guard's edit");
    assertNotSame(seen.get(0), seen.get(1), "each frame gets its own vector");
  }

  @Test
  void shouldNotAskTheGuardWithoutATarget() {
    AtomicInteger asked = new AtomicInteger();
    movement.setTarget(null);
    movement.setMovementGuard(
        direction -> {
          asked.incrementAndGet();
          return true;
        });

    step(5);

    assertEquals(0, asked.get());
  }

  @Test
  void shouldNotAskTheGuardWhileMovementIsDisabled() {
    AtomicInteger asked = new AtomicInteger();
    movement.setMoving(false);
    movement.setMovementGuard(
        direction -> {
          asked.incrementAndGet();
          return true;
        });

    step(5);

    assertEquals(0, asked.get());
  }

  @Test
  void shouldStillLetPhysicsMoveABlockedEntityVertically() {
    // A blocked walk vetoes only the walking: gravity and knockback still act.
    body().setGravityScale(1f);
    movement.setMovementGuard(direction -> true);
    float startY = entity.getPosition().y;

    step(25);

    assertTrue(entity.getPosition().y < startY, "a blocked entity still falls");
  }

  @Test
  void shouldWalkAgainTheMomentTheGuardStopsBlocking() {
    boolean[] block = {true};
    movement.setMovementGuard(direction -> block[0]);
    step(5);
    assertEquals(0f, body().getLinearVelocity().x, 1e-3f);

    block[0] = false;
    step(5);

    assertTrue(body().getLinearVelocity().x > 0.5f);
  }

  // ---------- helpers ----------

  private Body body() {
    return entity.getComponent(PhysicsComponent.class).getBody();
  }

  private void step(int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      entity.earlyUpdate();
      entity.update();
    }
  }
}
