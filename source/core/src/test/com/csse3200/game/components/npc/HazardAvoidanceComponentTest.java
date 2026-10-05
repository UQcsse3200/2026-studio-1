package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.csse3200.game.components.HazardDamageComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link HazardAvoidanceComponent}: a short horizontal ray ahead of the collider's leading
 * edge that vetoes a walk into a hazard.
 *
 * <p>Fixture: real physics, gravity off for the walker so only the sideways walk is under test.
 * Gaps are measured from the walker's collider edge to the hazard's near edge, read back from the
 * bodies themselves, so the numbers do not depend on how a collider is aligned. The look-ahead is
 * 0.6 world units unless a test says otherwise; gaps used are well clear of that threshold (0.3
 * inside, 1.5 outside).
 */
@ExtendWith(GameExtension.class)
class HazardAvoidanceComponentTest {
  private static final float FRAME = 0.02f;
  private static final float LOOKAHEAD = 0.6f;
  private static final float INSIDE = 0.3f;
  private static final float OUTSIDE = 1.5f;
  private static final Vector2 RIGHT = new Vector2(1f, 0f);
  private static final Vector2 LEFT = new Vector2(-1f, 0f);

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(FRAME);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  // ---------- constructor ----------

  @Test
  void shouldStoreTheLookahead() {
    assertEquals(1.2f, new HazardAvoidanceComponent(1.2f).getLookahead(), 1e-6f);
  }

  @Test
  void shouldUseAboutOneAndAQuarterTilesByDefault() {
    assertEquals(
        HazardAvoidanceComponent.DEFAULT_LOOKAHEAD,
        new HazardAvoidanceComponent().getLookahead(),
        1e-6f);
    assertEquals(0.6f, HazardAvoidanceComponent.DEFAULT_LOOKAHEAD, 1e-6f);
  }

  @ParameterizedTest(name = "lookahead {0} is rejected")
  @ValueSource(floats = {0f, -0.1f, -5f})
  void shouldRejectAZeroOrNegativeLookahead(float lookahead) {
    assertThrows(IllegalArgumentException.class, () -> new HazardAvoidanceComponent(lookahead));
  }

