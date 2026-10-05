package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the two movement strategies that are not a straight horizontal line: {@link
 * ArcMovementStrategy} (a ballistic arc under gravity) and {@link VerticalMovementStrategy} (a
 * straight fall).
 *
 * <p>Both groups run against a real Box2D body, so they check the velocity the body really ends up
 * with and do not depend on which setter a strategy uses.
 */
@ExtendWith(GameExtension.class)
class ProjectileMovementStrategyTest {
  private static final float G = 9.8f;
  private static final float TOLERANCE = 1e-3f;

  private World world;
  private Body body;
  private Entity projectile;

  @BeforeEach
  void setUp() {
    world = new World(new Vector2(0f, -G), true);
    BodyDef def = new BodyDef();
    def.type = BodyType.DynamicBody;
    body = world.createBody(def);

    PhysicsComponent physics = mock(PhysicsComponent.class);
    when(physics.getBody()).thenReturn(body);
    projectile = mock(Entity.class);
    when(projectile.getComponent(PhysicsComponent.class)).thenReturn(physics);
    when(projectile.getPosition()).thenReturn(new Vector2(0f, 0f));

    PhysicsEngine engine = mock(PhysicsEngine.class);
    when(engine.getWorld()).thenReturn(world);
    PhysicsService physicsService = mock(PhysicsService.class);
    when(physicsService.getPhysics()).thenReturn(engine);
    ServiceLocator.registerPhysicsService(physicsService);
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
    world.dispose();
  }

  /** Position after {@code t} seconds of free flight from {@code from} with velocity {@code v}. */
  private static Vector2 positionAt(Vector2 from, Vector2 v, float t) {
    return new Vector2(from.x + v.x * t, from.y + v.y * t - 0.5f * G * t * t);
  }

  // ======================================================================
  // ArcMovementStrategy
  // ======================================================================

  @Nested
  class Arc {

    // ---- constructor ----

    @Test
    void shouldStoreTheSpeedAndACopyOfTheTarget() {
      Vector2 target = new Vector2(4f, 2f);
      ArcMovementStrategy strategy = new ArcMovementStrategy(3f, target);
      target.set(99f, 99f);
      assertEquals(3f, strategy.getHorizontalSpeed());
      assertEquals(new Vector2(4f, 2f), strategy.getTargetPosition());
    }

    @Test
    void shouldHandBackACopyOfTheTarget() {
      ArcMovementStrategy strategy = new ArcMovementStrategy(3f, new Vector2(4f, 2f));
      Vector2 first = strategy.getTargetPosition();
      first.set(0f, 0f);
      assertNotSame(first, strategy.getTargetPosition());
      assertEquals(new Vector2(4f, 2f), strategy.getTargetPosition());
    }

    @ParameterizedTest
    @ValueSource(floats = {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void shouldRejectBadSpeeds(float speed) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ArcMovementStrategy(speed, new Vector2(1f, 1f)));
    }

