package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.projectile.ProjectileType;
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
 * Tests for the S2 ranged windup. Assumed behaviour (see S2 spec):
 *
 * <ul>
 *   <li>A valid trigger with a windup above zero stores the target and projectile type, resets the
 *       cooldown, and fires {@code "rangedAttackWindup"} (target). No arrow exists yet.
 *   <li>{@code update()} counts the windup down. When it ends, a still-valid target gets an arrow
 *       and {@code "rangedAttackFired"} (target); an invalid target gets {@code
 *       "rangedAttackCancelled"} (target) and no arrow. Range is checked at commit only.
 *   <li>A windup of zero announces the windup and fires inside the same trigger call, so the
 *       existing RangedAttackComponentTest cases keep passing unchanged.
 * </ul>
 *
 * Clock: 0.02 s per update. Test weapon windup is 0.1 s, so the shot resolves on update 5.
 */
@ExtendWith(GameExtension.class)
class RangedAttackWindupTest {
  private static final float DELTA = 0.02f;
  private static final float WINDUP = 0.1f;
  private static final float COOLDOWN = 2f;
  private static final float RANGE = 6f;
  private static final int UPDATES_TO_RESOLVE = 6;

  @BeforeEach
  void beforeEach() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(DELTA);
    ServiceLocator.registerTimeSource(gameTime);
  }

  // ---------- windup in progress ----------

  @Test
  void validTriggerStartsAWindupAndFiresNothingYet() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, ev.windups.size(), "Exactly one windup event expected.");
    assertSame(target, ev.windups.get(0));
    assertEquals(0, ev.fired.size(), "No arrow may be fired before the windup ends.");
    assertEquals(0, ev.cancels.size());
  }

  @Test
  void firesExactlyOnceWhenTheWindupEnds() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.fired.size(), "Exactly one shot expected after the windup.");
    assertSame(target, ev.fired.get(0));
    assertEquals(0, ev.cancels.size());
  }

  @Test
  void doesNotFireBeforeTheWindupHasFullyElapsed() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, 3); // 0.06 s of 0.10 s

    assertEquals(0, ev.fired.size(), "Fired early, with windup still running.");
  }

  @Test
  void aSecondTriggerDuringWindupDoesNotQueueAnotherShot() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    attacker.update();
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.windups.size());
    assertEquals(1, ev.fired.size());
  }

  @Test
  void cooldownStillLimitsTheSecondShotAfterTheFirstResolves() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW); // still cooling
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    assertEquals(1, ev.fired.size(), "Cooldown must still gate the second shot.");

    runUpdates(attacker, (int) (COOLDOWN / DELTA)); // let the cooldown elapse
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    assertEquals(2, ev.fired.size(), "A second shot should be allowed after the cooldown.");
  }

  // ---------- whiff ----------

  @Test
  void cancelsWhenTheTargetDiesDuringWindup() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.cancels.size());
    assertSame(target, ev.cancels.get(0));
    assertEquals(0, ev.fired.size(), "No arrow may be spawned for a dead target.");
  }

  @Test
  void stillFiresWhenTheTargetLeavesRangeDuringWindup() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    target.setPosition(RANGE + 20f, 0f);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.fired.size(), "Range is checked at commit only, so the shot still leaves.");
    assertEquals(0, ev.cancels.size());
  }

  @Test
  void aCancelStillSpendsTheCooldown() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    target.getComponent(CombatStatsComponent.class).setHealth(10);
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.cancels.size());
    assertEquals(0, ev.fired.size(), "A cancelled windup must not refund the cooldown.");
  }

  // ---------- rejected triggers ----------

  @Test
  void rejectsNullTargetWithoutAnyEvent() {
    Entity attacker = createAttacker(WINDUP);
    Events ev = listenToAll(attacker);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", (Entity) null, ProjectileType.ARROW));
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, ev.total());
  }

  @Test
  void rejectsNullProjectileTypeWithoutAnyEventAndWithoutSpendingTheCooldown() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, (ProjectileType) null);
    assertEquals(0, ev.total());

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);
    assertEquals(1, ev.fired.size(), "A rejected trigger must not eat the cooldown.");
  }

  @Test
  void rejectsOutOfRangeTargetAtTriggerTime() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(RANGE + 1f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, ev.total(), "A rejected trigger starts no windup, so it cannot whiff either.");
  }

  @Test
  void acceptsATargetExactlyAtRangeBoundary() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(RANGE, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.fired.size());
  }

  @Test
  void rejectsATargetWithoutCombatStats() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = new Entity();
    target.create();
    target.setPosition(2f, 0f);
    Events ev = listenToAll(attacker);

    assertDoesNotThrow(
        () -> attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW));
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(0, ev.total());
  }

  // ---------- projectile types ----------

  @Test
  void lightningShotAlsoWaitsOutTheWindup() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);
    assertEquals(0, ev.fired.size(), "Lightning must not strike before its windup ends.");
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertEquals(1, ev.fired.size());
  }

  // ---------- zero windup compatibility ----------

  @Test
  void zeroWindupAnnouncesWindupThenFiresImmediately() {
    Entity attacker = createAttacker(0f);
    Entity target = createTarget(2f, 10);
    Events ev = listenToAll(attacker);

    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertEquals(1, ev.fired.size(), "Zero windup must fire inside the trigger call.");
    assertEquals(1, ev.windups.size(), "The windup event is still announced, once.");
    assertFalse(
        attacker.getComponent(RangedAttackComponent.class).isWindingUp(),
        "A zero windup must not leave a shot pending.");
  }

  // ---------- windup getter ----------

  @Test
  void windupDurationGetterReportsTheWeaponWindup() {
    RangedAttackComponent ranged = new RangedAttackComponent(RANGE, COOLDOWN, 0f, bow(WINDUP));
    assertTrue(ranged.getWindupDuration() >= 0f);
    assertEquals(
        WINDUP, ranged.getWindupDuration(), 1e-6f, "Armed windup must come from the weapon.");
  }

  @Test
  void unarmedWindupDurationIsCooldownMinusOne() {
    RangedAttackComponent ranged = new RangedAttackComponent(RANGE, 3f, 0f);
    assertEquals(2f, ranged.getWindupDuration(), 1e-6f);
  }

  @Test
  void rejectsAWindupThatIsNotShorterThanTheCooldown() {
    assertThrowsIllegalArgument(() -> new RangedAttackComponent(RANGE, 1f, 0f, bow(1f)));
    assertThrowsIllegalArgument(() -> new RangedAttackComponent(RANGE, 1f, 0f, bow(2f)));
  }

  @Test
  void notWindingUpOnceTheShotHasResolved() {
    Entity attacker = createAttacker(WINDUP);
    Entity target = createTarget(2f, 10);
    attacker.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    runUpdates(attacker, UPDATES_TO_RESOLVE);

    assertFalse(attacker.getComponent(RangedAttackComponent.class).isWindingUp());
  }

  // ---------- helpers ----------

  private static void assertThrowsIllegalArgument(Runnable action) {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, action::run);
  }

  private WeaponItem bow(float windup) {
    return new WeaponItem("Test Bow", WeaponType.BOW, WeaponTier.TIER_1, 1, 1, windup);
  }

  private Entity createAttacker(float windup) {
    Entity attacker =
        new Entity()
            .addComponent(new RangedAttackComponent(RANGE, COOLDOWN, 0f, bow(windup)))
            .addComponent(new CombatStatsComponent(20, 5));
    attacker.create();
    attacker.setPosition(0f, 0f);
    return attacker;
  }

  private Entity createTarget(float x, int health) {
    Entity target = new Entity().addComponent(new CombatStatsComponent(health, 0));
    target.create();
    target.setPosition(x, 0f);
    return target;
  }

  private void runUpdates(Entity entity, int frames) {
    for (int i = 0; i < frames; i++) {
      entity.update();
    }
  }

  /** Collects every ranged attack event the attacker announces. */
  private static final class Events {
    final List<Entity> windups = new ArrayList<>();
    final List<Entity> fired = new ArrayList<>();
    final List<Entity> cancels = new ArrayList<>();

    int total() {
      return windups.size() + fired.size() + cancels.size();
    }
  }

  private Events listenToAll(Entity attacker) {
    Events ev = new Events();
    attacker.getEvents().addListener("rangedAttackWindup", (Entity e) -> ev.windups.add(e));
    attacker.getEvents().addListener("rangedAttackFired", (Entity e) -> ev.fired.add(e));
    attacker.getEvents().addListener("rangedAttackCancelled", (Entity e) -> ev.cancels.add(e));
    return ev;
  }
}
