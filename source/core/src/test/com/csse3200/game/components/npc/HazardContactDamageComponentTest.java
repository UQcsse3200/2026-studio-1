package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.HazardDamageComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link HazardContactDamageComponent} against the real physics world, so the contact events
 * come from Box2D exactly as they do in the game.
 *
 * <p>Fixture: a hazard is a static sensor on the hazard layer carrying a {@link
 * HazardDamageComponent}; an enemy is a dynamic body with its gravity switched off so it stays
 * where it is put. The game clock is a mock the test advances by hand, so the cooldown is exact.
 * One frame is 0.02 s of physics.
 */
@ExtendWith(GameExtension.class)
class HazardContactDamageComponentTest {
  private static final float FRAME = 0.02f;
  private static final Vector2 FAR_AWAY = new Vector2(50f, 50f);

  private final AtomicLong clock = new AtomicLong(1_000L);

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(FRAME);
    when(time.getTime()).thenAnswer(invocation -> clock.get());
    // The time source must exist before the physics service, which reads it when built.
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  // ---------- constructor ----------

  @Test
  void shouldUseTheSameCooldownAsThePlayerByDefault() {
    assertEquals(500L, new HazardContactDamageComponent().getCooldownMillis());
    assertEquals(500L, HazardContactDamageComponent.DEFAULT_COOLDOWN_MILLIS);
  }

  @Test
  void shouldStoreACustomCooldown() {
    assertEquals(200L, new HazardContactDamageComponent(200L).getCooldownMillis());
  }

  @ParameterizedTest(name = "cooldown {0} is rejected")
  @ValueSource(longs = {0L, -1L, -500L})
  void shouldRejectAZeroOrNegativeCooldown(long cooldown) {
    assertThrows(IllegalArgumentException.class, () -> new HazardContactDamageComponent(cooldown));
  }

  @Test
  void shouldUseTheLevelDefaultDamageConstant() {
    assertEquals(10, HazardContactDamageComponent.DEFAULT_HAZARD_DAMAGE);
  }

  // ---------- first contact ----------

  @Test
  void shouldHurtAnEnemyStandingInAHazardByTheHazardsOwnDamage() {
    Entity hazard = createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 2);