    @Test
    void shouldRejectANullTarget() {
      assertThrows(IllegalArgumentException.class, () -> new ArcMovementStrategy(3f, null));
    }

    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.POSITIVE_INFINITY})
    void shouldRejectANonFiniteTarget(float bad) {
      assertThrows(
          IllegalArgumentException.class, () -> new ArcMovementStrategy(3f, new Vector2(bad, 0f)));
      assertThrows(
          IllegalArgumentException.class, () -> new ArcMovementStrategy(3f, new Vector2(0f, bad)));
    }

    @Test
    void shouldAcceptATinyPositiveSpeed() {
      assertEquals(1e-6f, new ArcMovementStrategy(1e-6f, new Vector2(1f, 1f)).getHorizontalSpeed());
    }

    // ---- launch maths ----

    @ParameterizedTest
    @ValueSource(floats = {-6f, -2f, 2f, 6f})
    void shouldReachTheTargetForLevelAboveAndBelow(float dx) {
      for (float dy : new float[] {0f, 3f, -3f}) {
        Vector2 from = new Vector2(1f, 1f);
        Vector2 to = new Vector2(from.x + dx, from.y + dy);
        float speed = 3f;
        Vector2 v = ArcMovementStrategy.computeLaunchVelocity(from, to, speed, G);
        Vector2 landed = positionAt(from, v, Math.abs(dx) / speed);
        assertEquals(to.x, landed.x, TOLERANCE);
        assertEquals(to.y, landed.y, TOLERANCE);
      }
    }

    @Test
    void shouldPointTheSidewaysVelocityTowardsTheTarget() {
      Vector2 right =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(0f, 0f), new Vector2(5f, 0f), 3f, G);
      Vector2 left =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(0f, 0f), new Vector2(-5f, 0f), 3f, G);
      assertEquals(3f, right.x, TOLERANCE);
      assertEquals(-3f, left.x, TOLERANCE);
    }

    @Test
    void shouldMatchTheWorkedExample() {
      Vector2 v =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(0f, 0f), new Vector2(6f, 2f), 3f, G);
      assertEquals(3f, v.x, TOLERANCE);
      assertEquals(10.8f, v.y, TOLERANCE);
    }

    @Test
    void shouldThrowStraightUpWhenTheTargetIsDirectlyAbove() {
      Vector2 v =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(2f, 0f), new Vector2(2f, 4f), 3f, G);
      assertEquals(0f, v.x);
      assertFalse(Float.isNaN(v.y) || Float.isInfinite(v.y));
      assertTrue(v.y > 0f);
    }

    @Test
    void shouldNotDivideByZeroWhenTheTargetIsTheLaunchPoint() {
      Vector2 v =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(2f, 2f), new Vector2(2f, 2f), 3f, G);
      assertFalse(Float.isNaN(v.x) || Float.isNaN(v.y));
      assertFalse(Float.isInfinite(v.x) || Float.isInfinite(v.y));
    }

    @Test
    void shouldLandBackAtTheTargetHeightAfterTheFallbackTimeWhenThrownStraightUp() {
      Vector2 from = new Vector2(0f, 0f);
      Vector2 v = ArcMovementStrategy.computeLaunchVelocity(from, new Vector2(0f, 2f), 3f, G);
      Vector2 at = positionAt(from, v, ArcMovementStrategy.FALLBACK_FLIGHT_TIME);
      assertEquals(2f, at.y, TOLERANCE);
    }

    @Test
    void shouldGiveAStraightLineWithZeroGravity() {
      Vector2 v =
          ArcMovementStrategy.computeLaunchVelocity(
              new Vector2(0f, 0f), new Vector2(6f, 2f), 3f, 0f);
      assertEquals(1f, v.y, TOLERANCE);
    }

    // ---- start and update on a real body ----

    @Test
    void shouldGiveTheBodyTheLaunchVelocityOnStart() {
      new ArcMovementStrategy(3f, new Vector2(6f, 2f)).start(projectile);

      assertEquals(3f, body.getLinearVelocity().x, TOLERANCE);
      assertEquals(10.8f, body.getLinearVelocity().y, TOLERANCE);
    }

    @Test
    void shouldSwitchGravityOnRemoveDampingAndMakeTheBodyABullet() {
      body.setGravityScale(0f);
      body.setLinearDamping(5f);
      body.setBullet(false);

      new ArcMovementStrategy(3f, new Vector2(6f, 2f)).start(projectile);

      assertEquals(1f, body.getGravityScale());
      assertEquals(0f, body.getLinearDamping());
      assertTrue(body.isBullet());
    }

    @Test
    void shouldAimFromWhereTheProjectileIsNotFromTheOrigin() {
      when(projectile.getPosition()).thenReturn(new Vector2(2f, 1f));

      new ArcMovementStrategy(3f, new Vector2(8f, 3f)).start(projectile);

      // dx = 6, dy = 2 from (2, 1): the same velocity as the worked example.
      assertEquals(3f, body.getLinearVelocity().x, TOLERANCE);
      assertEquals(10.8f, body.getLinearVelocity().y, TOLERANCE);
    }

    @Test
    void shouldUseTheWorldsOwnGravityNotAFixedNumber() {
      world.setGravity(new Vector2(0f, -15f));

      new ArcMovementStrategy(8f, new Vector2(4f, 0f)).start(projectile);

      // flight time 0.5 s, so vy = 0.5 * 15 * 0.5
      assertEquals(3.75f, body.getLinearVelocity().y, TOLERANCE);
    }

    @Test
    void shouldLandOnTheTargetWhenTheWorldIsStepped() {
      Vector2 target = new Vector2(6f, 2f);
      new ArcMovementStrategy(3f, target).start(projectile);

      float step = 1f / 240f;
      int steps = Math.round((6f / 3f) / step);
      for (int i = 0; i < steps; i++) {
        world.step(step, 6, 2);
      }

      assertEquals(target.x, body.getPosition().x, 0.1f);
      assertEquals(target.y, body.getPosition().y, 0.1f);
    }

    @Test
    void shouldKeepTheSidewaysSpeedThroughoutBecauseNothingSlowsIt() {
      new ArcMovementStrategy(3f, new Vector2(6f, 2f)).start(projectile);

      for (int i = 0; i < 60; i++) {
        world.step(1f / 60f, 6, 2);
      }

      assertEquals(3f, body.getLinearVelocity().x, 0.05f);
    }

    @Test
    void shouldDoNothingOnStartWithoutAPhysicsComponent() {
      Entity bare = mock(Entity.class);
      when(bare.getComponent(PhysicsComponent.class)).thenReturn(null);

      new ArcMovementStrategy(3f, new Vector2(6f, 2f)).start(bare);
    }

    @Test
    void shouldLeaveTheVelocityAloneOnUpdate() {
      ArcMovementStrategy strategy = new ArcMovementStrategy(3f, new Vector2(6f, 2f));
      strategy.start(projectile);
      Vector2 before = body.getLinearVelocity().cpy();

      strategy.update(projectile, 0.016f);

      assertEquals(before.x, body.getLinearVelocity().x);
      assertEquals(before.y, body.getLinearVelocity().y);
    }
  }

  // ======================================================================
  // VerticalMovementStrategy
  // ======================================================================

  @Nested
  class Vertical {

    @ParameterizedTest
    @ValueSource(floats = {0f, -1f, -12f})
    void shouldRejectANonPositiveSpeed(float speed) {
      assertThrows(IllegalArgumentException.class, () -> new VerticalMovementStrategy(speed));
    }

    @Test
    void shouldAcceptATinyPositiveSpeed() {
      new VerticalMovementStrategy(1e-6f);
    }

    @ParameterizedTest
    @ValueSource(floats = {1f, 5f, 12f})
    void shouldFallStraightDownAtTheGivenSpeed(float speed) {
      new VerticalMovementStrategy(speed).start(projectile);

      assertEquals(0f, body.getLinearVelocity().x, TOLERANCE);
      assertEquals(-speed, body.getLinearVelocity().y, TOLERANCE);
    }

    @Test
    void shouldNotChangeTheVelocityOnUpdate() {
      VerticalMovementStrategy strategy = new VerticalMovementStrategy(8f);
      strategy.start(projectile);
      Vector2 before = body.getLinearVelocity().cpy();

      strategy.update(projectile, 0.016f);
      strategy.update(projectile, 1f);

      assertEquals(before.x, body.getLinearVelocity().x, TOLERANCE);
      assertEquals(before.y, body.getLinearVelocity().y, TOLERANCE);
    }

    @Test
    void shouldKeepFallingAtAConstantSpeedWhenTheWorldIsStepped() {
      body.setGravityScale(0f);
      new VerticalMovementStrategy(8f).start(projectile);

      for (int i = 0; i < 30; i++) {
        world.step(1f / 60f, 6, 2);
      }

      assertEquals(-8f, body.getLinearVelocity().y, 0.05f);
      assertEquals(0f, body.getPosition().x, TOLERANCE);
      assertTrue(body.getPosition().y < 0f, "it should have moved down");
    }
  }
}
