package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.entities.factories.PetProjectileFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises real Box2D flight and contacts, with disposal callbacks run on the test thread. */
@ExtendWith(GameExtension.class)
class PetProjectileComponentTest {
  private Application previousApp;
  private PhysicsEngine physics;
  private EntityService entities;
  private RenderService renderer;
  private Entity owner;
  private Entity pet;
  private final Queue<Runnable> scheduled = new ArrayDeque<>();

  @BeforeEach
  void setUp() {
    previousApp = Gdx.app;
    Gdx.app = mock(Application.class);
    doAnswer(
            invocation -> {
              scheduled.add(invocation.getArgument(0));
              return null;
            })
        .when(Gdx.app)
        .postRunnable(any(Runnable.class));

    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.016f);
    ServiceLocator.registerTimeSource(time);
    entities = new EntityService();
    ServiceLocator.registerEntityService(entities);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    physics = ServiceLocator.getPhysicsService().getPhysics();
    renderer = mock(RenderService.class);
    ServiceLocator.registerRenderService(renderer);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/items/arrow.png", Texture.class))
        .thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);

    PetManagerComponent manager =
        new PetManagerComponent(
            (petOwner, petData) ->
                new Entity()
                    .addComponent(new PetComponent(petOwner))
                    .addComponent(new PetCombatComponent())
                    .addComponent(new PetProjectileSpawnerComponent()));
    owner = new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(manager);
    entities.register(owner);
    manager.activatePet(new ShopComponent.Pet("Bird"));
    pet = manager.getActivePet();
    pet.setPosition(0f, 3f);
  }

  @AfterEach
  void tearDown() {
    try {
      entities.dispose();
      runScheduled();
      physics.dispose();
    } finally {
      Gdx.app = previousApp;
    }
  }

  @Test
  void shouldLaunchFromPetTowardsEnemyAndDamageOnceAcrossMultipleFixtures() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    List<Boolean> lockedDuringDamage = new ArrayList<>();
    enemy
        .getEvents()
        .addListener(
            "updateHealth",
            (Integer health) -> lockedDuringDamage.add(physics.getWorld().isLocked()));
    Entity projectile = fireAt(enemy);
    Body body = projectile.getComponent(PhysicsComponent.class).getBody();
    Vector2 direction = enemy.getCenterPosition().sub(pet.getCenterPosition()).nor();

    assertTrue(projectile.getCenterPosition().epsilonEquals(pet.getCenterPosition(), 0.001f));
    assertTrue(body.getLinearVelocity().epsilonEquals(direction.scl(8f), 0.001f));
    assertEquals(0f, body.getGravityScale());
    assertEquals(0f, body.getLinearDamping());
    assertEquals(100, health(enemy)); // Spawning alone must not apply damage.

    for (int i = 0; i < 100 && health(enemy) == 100; i++) {
      physics.update();
    }
    assertEquals(95, health(enemy));
    assertEquals(List.of(true), lockedDuringDamage);
    assertFalse(projectile.isDisposed());
    assertEquals(2, physics.getWorld().getBodyCount());
    assertEquals(1, scheduled.size());

    runScheduled();
    assertTrue(projectile.isDisposed());
    assertEquals(1, physics.getWorld().getBodyCount());
    verify(renderer).unregister(projectile.getComponent(TextureRenderComponent.class));
  }

  @Test
  void shouldCreateAssistAfterPlayerArrowCollisionHasFinishedWithoutChainingMoreAssists() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    List<Entity> playerHits = new ArrayList<>();
    owner
        .getEvents()
        .addListener(
            "playerAttackHit",
            (Entity target) -> {
              playerHits.add(target);
              assertTrue(physics.getWorld().isLocked());
              assertNull(findPetProjectile());
            });
    entities.register(ArrowFactory.createArrow(owner.getCenterPosition(), Vector2.X, 10, owner));

    for (int i = 0; i < 100 && playerHits.isEmpty(); i++) {
      physics.update();
    }
    assertEquals(List.of(enemy), playerHits);
    assertEquals(90, health(enemy));
    assertNull(findPetProjectile());

    entities.update();
    Entity assist = findPetProjectile();
    assertNotNull(assist);
    runScheduled();
    flyUntilDisposed(assist);
    assertEquals(85, health(enemy));

    for (int i = 0; i < 100; i++) {
      step();
    }
    assertEquals(List.of(enemy), playerHits);
    assertNull(findPetProjectile());
    assertEquals(1, physics.getWorld().getBodyCount());
  }

  @Test
  void shouldFireFromEachPetCreatedByTheGameFactory() {
    PetManagerComponent manager = new PetManagerComponent();
    Entity factoryOwner =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(manager);
    entities.register(factoryOwner);

    for (String type : List.of("Bird", "Bat", "Spirit")) {
      Entity enemy = createEnemy(4f, 0f, 100, null);
      manager.activatePet(new ShopComponent.Pet(type));
      factoryOwner.getEvents().trigger("playerAttackHit", enemy);
      assertNull(findPetProjectile());

      entities.update();
      Entity projectile = findPetProjectile();
      assertNotNull(projectile, type + " should create an assist projectile");
      flyUntilDisposed(projectile);
      assertEquals(95, health(enemy));
      enemy.dispose();
    }
  }

  @Test
  void shouldPassOtherEnemiesAndOnlyDamageTheConfirmedTarget() {
    Entity otherEnemy = createEnemy(2f, 3f, 100, null);
    Entity target = createEnemy(5f, 3f, 100, null);
    Entity projectile = fireAt(target);

    flyUntilDisposed(projectile);

    assertEquals(100, health(otherEnemy));
    assertEquals(95, health(target));
  }

  @Test
  void shouldBeBlockedByStaticTerrainBeforeReachingTarget() {
    Entity target = createEnemy(5f, 3f, 100, null);
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    wall.setPosition(2f, 3f);
    entities.register(wall);
    Entity projectile = fireAt(target);

    flyUntilDisposed(projectile);

    assertEquals(100, health(target));
    assertEquals(2, physics.getWorld().getBodyCount());
  }

  @Test
  void shouldRespectShieldAndConsumeOnlyOneShieldHit() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    enemy.getComponent(CombatStatsComponent.class).setShieldHits(2);
    Entity projectile = fireAt(enemy);

    flyUntilDisposed(projectile);

    assertEquals(100, health(enemy));
    assertEquals(1, enemy.getComponent(CombatStatsComponent.class).getShieldHits());
  }

  @Test
  void shouldCreditPlayerAndReuseEnemyDeathAndLootOnce() {
    ItemDropComponent dropper = mock(ItemDropComponent.class);
    when(dropper.dropFirstStack()).thenReturn(true, false);
    Entity enemy = createEnemy(4f, 0f, 5, dropper);
    AtomicInteger kills = new AtomicInteger();
    owner.getEvents().addListener("enemyKilled", kills::incrementAndGet);
    List<Entity> playerHits = new ArrayList<>();
    owner.getEvents().addListener("playerAttackHit", (Entity target) -> playerHits.add(target));
    Entity projectile = fireAt(enemy);

    flyUntilDisposed(projectile);

    assertEquals(0, health(enemy));
    assertTrue(enemy.isDisposed());
    assertEquals(1, kills.get());
    assertTrue(playerHits.isEmpty());
    verify(dropper).dropGold();
    verify(dropper, times(2)).dropFirstStack();
    assertEquals(0, physics.getWorld().getBodyCount());
  }

  @Test
  void shouldCleanUpMissedShotWhenLifetimeEnds() {
    Entity enemy = createEnemy(5f, 3f, 100, null);
    Entity projectile = fireAt(enemy);
    enemy.setPosition(5f, 9f);

    flyUntilDisposed(projectile);

    assertEquals(100, health(enemy));
    assertEquals(1, physics.getWorld().getBodyCount());
    verify(renderer).unregister(projectile.getComponent(TextureRenderComponent.class));
  }

  @Test
  void shouldCancelFlightWhenTargetDiesOrIsRemoved() {
    Entity deadTarget = createEnemy(4f, 0f, 100, null);
    Entity first = fireAt(deadTarget);
    deadTarget.getComponent(CombatStatsComponent.class).setHealth(0);
    step();
    assertTrue(first.isDisposed());

    Entity removedTarget = createEnemy(4f, 0f, 100, null);
    Entity second = fireAt(removedTarget);
    removedTarget.dispose();
    step();
    assertTrue(second.isDisposed());
    assertEquals(1, physics.getWorld().getBodyCount());
  }

  @Test
  void shouldNotDamageFromCollisionAfterOwnerDies() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    Entity projectile = fireAt(enemy);
    owner.getComponent(CombatStatsComponent.class).setHealth(0);
    // Exercise the collision guard before the projectile's next update can cancel its flight.
    for (int i = 0; i < 100 && scheduled.isEmpty(); i++) {
      physics.update();
    }
    assertFalse(scheduled.isEmpty());
    runScheduled();

    assertTrue(projectile.isDisposed());
    assertEquals(100, health(enemy));
  }

  @Test
  void shouldCancelOldProjectilesWhenPetIsReplaced() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    Entity projectile = fireAt(enemy);

    owner.getComponent(PetManagerComponent.class).activatePet(new ShopComponent.Pet("Bat"));
    step();

    assertTrue(projectile.isDisposed());
    assertEquals(100, health(enemy));
    assertEquals(1, physics.getWorld().getBodyCount());
  }

  @Test
  void shouldTolerateSceneDisposalBeforeScheduledProjectileCleanup() {
    Entity enemy = createEnemy(4f, 0f, 100, null);
    Entity projectile = fireAt(enemy);
    owner.dispose();
    entities.update();
    assertFalse(scheduled.isEmpty());

    entities.dispose();
    runScheduled();

    assertTrue(projectile.isDisposed());
    assertEquals(0, physics.getWorld().getBodyCount());
  }

  private Entity createEnemy(float x, float y, int health, ItemDropComponent dropper) {
    Entity enemy =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            // Two fixtures can report the same hit during a single physics step.
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.NPC).setSensor(true))
            .addComponent(new CombatStatsComponent(health, 0));
    if (dropper != null) {
      enemy.addComponent(dropper).addComponent(new EnemyDeathComponent());
    }
    enemy.setPosition(x, y);
    enemy.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    entities.register(enemy);
    return enemy;
  }

  private Entity fireAt(Entity enemy) {
    Entity projectile = PetProjectileFactory.createProjectile(pet, enemy);
    entities.register(projectile);
    return projectile;
  }

  private Entity findPetProjectile() {
    Array<Body> bodies = new Array<>();
    physics.getWorld().getBodies(bodies);
    for (Body body : bodies) {
      Entity entity = ((BodyUserData) body.getUserData()).entity;
      if (entity.getComponent(PetProjectileComponent.class) != null) {
        return entity;
      }
    }
    return null;
  }

  private int health(Entity enemy) {
    return enemy.getComponent(CombatStatsComponent.class).getHealth();
  }

  private void flyUntilDisposed(Entity projectile) {
    for (int i = 0; i < 300 && !projectile.isDisposed(); i++) {
      step();
    }
    assertTrue(projectile.isDisposed(), "Projectile should hit or expire within its lifetime");
  }

  private void step() {
    physics.update();
    entities.update();
    runScheduled();
  }

  private void runScheduled() {
    assertFalse(physics.getWorld().isLocked());
    while (!scheduled.isEmpty()) {
      scheduled.remove().run();
    }
  }
}