    assertEquals(85, health(enemy));
    assertTrue(enemy.getComponent(HazardContactDamageComponent.class).isTouchingHazard());
    assertTrue(hazard.getComponent(HazardDamageComponent.class).getDamage() == 15);
  }

  @Test
  void shouldUseTheDefaultDamageWhenTheHazardCarriesNone() {
    createHazardWithoutDamage(0f, 0f);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 2);

    assertEquals(100 - HazardContactDamageComponent.DEFAULT_HAZARD_DAMAGE, health(enemy));
  }

  @Test
  void shouldNotHurtAnEnemyThatIsNotTouchingAHazard() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(FAR_AWAY.x, FAR_AWAY.y, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 10);

    assertEquals(100, health(enemy));
    assertFalse(enemy.getComponent(HazardContactDamageComponent.class).isTouchingHazard());
  }

  @Test
  void shouldIgnoreASensorThatIsNotOnTheHazardLayer() {
    // Same damage component, but on the item layer: only the hazard layer counts.
    Entity notAHazard = createSensor(0f, 0f, PhysicsLayer.ITEM, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 5);

    assertEquals(100, health(enemy));
    assertFalse(enemy.getComponent(HazardContactDamageComponent.class).isTouchingHazard());
    assertTrue(notAHazard.getComponent(HazardDamageComponent.class) != null);
  }

  @Test
  void shouldAnnounceTheDamageItDealt() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    List<Integer> announced = new ArrayList<>();
    enemy.getEvents().addListener("hazardDamage", (EventListener1<Integer>) announced::add);

    stepFrames(enemy, 2);

    assertEquals(List.of(15), announced);
  }

  // ---------- cooldown ----------

  @Test
  void shouldNotHurtAgainBeforeTheCooldownHasPassed() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    stepFrames(enemy, 2);

    clock.addAndGet(499L);
    stepFrames(enemy, 5);

    assertEquals(85, health(enemy), "still inside the cooldown");
  }

  @Test
  void shouldHurtAgainOnceTheCooldownHasPassedWhileStillInside() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    stepFrames(enemy, 2);

    clock.addAndGet(500L);
    stepFrames(enemy, 2);

    assertEquals(70, health(enemy));
  }

  @Test
  void shouldHurtOnlyOncePerCooldownWindowNoMatterHowManyFramesPass() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 60); // 1.2 s of physics but the clock has not moved

    assertEquals(85, health(enemy));
  }

  @Test
  void shouldRespectACustomCooldown() {
    createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent(200L));
    stepFrames(enemy, 2);

    clock.addAndGet(200L);
    stepFrames(enemy, 2);

    assertEquals(70, health(enemy));
  }

  @Test
  void shouldNotResetTheCooldownByLeavingAndReEntering() {
    Entity hazard = createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    stepFrames(enemy, 2);
    assertEquals(85, health(enemy));

    teleport(enemy, FAR_AWAY);
    stepFrames(enemy, 2);
    assertFalse(enemy.getComponent(HazardContactDamageComponent.class).isTouchingHazard());
    teleport(enemy, hazard.getPosition());
    clock.addAndGet(100L);
    stepFrames(enemy, 2);

    assertEquals(85, health(enemy), "100 ms is inside the cooldown, so re-entering cannot hurt");
  }

  @Test
  void shouldHurtOnReEntryOnceTheCooldownHasPassed() {
    Entity hazard = createHazard(0f, 0f, 15);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    stepFrames(enemy, 2);

    teleport(enemy, FAR_AWAY);
    stepFrames(enemy, 2);
    clock.addAndGet(600L);
    teleport(enemy, hazard.getPosition());
    stepFrames(enemy, 2);

    assertEquals(70, health(enemy));
  }

  // ---------- several hazards, health limits, missing parts ----------

  @Test
  void shouldApplyOnlyTheLargestDamageWhenTouchingTwoHazards() {
    createHazard(0f, 0f, 10);
    createHazard(0f, 0f, 25);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    stepFrames(enemy, 2);

    assertEquals(75, health(enemy), "25 once, not 10 plus 25");
  }

  @Test
  void shouldNeverTakeTheHealthBelowZeroAndShouldStopHurtingTheDead() {
    createHazard(0f, 0f, 500);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());
    List<Integer> announced = new ArrayList<>();
    enemy.getEvents().addListener("hazardDamage", (EventListener1<Integer>) announced::add);

    stepFrames(enemy, 2);
    clock.addAndGet(1_000L);
    stepFrames(enemy, 2);

    assertTrue(health(enemy) <= 0);
    assertEquals(1, announced.size(), "a dead enemy is not hurt again");
  }

  @Test
  void shouldDoNothingWhenTheHazardDealsZero() {
    createHazard(0f, 0f, 0);
    Entity enemy = createEnemy(0f, 0f, 100, new HazardContactDamageComponent());

    assertDoesNotThrow(() -> stepFrames(enemy, 5));

    assertEquals(100, health(enemy));
  }

  @Test
  void shouldNotThrowForAnEntityWithoutCombatStats() {
    createHazard(0f, 0f, 15);
    Entity noStats =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HazardContactDamageComponent());
    noStats.create();
    noStats.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    noStats.setPosition(0f, 0f);

    assertDoesNotThrow(() -> stepFrames(noStats, 5));
  }

  @Test
  void shouldHurtAnEnemyThatIsHoveringJustLikeAWalkingOne() {
    // A flyer has gravity off and no movement controller; touching the tile is all that matters.
    createHazard(0f, 0f, 15);
    Entity flyer = createEnemy(0f, 0f, 40, new HazardContactDamageComponent());

    stepFrames(flyer, 2);

    assertEquals(25, health(flyer));
  }

  // ---------- helpers ----------

  private Entity createHazard(float x, float y, int damage) {
    Entity hazard = createSensor(x, y, PhysicsLayer.HAZARD, damage);
    return hazard;
  }

  private Entity createHazardWithoutDamage(float x, float y) {
    Entity hazard =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.HAZARD).setSensor(true));
    hazard.setScale(0.5f, 0.5f);
    hazard.create();
    hazard.setPosition(x, y);
    return hazard;
  }

  private Entity createSensor(float x, float y, short layer, int damage) {
    Entity sensor =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(layer).setSensor(true))
            .addComponent(new HazardDamageComponent(damage));
    sensor.setScale(0.5f, 0.5f);
    sensor.create();
    sensor.setPosition(x, y);
    return sensor;
  }

  private Entity createEnemy(
      float x, float y, int health, HazardContactDamageComponent hazardRule) {
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(hazardRule);
    enemy.setScale(0.5f, 0.5f);
    enemy.create();
    enemy.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    enemy.setPosition(x, y);
    return enemy;
  }

  private void teleport(Entity entity, Vector2 where) {
    entity.setPosition(where.x, where.y);
  }

  private int health(Entity entity) {
    return entity.getComponent(CombatStatsComponent.class).getHealth();
  }

  private void stepFrames(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      entity.earlyUpdate();
      entity.update();
    }
  }
}
