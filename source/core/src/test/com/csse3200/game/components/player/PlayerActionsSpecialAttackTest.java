package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerActionsSpecialAttackTest {
  @Test
  void shouldDealTripleDamageToOneNearbyEnemyAndRespectCooldown() {
    PlayerActions actions = new PlayerActions();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 10);
    StaminaComponent stamina = new StaminaComponent();
    HitboxComponent hitbox = mock(HitboxComponent.class);
    Fixture playerFixture = mock(Fixture.class);
    when(hitbox.getFixture()).thenReturn(playerFixture);

    Entity player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(stamina)
            .addComponent(hitbox)
            .addComponent(actions);
    player.create();

    CombatStatsComponent enemyStats = new CombatStatsComponent(100, 0);
    Entity enemy = new Entity().addComponent(enemyStats);
    enemy.setPosition(1f, 0f);
    AtomicReference<Entity> effectTarget = new AtomicReference<>();
    player.getEvents().addListener("specialAttackHit", effectTarget::set);
    player.getEvents().trigger("collisionStart", playerFixture, npcFixtureFor(enemy));

    player.getEvents().trigger("specialAttack");
    player.getEvents().trigger("specialAttack");

    assertEquals(70, enemyStats.getHealth());
    assertEquals(95f, stamina.getStamina());
    assertEquals(enemy, effectTarget.get());
  }

  @Test
  void shouldDamageEveryEnemyInsideAreaAndRespectItsOwnCooldown() {
    World world = new World(new Vector2(0f, 0f), true);
    PhysicsEngine physics = new PhysicsEngine(world, mock(GameTime.class));
    ServiceLocator.registerPhysicsService(new PhysicsService(physics));

    try {
      PlayerActions actions = new PlayerActions();
      CombatStatsComponent playerStats = new CombatStatsComponent(100, 10);
      StaminaComponent stamina = new StaminaComponent();
      Entity player =
          new Entity()
              .addComponent(playerStats)
              .addComponent(stamina)
              .addComponent(mock(HitboxComponent.class))
              .addComponent(actions);
      player.create();

      CombatStatsComponent firstStats = new CombatStatsComponent(100, 0);
      Entity firstEnemy = createNpc(world, firstStats, 1f, 0f);
      CombatStatsComponent secondStats = new CombatStatsComponent(100, 0);
      Entity secondEnemy = createNpc(world, secondStats, 0f, 1f);
      CombatStatsComponent distantStats = new CombatStatsComponent(100, 0);
      createNpc(world, distantStats, 2.5f, 0f);

      AtomicReference<Entity> lastHit = new AtomicReference<>();
      AtomicInteger hitCount = new AtomicInteger();
      player
          .getEvents()
          .addListener(
              "areaAttackHit",
              (Entity target) -> {
                lastHit.set(target);
                hitCount.incrementAndGet();
              });

      player.getEvents().trigger("areaAttack");
      player.getEvents().trigger("areaAttack");

      assertEquals(80, firstStats.getHealth());
      assertEquals(80, secondStats.getHealth());
      assertEquals(100, distantStats.getHealth());
      assertEquals(2, hitCount.get());
      assertTrue(lastHit.get() == firstEnemy || lastHit.get() == secondEnemy);
      assertEquals(95f, stamina.getStamina());
    } finally {
      physics.dispose();
    }
  }

  private Entity createNpc(World world, CombatStatsComponent stats, float x, float y) {
    Entity enemy = new Entity().addComponent(stats);
    enemy.setPosition(x, y);

    BodyDef bodyDefinition = new BodyDef();
    bodyDefinition.type = BodyDef.BodyType.StaticBody;
    bodyDefinition.position.set(enemy.getCenterPosition());
    Body body = world.createBody(bodyDefinition);
    BodyUserData userData = new BodyUserData();
    userData.entity = enemy;
    body.setUserData(userData);

    PolygonShape shape = new PolygonShape();
    shape.setAsBox(0.1f, 0.1f);
    FixtureDef fixtureDefinition = new FixtureDef();
    fixtureDefinition.shape = shape;
    fixtureDefinition.filter.categoryBits = PhysicsLayer.NPC;
    body.createFixture(fixtureDefinition);
    shape.dispose();
    return enemy;
  }

  private Fixture npcFixtureFor(Entity enemy) {
    Fixture fixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData bodyUserData = new BodyUserData();
    bodyUserData.entity = enemy;
    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.NPC;

    when(fixture.getBody()).thenReturn(body);
    when(fixture.getFilterData()).thenReturn(filter);
    when(body.getUserData()).thenReturn(bodyUserData);
    return fixture;
  }
}
