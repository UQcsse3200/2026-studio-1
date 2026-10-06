package com.csse3200.game.components.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
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

/**
 * PC-1: real physics contact tests for enemy arrows built by {@link ArrowFactory}.
 *
 * <p>The existing {@code ProjectileHitComponentTest} fires the {@code "collisionStart"} event by
 * hand, so it can never notice that Box2D never produces the contact in the first place. These
 * tests step a real {@link PhysicsService} world, so the contact has to come from the engine.
 *
 * <p>Suspected cause for a failing block test: the enemy arrow is a {@code KinematicBody}, and
 * Box2D only creates contacts when at least one of the two bodies is dynamic. A kinematic arrow
 * against a static wall therefore never fires {@code "collisionStart"}, so the arrow flies through
 * walls. Hits on the player should still pass because the player body is dynamic. Run this class
 * BEFORE changing anything and record which tests fail; that result decides the fix.
 *
 * <p>Layout: the arrow starts at x = 0 and travels along x at 8 units/second. Targets are 1 x 1 at
 * y = 0, so the 0.2 high arrow overlaps them vertically. The world has gravity, so every dynamic
 * test body has its gravity scale set to zero to keep it in place.
 */
@ExtendWith(GameExtension.class)
class ProjectileRealContactTest {
  private static final float SPEED = 8f;
  private static final float MAX_RANGE = 10f;
  private static final int DAMAGE = 4;
  private static final int MAX_STEPS = 120;

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

  // ---------- hits ----------

