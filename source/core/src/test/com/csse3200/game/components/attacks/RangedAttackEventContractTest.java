package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.components.tasks.RangedAttackTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
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
 * Event contract and firing behaviour of {@link RangedAttackComponent} after the projectile type
 * was added.
 *
 * <p>The component registers {@code attemptAttack(Entity target, ProjectileType projectile)} for
 * the {@code "rangedAttack"} event, so every trigger carries TWO values after the event name: the
 * target, then the projectile type. The event handler picks the listener type from the number of
 * values, so a one-value trigger fails with a ClassCastException at runtime, and swapped values
 * fail the same way inside the listener.
 *
 * <p>Fixture: frame time 0.02 s; shooter at the origin with a bow of zero windup (so a shot leaves
 * on the trigger call), range 8, cooldown 1 s; target at (3, 0) with combat stats; a mock entity
 * service records every entity registered, so arrows can be counted and inspected.
 */
@ExtendWith(GameExtension.class)
class RangedAttackEventContractTest {
  private static final float RANGE = 8f;
  private static final float COOLDOWN = 1f;

  private final List<Entity> registered = new ArrayList<>();
  private Entity shooter;
  private Entity target;

  @BeforeEach
  void setUp() {
    registered.clear();
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    EntityService entityService = mock(EntityService.class);
    doAnswer(
            invocation -> {
              registered.add(invocation.getArgument(0));
              return null;
            })
        .when(entityService)
        .register(any(Entity.class));
    ServiceLocator.registerEntityService(entityService);
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(gameTime);

    shooter = createShooter(99);
    target = createTarget(3f);
  }

  // ---------- firing ----------

