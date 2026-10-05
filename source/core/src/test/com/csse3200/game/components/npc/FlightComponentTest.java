package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link FlightComponent}: on create it removes gravity from the entity's physics body and
 * sets the configured linear damping, so a hovering entity neither falls nor drifts away forever.
 *
 * <p>Fixture: frame time 0.02 s (mocked), so 50 frames is one second of simulation. The world's
 * gravity is the engine's own (-15). Entities have a physics component and a collider; the physics
 * component is always created first (it has high creation priority), so the body exists when the
 * flight component runs.
 */
@ExtendWith(GameExtension.class)
class FlightComponentTest {
  private static final float FRAME = 0.02f;

  @BeforeEach
  void setUp() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(FRAME);
    // The time source must exist before the physics service, which reads it when built.
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  // ---------- constructor ----------

  @Test
  void shouldStoreTheConfiguredDamping() {
    assertEquals(2.5f, new FlightComponent(2.5f).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldAcceptZeroDamping() {
    assertEquals(0f, new FlightComponent(0f).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldAcceptAVerySmallPositiveDamping() {
    assertDoesNotThrow(() -> new FlightComponent(0.0001f));
  }

  @ParameterizedTest(name = "damping {0} is rejected")
  @ValueSource(floats = {-0.0001f, -1f, -100f})
  void shouldRejectNegativeDamping(float damping) {
    assertThrows(IllegalArgumentException.class, () -> new FlightComponent(damping));
  }

  @Test
  void shouldRejectNotANumberDamping() {
    assertThrows(IllegalArgumentException.class, () -> new FlightComponent(Float.NaN));
  }

  @Test
  void shouldRejectInfiniteDamping() {
    assertThrows(
        IllegalArgumentException.class, () -> new FlightComponent(Float.POSITIVE_INFINITY));
    assertThrows(
        IllegalArgumentException.class, () -> new FlightComponent(Float.NEGATIVE_INFINITY));
  }

  // ---------- create: the body settings ----------

  @Test
  void shouldSwitchOffGravityOnTheBodyWhenCreated() {
    Entity entity = createEntity(new FlightComponent(2f), 0f, 100f);

    assertEquals(0f, body(entity).getGravityScale(), 1e-6f);
  }

  @Test
  void shouldApplyTheConfiguredDampingWhenCreated() {
    Entity entity = createEntity(new FlightComponent(3.5f), 0f, 100f);

    assertEquals(3.5f, body(entity).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldReplaceTheDefaultGroundFrictionDamping() {
    // The physics component starts every body with a heavy damping (ground friction).
    Entity plain = createEntity(null, 0f, 100f);
    float defaultDamping = body(plain).getLinearDamping();
    assertTrue(defaultDamping > 0f, "test assumes the default body damping is above zero");

    Entity flying = createEntity(new FlightComponent(0.5f), 0f, 100f);

    assertEquals(0.5f, body(flying).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldSetZeroDampingWhenConfiguredWithZero() {
    Entity entity = createEntity(new FlightComponent(0f), 0f, 100f);

    assertEquals(0f, body(entity).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldLeaveTheBodyDynamicAndActive() {
    Entity entity = createEntity(new FlightComponent(2f), 0f, 100f);

    assertEquals(BodyType.DynamicBody, body(entity).getType());
    assertTrue(body(entity).isActive());
  }

  @Test
  void shouldNotChangeWhereTheEntityStartsOrTouchItsRotationLock() {
    Entity entity = createEntity(new FlightComponent(2f), 4f, 50f);

    assertEquals(4f, body(entity).getPosition().x, 1e-4f);
    assertEquals(50f, body(entity).getPosition().y, 1e-4f);
    assertTrue(body(entity).isFixedRotation());
  }

  @Test
  void shouldWorkWhenAddedBeforeThePhysicsComponent() {
    // The physics component is created first whatever order components are added in, so the body
    // is ready when the flight component runs.
    Entity entity =
        new Entity()
            .addComponent(new FlightComponent(1.5f))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent());
    entity.create();

    assertEquals(0f, body(entity).getGravityScale(), 1e-6f);
    assertEquals(1.5f, body(entity).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldNotThrowWhenTheEntityHasNoPhysicsComponent() {
    Entity entity = new Entity().addComponent(new FlightComponent(2f));

    assertDoesNotThrow(entity::create);
  }

  @Test
  void shouldKeepTwoFlyingEntitiesIndependent() {
    Entity slow = createEntity(new FlightComponent(5f), 0f, 100f);
    Entity free = createEntity(new FlightComponent(0.5f), 10f, 100f);

    assertEquals(5f, body(slow).getLinearDamping(), 1e-6f);
    assertEquals(0.5f, body(free).getLinearDamping(), 1e-6f);
  }

  @Test
  void shouldNotAffectAnEntityThatHasNoFlightComponent() {
    Entity flying = createEntity(new FlightComponent(0f), 0f, 100f);
    Entity walking = createEntity(null, 10f, 100f);

    assertEquals(0f, body(flying).getGravityScale(), 1e-6f);
    assertEquals(1f, body(walking).getGravityScale(), 1e-6f, "other entities keep gravity");
  }

  // ---------- behaviour in a stepped world ----------

  @Test
  void shouldHoverInPlaceInsteadOfFalling() {
    Entity flying = createEntity(new FlightComponent(1f), 0f, 100f);
    step(flying, 1);
    float startY = body(flying).getPosition().y;

    step(flying, 100); // two seconds

    assertEquals(startY, body(flying).getPosition().y, 1e-3f, "a flying entity must not fall");
  }

  @Test
  void controlShouldFallWithoutAFlightComponent() {
    Entity walking = createEntity(null, 0f, 100f);
    step(walking, 1);
    float startY = body(walking).getPosition().y;

    step(walking, 100);

    assertTrue(
        body(walking).getPosition().y < startY - 1f,
        "control: an entity without flight falls under the world's gravity");
  }

  @Test
  void shouldLetKnockbackFadeWithDamping() {
    Entity flying = createEntity(new FlightComponent(2f), 0f, 100f);
    body(flying).setLinearVelocity(5f, 0f);

    step(flying, 50); // one second

    assertTrue(
        body(flying).getLinearVelocity().len() < 1.5f,
        "damping of 2 should bring 5 units/s well below 1.5 after a second");
  }

  @Test
  void controlShouldKeepDriftingWithZeroDamping() {
    Entity flying = createEntity(new FlightComponent(0f), 0f, 100f);
    body(flying).setLinearVelocity(5f, 0f);

    step(flying, 50);

    assertEquals(
        5f,
        body(flying).getLinearVelocity().len(),
        0.05f,
        "with no gravity and no damping nothing slows the entity");
  }

  @Test
  void shouldFadeKnockbackFasterWithMoreDamping() {
    Entity light = createEntity(new FlightComponent(0.5f), 0f, 100f);
    Entity heavy = createEntity(new FlightComponent(4f), 20f, 100f);
    body(light).setLinearVelocity(5f, 0f);
    body(heavy).setLinearVelocity(5f, 0f);

    step(light, 25);
    // both bodies live in the same world, so one stepping loop advances both
    float lightSpeed = body(light).getLinearVelocity().len();
    float heavySpeed = body(heavy).getLinearVelocity().len();

    assertTrue(heavySpeed < lightSpeed, "more damping must slow the entity more");
  }

  @Test
  void shouldKeepAnImpulseInTheDirectionItWasPushed() {
    Entity flying = createEntity(new FlightComponent(1f), 0f, 100f);
    step(flying, 1);
    float startX = body(flying).getPosition().x;
    float startY = body(flying).getPosition().y;

    body(flying)
        .applyLinearImpulse(
            2f, 3f, body(flying).getWorldCenter().x, body(flying).getWorldCenter().y, true);
    step(flying, 10);

    assertTrue(body(flying).getPosition().x > startX, "pushed right, so it moves right");
    assertTrue(
        body(flying).getPosition().y > startY, "pushed up, so it moves up and is not pulled down");
  }

  // ---------- helpers ----------

  private Entity createEntity(FlightComponent flight, float x, float y) {
    Entity entity =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new ColliderComponent());
    if (flight != null) {
      entity.addComponent(flight);
    }
    entity.create();
    entity.setPosition(x, y);
    return entity;
  }

  private Body body(Entity entity) {
    return entity.getComponent(PhysicsComponent.class).getBody();
  }

  private void step(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      entity.earlyUpdate();
      entity.update();
    }
  }
}
