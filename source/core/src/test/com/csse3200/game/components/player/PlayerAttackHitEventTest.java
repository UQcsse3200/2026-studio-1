package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Verifies the hit event that companion combat will consume, using the real attack handler. */
@ExtendWith(GameExtension.class)
class PlayerAttackHitEventTest {
  private Entity player;
  private Fixture playerFixture;
  private StaminaComponent stamina;
  private final List<Entity> hitTargets = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ResourceService resources = mock(ResourceService.class);
    Sound impactSound = mock(Sound.class);
    when(resources.getAsset("sounds/Impact4.ogg", Sound.class)).thenReturn(impactSound);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerEntityService(new EntityService());

    HitboxComponent hitbox = mock(HitboxComponent.class);
    playerFixture = mock(Fixture.class);
    when(hitbox.getFixture()).thenReturn(playerFixture);
    PhysicsComponent physics = mock(PhysicsComponent.class);

    Body physicsBody = mock(Body.class);
    when(physics.getBody()).thenReturn(physicsBody);
    stamina = new StaminaComponent();
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(stamina)
            .addComponent(hitbox)
            .addComponent(physics)
            .addComponent(new PlayerActions());
    player.create();
    EventListener1<Entity> hitTargetListener = hitTargets::add;
    player.getEvents().addListener("playerAttackHit", hitTargetListener);
  }

  @Test
  void shouldReportEachMeleeTargetAfterDamageAndKeepWeaponAttackEvent() {
    Entity first = enemyInRange(100);
    Entity second = enemyInRange(100);
    List<Integer> healthAtNotification = new ArrayList<>();
    AtomicInteger weaponAttacks = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            "playerAttackHit",
            (Entity target) ->
                healthAtNotification.add(
                    target.getComponent(CombatStatsComponent.class).getHealth()));
    player.getEvents().addListener("weaponAttack", weaponAttacks::incrementAndGet);

    player.getEvents().trigger("attack");

    assertEquals(2, hitTargets.size());
    assertEquals(Set.of(first, second), Set.copyOf(hitTargets));
    assertEquals(List.of(90, 90), healthAtNotification);
    assertEquals(1, weaponAttacks.get());
  }

  @Test
  void shouldNotReportAnotherHitDuringCooldown() {
    Entity enemy = enemyInRange(100);

    player.getEvents().trigger("attack");
    player.getEvents().trigger("attack");

    assertEquals(List.of(enemy), hitTargets);
    assertEquals(90, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotReportHitsWhenNoEnemyIsInRange() {
    player.getEvents().trigger("attack");

    assertTrue(hitTargets.isEmpty());
  }

  @Test
  void shouldNotReportHitsWhenStaminaIsExhausted() {
    Entity enemy = enemyInRange(100);
    stamina.useStamina(stamina.getMaxStamina());

    player.getEvents().trigger("attack");

    assertTrue(hitTargets.isEmpty());
    assertEquals(100, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldNotReportHitsAfterPlayerDeath() {
    Entity enemy = enemyInRange(100);
    player.getComponent(CombatStatsComponent.class).setHealth(0);

    player.getEvents().trigger("attack");

    assertTrue(hitTargets.isEmpty());
    assertEquals(100, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldIgnoreEnemiesThatAreAlreadyDeadOrDisposed() {
    enemyInRange(0);
    Entity disposedEnemy = enemyInRange(100);
    disposedEnemy.dispose();

    player.getEvents().trigger("attack");

    assertTrue(hitTargets.isEmpty());
    assertEquals(100, disposedEnemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldReportLethalHitWithTheDefeatedTarget() {
    Entity enemy = enemyInRange(5);

    player.getEvents().trigger("attack");

    assertEquals(List.of(enemy), hitTargets);
    assertTrue(enemy.getComponent(CombatStatsComponent.class).isDead());
  }

  private Entity enemyInRange(int health) {
    Entity enemy = new Entity().addComponent(new CombatStatsComponent(health, 0));
    enemy.create();
    Fixture fixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData userData = new BodyUserData();
    userData.entity = enemy;
    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.NPC;
    when(fixture.getFilterData()).thenReturn(filter);
    when(fixture.getBody()).thenReturn(body);
    when(body.getUserData()).thenReturn(userData);
    player.getEvents().trigger("collisionStart", playerFixture, fixture);
    return enemy;
  }
}