  @Test
  void shouldRejectNotANumberAndInfiniteLookahead() {
    assertThrows(IllegalArgumentException.class, () -> new HazardAvoidanceComponent(Float.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> new HazardAvoidanceComponent(Float.POSITIVE_INFINITY));
  }

  // ---------- registration ----------

  @Test
  void shouldRegisterItselfAsTheMovementGuardWhenCreated() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));

    PhysicsMovementComponent movement = walker.getComponent(PhysicsMovementComponent.class);

    assertSame(walker.getComponent(HazardAvoidanceComponent.class), movement.getMovementGuard());
  }

  @Test
  void shouldNotThrowWhenTheEntityHasNoMovementComponent() {
    Entity noMovement =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HazardAvoidanceComponent(LOOKAHEAD));

    assertDoesNotThrow(noMovement::create);
  }

  // ---------- the ray: what it blocks ----------

  @Test
  void shouldBlockAWalkRightIntoAHazardInsideTheLookahead() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, INSIDE);

    assertTrue(guard(walker).blocks(RIGHT.cpy()));
  }

  @Test
  void shouldBlockAWalkLeftIntoAHazardInsideTheLookahead() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheLeft(walker, INSIDE);

    assertTrue(guard(walker).blocks(LEFT.cpy()));
  }

  @Test
  void shouldNotBlockWhenTheHazardIsBeyondTheLookahead() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, OUTSIDE);

    assertFalse(guard(walker).blocks(RIGHT.cpy()));
  }

  @Test
  void shouldNotBlockWalkingAwayFromAHazard() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, INSIDE);

    assertFalse(guard(walker).blocks(LEFT.cpy()), "the hazard is behind, not ahead");
  }

  @Test
  void shouldNotBlockWhenThereIsNoHazardAtAll() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));

    assertFalse(guard(walker).blocks(RIGHT.cpy()));
    assertFalse(guard(walker).blocks(LEFT.cpy()));
  }

  @Test
  void shouldIgnoreAnObstacleThatIsNotAHazard() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createBlockToTheRight(walker, INSIDE, PhysicsLayer.OBSTACLE, false);

    assertFalse(guard(walker).blocks(RIGHT.cpy()), "walls are not hazards");
  }

  @Test
  void shouldRespectALongerLookahead() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(2.0f));
    createHazardToTheRight(walker, OUTSIDE);

    assertTrue(guard(walker).blocks(RIGHT.cpy()), "1.5 is inside a look-ahead of 2.0");
  }

  @Test
  void shouldNotBlockAPurelyVerticalWalk() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, INSIDE);

    assertFalse(guard(walker).blocks(new Vector2(0f, 1f)));
    assertFalse(guard(walker).blocks(new Vector2(0f, -1f)));
  }

  @Test
  void shouldTreatADiagonalWalkByItsSidewaysPart() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, INSIDE);

    assertTrue(guard(walker).blocks(new Vector2(0.7f, 0.7f)));
    assertFalse(guard(walker).blocks(new Vector2(-0.7f, 0.7f)));
  }

  @Test
  void shouldNotChangeTheDirectionItIsGiven() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, INSIDE);
    Vector2 direction = new Vector2(1f, 0f);

    guard(walker).blocks(direction);

    assertEquals(1f, direction.x, 1e-6f);
    assertEquals(0f, direction.y, 1e-6f);
  }

  // ---------- end to end: the walker really stops ----------

  @Test
  void shouldStopAtTheEdgeOfAHazardInsteadOfWalkingIn() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    Entity hazard = createHazardToTheRight(walker, 1.0f);
    walker
        .getComponent(PhysicsMovementComponent.class)
        .setTarget(new Vector2(30f, walker.getPosition().y));

    stepFrames(walker, 200); // four seconds at full speed would be well past the hazard

    float walkerRight = aabb(walker)[2];
    float hazardLeft = aabb(hazard)[0];
    assertTrue(walkerRight <= hazardLeft + 1e-3f, "the walker must stay outside the hazard");
    assertEquals(0f, body(walker).getLinearVelocity().x, 1e-3f, "and be standing still");
  }

  @Test
  void shouldNotBeHurtBecauseItNeverTouchesTheHazard() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, 1.0f);
    AtomicInteger touches = new AtomicInteger();
    walker.getEvents().addListener("collisionStart", (a, b) -> touches.incrementAndGet());
    walker
        .getComponent(PhysicsMovementComponent.class)
        .setTarget(new Vector2(30f, walker.getPosition().y));

    stepFrames(walker, 200);

    assertEquals(0, touches.get(), "no contact with the hazard ever began");
  }

  @Test
  void shouldAnnounceThatMovementWasBlocked() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, 1.0f);
    AtomicInteger blocked = new AtomicInteger();
    walker.getEvents().addListener("movementBlocked", blocked::incrementAndGet);
    walker
        .getComponent(PhysicsMovementComponent.class)
        .setTarget(new Vector2(30f, walker.getPosition().y));

    stepFrames(walker, 200);

    assertTrue(blocked.get() > 0);
  }

  @Test
  void shouldWalkFreelyAwayFromTheHazardAfterItsTargetMovesToTheOtherSide() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    createHazardToTheRight(walker, 1.0f);
    PhysicsMovementComponent movement = walker.getComponent(PhysicsMovementComponent.class);
    movement.setTarget(new Vector2(30f, walker.getPosition().y));
    stepFrames(walker, 150);
    float stoppedAt = aabb(walker)[0];

    movement.setTarget(new Vector2(-30f, walker.getPosition().y));
    stepFrames(walker, 100);

    assertTrue(
        aabb(walker)[0] < stoppedAt - 0.5f, "it must walk away once its target is behind it");
  }

  @Test
  void shouldWalkNormallyWhenNoHazardIsInTheWay() {
    Entity walker = createWalker(0f, new HazardAvoidanceComponent(LOOKAHEAD));
    walker
        .getComponent(PhysicsMovementComponent.class)
        .setTarget(new Vector2(30f, walker.getPosition().y));
    float startLeft = aabb(walker)[0];

    stepFrames(walker, 100);

    assertTrue(aabb(walker)[0] > startLeft + 0.5f);
  }

  @Test
  void shouldLeaveAnEntityWithoutTheComponentFreeToWalkIntoAHazard() {
    // Control: the same setup without the component walks straight into the hazard.
    Entity walker = createWalker(0f, null);
    Entity hazard = createHazardToTheRight(walker, 1.0f);
    walker
        .getComponent(PhysicsMovementComponent.class)
        .setTarget(new Vector2(30f, walker.getPosition().y));

    stepFrames(walker, 200);

    assertTrue(aabb(walker)[2] > aabb(hazard)[0], "without avoidance it ends up in the hazard");
  }

  // ---------- helpers ----------

  /**
   * A one by one collider with gravity off, grounded movement, and the given avoidance component.
   */
  private Entity createWalker(float x, HazardAvoidanceComponent avoidance) {
    Entity walker =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new PhysicsMovementComponent());
    if (avoidance != null) {
      walker.addComponent(avoidance);
    }
    walker.setScale(1f, 1f);
    walker.create();
    walker.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    body(walker).setGravityScale(0f);
    walker.setPosition(x, 5f);
    return walker;
  }

  private com.csse3200.game.physics.components.MovementGuard guard(Entity walker) {
    com.csse3200.game.physics.components.MovementGuard guard =
        walker.getComponent(PhysicsMovementComponent.class).getMovementGuard();
    assertNotNull(guard, "the component should have registered itself");
    return guard;
  }

  private Entity createHazardToTheRight(Entity walker, float gap) {
    return createBlockToTheRight(walker, gap, PhysicsLayer.HAZARD, true);
  }

  private Entity createHazardToTheLeft(Entity walker, float gap) {
    Entity hazard = newBlock(PhysicsLayer.HAZARD, true);
    float[] w = aabb(walker);
    float[] h = aabb(hazard);
    float dx = (w[0] - gap) - h[2];
    float dy = ((w[1] + w[3]) / 2f) - ((h[1] + h[3]) / 2f);
    hazard.setPosition(hazard.getPosition().x + dx, hazard.getPosition().y + dy);
    return hazard;
  }

  private Entity createBlockToTheRight(Entity walker, float gap, short layer, boolean withDamage) {
    Entity block = newBlock(layer, withDamage);
    float[] w = aabb(walker);
    float[] h = aabb(block);
    float dx = (w[2] + gap) - h[0];
    float dy = ((w[1] + w[3]) / 2f) - ((h[1] + h[3]) / 2f);
    block.setPosition(block.getPosition().x + dx, block.getPosition().y + dy);
    return block;
  }

  private Entity newBlock(short layer, boolean withDamage) {
    Entity block =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(
                new ColliderComponent().setLayer(layer).setSensor(layer == PhysicsLayer.HAZARD));
    if (withDamage) {
      block.addComponent(new HazardDamageComponent(15));
    }
    block.setScale(0.5f, 0.5f);
    block.create();
    return block;
  }

  private Body body(Entity entity) {
    return entity.getComponent(PhysicsComponent.class).getBody();
  }

  /** World bounding box of an entity's collider as {left, bottom, right, top}. */
  private float[] aabb(Entity entity) {
    Body body = body(entity);
    Fixture fixture = body.getFixtureList().first();
    PolygonShape shape = (PolygonShape) fixture.getShape();
    float left = Float.MAX_VALUE;
    float bottom = Float.MAX_VALUE;
    float right = -Float.MAX_VALUE;
    float top = -Float.MAX_VALUE;
    Vector2 vertex = new Vector2();
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, vertex);
      Vector2 world = body.getWorldPoint(vertex).cpy();
      left = Math.min(left, world.x);
      bottom = Math.min(bottom, world.y);
      right = Math.max(right, world.x);
      top = Math.max(top, world.y);
    }
    return new float[] {left, bottom, right, top};
  }

  private void stepFrames(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      entity.earlyUpdate();
      entity.update();
    }
  }
}