  @Test
  void arrowDamagesAPlayerLayerTargetExactlyOnce() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity player = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);
    List<Entity> hits = listenForHits(arrow);

    stepUntilExpired(arrow);

    assertEquals(1, hits.size(), "The arrow should report exactly one hit.");
    assertEquals(
        20 - DAMAGE,
        player.getComponent(CombatStatsComponent.class).getHealth(),
        "Damage must equal the damage the arrow was built with.");
  }

  @Test
  void arrowFiredLeftwardAlsoHitsThePlayer() {
    Entity arrow = fireArrow(8f, false, 0f);
    Entity player = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    assertEquals(20 - DAMAGE, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void arrowAppliesKnockbackAwayFromTheShooter() {
    Entity arrow = fireArrow(0f, true, 4f);
    Entity player = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    float velocityX = player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    assertTrue(velocityX > 0f, "Knockback should push the target in the direction of travel.");
  }

  @Test
  void arrowAppliesNoKnockbackWhenKnockbackIsZero() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity player = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    assertEquals(
        0f,
        player.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().len(),
        1e-4f,
        "Zero knockback must leave the target's velocity unchanged.");
  }

  @Test
  void arrowStopsAtTheFirstTargetAndNeverHitsASecondOneBehindIt() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity first = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);
    Entity second = createDynamicTarget(5f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    assertEquals(20 - DAMAGE, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(
        20,
        second.getComponent(CombatStatsComponent.class).getHealth(),
        "A resolved arrow must not land a second hit.");
  }

  // ---------- blocking ----------

  @Test
  void aWallBlocksTheArrowSoTheTargetBehindItIsNotHurt() {
    Entity arrow = fireArrow(0f, true, 0f);
    createStaticWall(2f);
    Entity player = createDynamicTarget(4f, PhysicsLayer.PLAYER, 20);
    List<Object> expired = listenForExpiry(arrow);

    stepUntilExpired(arrow);

    assertEquals(
        20,
        player.getComponent(CombatStatsComponent.class).getHealth(),
        "The arrow went through a wall and hurt the player behind it.");
    assertEquals(1, expired.size(), "Hitting the wall should end the arrow's flight once.");
  }

  @Test
  void aWallEndsTheFlightEarlierThanTheMaximumRange() {
    Entity arrow = fireArrow(0f, true, 0f);
    createStaticWall(2f);

    int steps = stepUntilExpired(arrow);

    assertTrue(
        steps < MAX_STEPS / 2,
        "The arrow should stop at the wall (about 0.3 s), not fly its full range. Steps: " + steps);
  }

  // ---------- misses and ignored layers ----------

  @Test
  void arrowPassesThroughAnEntityOnAnUnrelatedLayer() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity npc = createDynamicTarget(3f, PhysicsLayer.NPC, 20);
    List<Entity> hits = listenForHits(arrow);

    stepUntilExpired(arrow);

    assertEquals(0, hits.size());
    assertEquals(20, npc.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void arrowAboveTheTargetMissesAndDespawnsAtMaximumRange() {
    Entity arrow = fireArrow(0f, true, 0f);
    arrow.setPosition(0f, 5f);
    Entity player = createDynamicTarget(3f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    assertEquals(20, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(
        arrow.getComponent(ProjectileComponent.class).isExpired(),
        "A missed arrow should still despawn once it reaches its maximum range.");
  }

  @Test
  void arrowDoesNotHitAnEntityThatIsOutOfRangeBehindIt() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity behind = createDynamicTarget(-4f, PhysicsLayer.PLAYER, 20);

    stepUntilExpired(arrow);

    assertEquals(20, behind.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void targetWithoutCombatStatsDoesNotCrashAndStillEndsTheFlight() {
    Entity arrow = fireArrow(0f, true, 0f);
    Entity statless =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    register(statless);
    statless.setPosition(3f, 0f);
    statless.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    List<Entity> hits = listenForHits(arrow);
    List<Object> expired = listenForExpiry(arrow);

    stepUntilExpired(arrow); // must complete without throwing

    assertEquals(0, hits.size(), "Nothing can be damaged, so no hit may be reported.");
    assertEquals(1, expired.size(), "The arrow is still consumed by the contact.");
  }

  // ---------- helpers ----------

  private Entity fireArrow(float x, boolean movingRight, float knockback) {
    Entity arrow =
        ArrowFactory.createRangedArrow(
            new com.badlogic.gdx.math.Vector2(x, 0f),
            movingRight,
            SPEED,
            MAX_RANGE,
            DAMAGE,
            knockback,
            PhysicsLayer.PLAYER);
    register(arrow);
    return arrow;
  }

  private Entity createDynamicTarget(float x, short layer, int health) {
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(layer))
            .addComponent(new CombatStatsComponent(health, 0));
    register(target);
    target.setPosition(x, 0f);
    target.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return target;
  }

  private Entity createStaticWall(float x) {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    register(wall);
    wall.setPosition(x, 0f);
    return wall;
  }

  private void register(Entity entity) {
    ServiceLocator.getEntityService().register(entity);
    world.add(entity);
  }

  /** Steps the real physics world and every entity until the arrow expires; returns steps used. */
  private int stepUntilExpired(Entity arrow) {
    ProjectileComponent projectile = arrow.getComponent(ProjectileComponent.class);
    List<Object> expired = listenForExpiry(arrow);
    int steps = 0;
    while (steps < MAX_STEPS && expired.isEmpty() && !projectile.isExpired()) {
      stepOnce();
      steps++;
    }
    // a few extra steps so a second (unwanted) contact would have the chance to show up
    for (int i = 0; i < 5; i++) {
      stepOnce();
    }
    return steps;
  }

  private void stepOnce() {
    ServiceLocator.getPhysicsService().getPhysics().update();
    for (Entity entity : new ArrayList<>(world)) {
      entity.earlyUpdate();
      entity.update();
    }
  }

  private List<Entity> listenForHits(Entity arrow) {
    List<Entity> hits = new ArrayList<>();
    arrow.getEvents().addListener("projectileHit", (Entity e) -> hits.add(e));
    return hits;
  }

  private List<Object> listenForExpiry(Entity arrow) {
    List<Object> expired = new ArrayList<>();
    arrow.getEvents().addListener("projectileExpired", () -> expired.add(new Object()));
    return expired;
  }
}
