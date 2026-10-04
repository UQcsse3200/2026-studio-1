package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for S3 aimed shots, using the pack's direction-based API:
 *
 * <ul>
 *   <li>{@code AimedLineMovementStrategy(float speed, Vector2 direction)} stores a normalised copy
 *       of the direction; {@code start(...)} gives the body a velocity of length {@code speed}
 *       along it and makes the body gravity-free, undamped and a bullet.
 *   <li>{@code ArrowFactory.createAimedArrow(position, direction, speed, maxRange, damage,
 *       knockback, targetLayer)}.
 *   <li>{@code RangedAttackComponent.setAimed(boolean)} / {@code isAimed()}, default false. When
 *       true, an ARROW flies along the line from the shooter's centre to the target's centre.
 * </ul>
 */
@ExtendWith(GameExtension.class)
class AimedArrowTest {
  private static final float SPEED = 8f;
  private static final float TOLERANCE = 1e-3f;
  private final List<Entity> world = new ArrayList<>();

  @BeforeEach
  void beforeEach() {
    world.clear();
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
  }

  // ---------- strategy: velocity ----------

  @ParameterizedTest(name = "direction ({0},{1})")
  @CsvSource({
    "4, -3", // down and right
    "1, 1", // up and right at 45 degrees
    "-5, -3", // down and left
    "-5, 3", // up and left
    "9, 0", // straight right
    "-9, 0", // straight left
    "0, 7", // straight up
    "0, -7" // straight down
  })
  void velocityPointsAlongTheDirectionAtTheGivenSpeed(float dx, float dy) {
    Entity arrow = createBareArrowBody();
    Vector2 expected = new Vector2(dx, dy).nor().scl(SPEED);

    new AimedLineMovementStrategy(SPEED, new Vector2(dx, dy)).start(arrow);

    Vector2 actual = velocityOf(arrow);
    assertEquals(expected.x, actual.x, TOLERANCE, "x component of the velocity is wrong");
    assertEquals(expected.y, actual.y, TOLERANCE, "y component of the velocity is wrong");
  }

  @ParameterizedTest(name = "speed {0} along ({1},{2})")
  @CsvSource({"8, 1, 1", "8, 5, -3", "3, -4, 2", "12, 0.1, 9", "8, 100, 100"})
  void speedIsConstantWhateverTheAngleOrLength(float speed, float dx, float dy) {
    Entity arrow = createBareArrowBody();

    new AimedLineMovementStrategy(speed, new Vector2(dx, dy)).start(arrow);

    assertEquals(speed, velocityOf(arrow).len(), TOLERANCE, "Diagonal shots must not be faster.");
  }

  @Test
  void startMakesTheBodyFlyLikeABullet() {
    Entity arrow = createBareArrowBody();
    arrow.getComponent(PhysicsComponent.class).getBody().setLinearDamping(5f);

    new AimedLineMovementStrategy(SPEED, new Vector2(1f, 0f)).start(arrow);

    assertEquals(0f, arrow.getComponent(PhysicsComponent.class).getBody().getGravityScale());
    assertEquals(0f, arrow.getComponent(PhysicsComponent.class).getBody().getLinearDamping());
    assertTrue(arrow.getComponent(PhysicsComponent.class).getBody().isBullet());
  }

  @Test
  void updateDoesNotChangeTheVelocityItSetAtStart() {
    Entity arrow = createBareArrowBody();
    AimedLineMovementStrategy strategy = new AimedLineMovementStrategy(SPEED, new Vector2(4f, 2f));
    strategy.start(arrow);
    Vector2 before = velocityOf(arrow);

    for (int i = 0; i < 30; i++) {
      strategy.update(arrow, 0.02f);
    }

    assertEquals(before.x, velocityOf(arrow).x, TOLERANCE);
    assertEquals(before.y, velocityOf(arrow).y, TOLERANCE);
  }

  // ---------- strategy: storing the direction ----------

  @Test
  void directionIsStoredNormalised() {
    AimedLineMovementStrategy strategy = new AimedLineMovementStrategy(SPEED, new Vector2(3f, 4f));

    assertEquals(0.6f, strategy.getDirection().x, TOLERANCE);
    assertEquals(0.8f, strategy.getDirection().y, TOLERANCE);
    assertEquals(SPEED, strategy.getSpeed());
  }

  @Test
  void aTinyButNonZeroDirectionIsAccepted() {
    assertDoesNotThrow(() -> new AimedLineMovementStrategy(SPEED, new Vector2(0.0001f, 0f)));
  }