  @Test
  void firesOneArrowAtAValidTargetAndResetsTheCooldown() {
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size(), "Expected one arrow after a valid two-value attack");

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size(), "A second trigger on the same frame must not fire");
  }

  @Test
  void arrowSpawnsOnTheRightWhenTheTargetIsToTheRight() {
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    float expected = shooter.getCenterPosition().x + 0.3f;
    assertEquals(expected, registered.get(0).getPosition().x, 1e-4f);
  }

  @Test
  void arrowSpawnsOnTheLeftWhenTheTargetIsToTheLeft() {
    Entity leftTarget = createTarget(-3f);

    shooter.getEvents().trigger("rangedAttack", leftTarget, ProjectileType.ARROW);

    float expected = shooter.getCenterPosition().x - 0.3f;
    assertEquals(expected, registered.get(0).getPosition().x, 1e-4f);
  }

  @Test
  void lightningSpawnsAboveTheTargetCentre() {
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);

    assertEquals(1, registered.size());
    Entity bolt = registered.get(0);
    // The bolt is centred on the target in x (its width is 0.3125) and starts 8 units above it.
    assertEquals(target.getCenterPosition().x - 0.3125f / 2f, bolt.getPosition().x, 1e-4f);
    assertEquals(target.getCenterPosition().y + 8f, bolt.getPosition().y, 1e-4f);
  }

  // ---------- cooldown and range ----------

  @Test
  void doesNotFireDuringTheCooldown() {
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    updateFrames(shooter, 25); // 0.5 s of a 1 s cooldown

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, registered.size(), "A second shot inside the cooldown must be ignored");
  }

  @Test
  void firesAgainOnceTheCooldownHasElapsed() {
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    updateFrames(shooter, 51); // slightly over 1 s, not exactly 50

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(2, registered.size(), "A shot after the cooldown should fire");
  }

  @Test
  void firesAtExactlyMaximumRange() {
    Entity edge = createTarget(RANGE);

    shooter.getEvents().trigger("rangedAttack", edge, ProjectileType.ARROW);

    assertEquals(1, registered.size(), "A target exactly at maximum range should be shot");
  }

  @Test
  void doesNotFireJustBeyondMaximumRangeAndKeepsTheCooldown() {
    Entity tooFar = createTarget(RANGE + 0.01f);

    shooter.getEvents().trigger("rangedAttack", tooFar, ProjectileType.ARROW);
    assertEquals(0, registered.size(), "A target just outside range must not be shot");

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size(), "The rejected trigger must not have spent the cooldown");
  }

  // ---------- rejected triggers ----------

  @Test
  void nullTargetDoesNothingAndKeepsTheCooldown() {
    assertDoesNotThrow(
        () -> shooter.getEvents().trigger("rangedAttack", (Entity) null, ProjectileType.ARROW));
    assertEquals(0, registered.size());

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size(), "A null target must not eat the cooldown");
  }

  @Test
  void nullProjectileTypeDoesNothingAndKeepsTheCooldown() {
    assertDoesNotThrow(
        () -> shooter.getEvents().trigger("rangedAttack", target, (ProjectileType) null));
    assertEquals(0, registered.size());

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size(), "A null projectile type must not eat the cooldown");
  }

  @Test
  void targetWithoutCombatStatsIsIgnoredAndKeepsTheCooldown() {
    Entity noStats = new Entity();
    noStats.create();
    noStats.setPosition(3f, 0f);

    assertDoesNotThrow(
        () -> shooter.getEvents().trigger("rangedAttack", noStats, ProjectileType.ARROW));
    assertEquals(0, registered.size(), "A target that cannot take damage must not be shot at");

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, registered.size());
  }

  @Test
  void swappedArgumentsThrowAndSpawnNothing() {
    // The order is part of the contract: target first, projectile type second.
    assertThrows(
        ClassCastException.class,
        () -> shooter.getEvents().trigger("rangedAttack", ProjectileType.ARROW, target));

    assertEquals(0, registered.size());
  }

  @Test
  void aOneValueTriggerFailsWithAClassCastException() {
    // A forgotten call site that sends only the target fails like this every time it attacks.
    assertThrows(
        ClassCastException.class, () -> shooter.getEvents().trigger("rangedAttack", target));

    assertEquals(0, registered.size());
  }

  // ---------- announced events ----------

  @Test
  void rangedAttackFiredIsTriggeredOncePerShotWithTheTarget() {
    List<Entity> fired = new ArrayList<>();
    shooter.getEvents().addListener("rangedAttackFired", (Entity t) -> fired.add(t));

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    assertEquals(1, fired.size());
    assertSame(target, fired.get(0));

    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW); // cooldown
    assertEquals(1, fired.size(), "No second 'fired' event during the cooldown");
  }

  @Test
  void rangedAttackHitIsRefiredOnTheShooterWhenTheArrowHits() {
    List<Entity> hits = new ArrayList<>();
    shooter.getEvents().addListener("rangedAttackHit", (Entity t) -> hits.add(t));
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    Entity arrow = registered.get(0);

    arrow.getEvents().trigger("projectileHit", target);

    assertEquals(1, hits.size());
    assertSame(target, hits.get(0));
  }

  // ---------- damage ----------

  @Test
  void arrowDamageIsTheWeaponsDamageNotTheShootersBaseAttack() {
    // shooter base attack is 99; a tier-1 bow deals 7 per arrow
    shooter.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    int arrowDamage = registered.get(0).getComponent(CombatStatsComponent.class).getBaseAttack();
    assertEquals(7, arrowDamage);
  }

  // ---------- the AI task ----------

  @Test
  void rangedAttackTaskFiresTheEventWithTheTargetAndAProjectileType() {
    // Components cannot be added after an entity is created, so the AI goes in at build time.
    AITaskComponent ai = new AITaskComponent();
    ai.addTask(new RangedAttackTask(target, 15, RANGE, ProjectileType.ARROW));
    Entity archer = createShooter(5, ai);
    List<Entity> fired = new ArrayList<>();
    archer.getEvents().addListener("rangedAttackFired", (Entity t) -> fired.add(t));

    archer.update();

    assertEquals(1, fired.size(), "The task must send both values or the attack never happens");
    assertEquals(1, registered.size());
  }

  // ---------- helpers ----------

  private Entity createShooter(int baseAttack) {
    return createShooter(baseAttack, null);
  }

  private Entity createShooter(int baseAttack, AITaskComponent ai) {
    WeaponItem bow = new WeaponItem("Test Bow", WeaponType.BOW, WeaponTier.TIER_1, 1, 1, 0f);
    Entity entity =
        new Entity()
            .addComponent(new RangedAttackComponent(RANGE, COOLDOWN, 0f, bow))
            .addComponent(new CombatStatsComponent(20, baseAttack));
    if (ai != null) {
      entity.addComponent(ai);
    }
    entity.create();
    entity.setPosition(0f, 0f);
    return entity;
  }

  private Entity createTarget(float x) {
    Entity entity = new Entity().addComponent(new CombatStatsComponent(10, 0));
    entity.create();
    entity.setPosition(x, 0f);
    return entity;
  }

  private void updateFrames(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      entity.update();
    }
  }
}