  @Test
  void constructorCopiesTheDirectionSoLaterChangesDoNotRedirectTheShot() {
    Vector2 direction = new Vector2(5f, 0f);
    AimedLineMovementStrategy strategy = new AimedLineMovementStrategy(SPEED, direction);
    direction.set(0f, 50f);

    assertEquals(1f, strategy.getDirection().x, TOLERANCE);
    assertEquals(0f, strategy.getDirection().y, TOLERANCE);
  }

  @Test
  void getDirectionReturnsACopy() {
    AimedLineMovementStrategy strategy = new AimedLineMovementStrategy(SPEED, new Vector2(1f, 0f));

    strategy.getDirection().set(0f, 9f);

    assertEquals(1f, strategy.getDirection().x, TOLERANCE);
  }

  // ---------- strategy: invalid input ----------

  @Test
  void strategyRejectsNonPositiveOrNonFiniteSpeed() {
    Vector2 direction = new Vector2(1f, 1f);
    assertThrows(
        IllegalArgumentException.class, () -> new AimedLineMovementStrategy(0f, direction));
    assertThrows(
        IllegalArgumentException.class, () -> new AimedLineMovementStrategy(-1f, direction));
    assertThrows(
        IllegalArgumentException.class, () -> new AimedLineMovementStrategy(Float.NaN, direction));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AimedLineMovementStrategy(Float.POSITIVE_INFINITY, direction));
  }

  @Test
  void strategyRejectsANullDirection() {
    assertThrows(IllegalArgumentException.class, () -> new AimedLineMovementStrategy(SPEED, null));
  }

  @Test
  void strategyRejectsAZeroDirection() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AimedLineMovementStrategy(SPEED, new Vector2(0f, 0f)));
  }

  @Test
  void strategyRejectsNonFiniteDirections() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AimedLineMovementStrategy(SPEED, new Vector2(Float.NaN, 1f)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AimedLineMovementStrategy(SPEED, new Vector2(Float.POSITIVE_INFINITY, 0f)));
  }

  // ---------- factory ----------

  @Test
  void factoryRejectsNullPositionAndBadDirections() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createAimedArrow(
                null, new Vector2(1, 1), SPEED, 10f, 4, 0f, PhysicsLayer.PLAYER));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createAimedArrow(
                new Vector2(0, 0), null, SPEED, 10f, 4, 0f, PhysicsLayer.PLAYER));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createAimedArrow(
                new Vector2(0, 0), new Vector2(0, 0), SPEED, 10f, 4, 0f, PhysicsLayer.PLAYER));
  }

  @Test
  void factoryRejectsNonPositiveSpeedAndRange() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createAimedArrow(
                new Vector2(0, 0), new Vector2(1, 1), 0f, 10f, 4, 0f, PhysicsLayer.PLAYER));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createAimedArrow(
                new Vector2(0, 0), new Vector2(1, 1), SPEED, 0f, 4, 0f, PhysicsLayer.PLAYER));
  }

  @Test
  void factoryBuildsAnArrowThatCarriesItsDamage() {
    Entity arrow =
        ArrowFactory.createAimedArrow(
            new Vector2(0, 5), new Vector2(4, -5), SPEED, 10f, 4, 0f, PhysicsLayer.PLAYER);
    register(arrow);

    assertEquals(4, arrow.getComponent(CombatStatsComponent.class).getBaseAttack());
    assertNotNull(arrow.getComponent(PhysicsComponent.class));
    assertNotNull(arrow.getComponent(ProjectileComponent.class));
  }

  @Test
  void factoryGivesTheArrowTheDirectionalVelocityOnceCreated() {
    Entity arrow =
        ArrowFactory.createAimedArrow(
            new Vector2(0, 5), new Vector2(3, -4), SPEED, 10f, 4, 0f, PhysicsLayer.PLAYER);
    register(arrow);

    Vector2 velocity = velocityOf(arrow);
    assertEquals(4.8f, velocity.x, TOLERANCE);
    assertEquals(-6.4f, velocity.y, TOLERANCE);
  }

  // ---------- real physics: the reason aimed shots exist ----------

  @Test
  void anAimedArrowFromAboveHitsATargetBelow() {
    Entity target = createDynamicTarget(4f, 0f, 20);
    Vector2 start = new Vector2(0f, 4f);
    Vector2 direction = target.getCenterPosition().sub(start.cpy().add(0.25f, 0.1f));
    Entity arrow =
        ArrowFactory.createAimedArrow(start, direction, SPEED, 12f, 4, 0f, PhysicsLayer.PLAYER);
    register(arrow);

    stepWorld(120);

    assertEquals(16, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void aStraightArrowFromTheSameHeightMissesTheSameTarget() {
    Entity target = createDynamicTarget(4f, 0f, 20);
    Entity arrow =
        ArrowFactory.createRangedArrow(
            new Vector2(0f, 4f), true, SPEED, 12f, 4, 0f, PhysicsLayer.PLAYER);
    register(arrow);

    stepWorld(120);

    assertEquals(20, target.getComponent(CombatStatsComponent.class).getHealth(), "Control case.");
  }

  @Test
  void anAimedArrowAtATargetBehindItsShooterFliesBackwards() {
    Entity target = createDynamicTarget(-4f, 0f, 20);
    Vector2 start = new Vector2(4f, 0f);
    Vector2 direction = target.getCenterPosition().sub(start.cpy().add(0.25f, 0.1f));
    Entity arrow =
        ArrowFactory.createAimedArrow(start, direction, SPEED, 14f, 4, 0f, PhysicsLayer.PLAYER);
    register(arrow);

    stepWorld(150);

    assertEquals(16, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  // ---------- RangedAttackComponent flag ----------

  @Test
  void aimedFlagDefaultsToFalse() {
    assertFalse(new RangedAttackComponent(6f, 2f, 0f, bow()).isAimed());
  }

  @Test
  void aimedFlagCanBeToggled() {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2f, 0f, bow());
    ranged.setAimed(true);
    assertTrue(ranged.isAimed());
    ranged.setAimed(false);
    assertFalse(ranged.isAimed());
  }

  @Test
  void aimedAttackerFiresOnceAtATargetBelowIt() {
    Entity attacker = createAimedAttacker(true);
    Entity target = createTarget(3f, -3f);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, fired.size());
  }

  @Test
  void aimedAttackerDirectlyAboveTheTargetDoesNotCrash() {
    Entity attacker = createAimedAttacker(true);
    Entity target = createTarget(0f, -3f);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW));
  }

  @Test
  void aimedAttackerOnTopOfItsTargetFallsBackToTheRightAndDoesNotCrash() {
    Entity attacker = createAimedAttacker(true);
    Entity target = createTarget(0f, 0f);
    List<Entity> fired = listenForFired(attacker);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW));
    assertEquals(1, fired.size());
  }

  @Test
  void aimedFlagDoesNotChangeLightningBehaviour() {
    Entity attacker = createAimedAttacker(true);
    Entity target = createTarget(3f, -3f);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);

    assertEquals(1, fired.size(), "Lightning keeps its own spawn and still fires once.");
  }

  @Test
  void unaimedAttackerStillFiresAsBefore() {
    Entity attacker = createAimedAttacker(false);
    Entity target = createTarget(3f, 0f);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, fired.size());
  }

  @Test
  void aimedAttackerStillRespectsRange() {
    Entity attacker = createAimedAttacker(true);
    Entity target = createTarget(30f, -30f);
    List<Entity> fired = listenForFired(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(0, fired.size());
  }

  // ---------- helpers ----------

  private WeaponItem bow() {
    return new WeaponItem("Test Bow", WeaponType.BOW, WeaponTier.TIER_1, 1, 1, 0f);
  }

  private Entity createAimedAttacker(boolean aimed) {
    RangedAttackComponent ranged = new RangedAttackComponent(6f, 2f, 0f, bow());
    ranged.setAimed(aimed);
    Entity attacker =
        new Entity().addComponent(ranged).addComponent(new CombatStatsComponent(20, 5));
    attacker.create();
    attacker.setPosition(0f, 0f);
    return attacker;
  }

  private Entity createTarget(float x, float y) {
    Entity target = new Entity().addComponent(new CombatStatsComponent(10, 0));
    target.create();
    target.setPosition(x, y);
    return target;
  }

  private List<Entity> listenForFired(Entity attacker) {
    List<Entity> fired = new ArrayList<>();
    attacker.getEvents().addListener("rangedAttackFired", (Entity e) -> fired.add(e));
    return fired;
  }

  private Entity createBareArrowBody() {
    Entity arrow =
        new Entity().addComponent(new PhysicsComponent().setBodyType(BodyType.DynamicBody));
    arrow.create();
    arrow.setScale(0.5f, 0.2f);
    arrow.setPosition(0f, 0f);
    return arrow;
  }

  private Vector2 velocityOf(Entity entity) {
    return entity.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().cpy();
  }

  private Entity createDynamicTarget(float x, float y, int health) {
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(health, 0));
    register(target);
    target.setPosition(x, y);
    target.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return target;
  }

  private void register(Entity entity) {
    ServiceLocator.getEntityService().register(entity);
    world.add(entity);
  }

  private void stepWorld(int steps) {
    for (int i = 0; i < steps; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      for (Entity entity : new ArrayList<>(world)) {
        entity.earlyUpdate();
        entity.update();
      }
    }
  }
}
