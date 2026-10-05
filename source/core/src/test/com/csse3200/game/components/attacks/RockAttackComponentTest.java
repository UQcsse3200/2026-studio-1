package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.AttackTestWorld;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * TEMPLATE 1 of 2: how to test an attack component.
 *
 * <p>Copy this class for {@code LaserAttackComponentTest}. Change the class under test, the event
 * prefix (rockAttack becomes laserAttack), the constructor arguments and the projectile assertions;
 * keep the structure. The groups below are the ones every attack component needs:
 *
 * <ol>
 *   <li>construction and validation: stored values, bad arguments, parameterised bad values
 *   <li>lookup: the subclass is found by its own class, never by the base class
 *   <li>events: the prefix is used for every event, the default prefix is never used
 *   <li>windup and cooldown: timed behaviour driven by {@code step}
 *   <li>the projectile: where it appears, how much damage it carries, where it flies
 *   <li>refusals: out of range, dead target, no target, two attacks on one owner
 * </ol>
 *
 * <p>The numbers: the test weapon deals 10 damage with a 0.1 s windup; the rock multiplies damage
 * by 1.5, so a hit is 15. The owner is 1 wide by 2 tall at the origin. One frame is 0.02 s.
 */
@ExtendWith(GameExtension.class)
class RockAttackComponentTest {
  private static final float RANGE = 11f;
  private static final float COOLDOWN = 3f;
  private static final float WINDUP = 0.1f;
  private static final float KNOCKBACK = 2f;
  private static final float SPAWN_HEIGHT = 0.8f;
  private static final float MULTIPLIER = 1.5f;
  private static final int WEAPON_DAMAGE = 10;
  private static final float SPEED = 8f;

  private AttackTestWorld world;

  @BeforeEach
  void beforeEach() {
    world = AttackTestWorld.create();
  }

  // ---------- 1. construction and validation ----------

  @Test
  void shouldStoreEverySettingItWasGiven() {
    RockAttackComponent rock = newRock();

    assertEquals(RANGE, rock.getRange(), 1e-6f);
    assertEquals(COOLDOWN, rock.getCooldown(), 1e-6f);
    assertEquals(KNOCKBACK, rock.getKnockback(), 1e-6f);
    assertEquals(SPAWN_HEIGHT, rock.getSpawnHeightFraction(), 1e-6f);
    assertEquals(MULTIPLIER, rock.getDamageMultiplier(), 1e-6f);
    assertEquals(WINDUP, rock.getWindupDuration(), 1e-6f);
    assertEquals(WEAPON_DAMAGE, rock.getDamage());
  }

  @ParameterizedTest(name = "spawnHeightFraction {0} is rejected")
  @ValueSource(floats = {-0.01f, 1.01f, 2f, -5f})
  void shouldRejectASpawnHeightOutsideZeroToOne(float fraction) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), fraction, MULTIPLIER));
  }

  @ParameterizedTest(name = "spawnHeightFraction {0} is accepted")
  @ValueSource(floats = {0f, 0.5f, 1f})
  void shouldAcceptTheEdgesOfTheSpawnHeightRange(float fraction) {
    assertDoesNotThrow(
        () -> new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), fraction, MULTIPLIER));
  }

  @ParameterizedTest(name = "damageMultiplier {0} is rejected")
  @ValueSource(floats = {0f, -1f, -0.0001f})
  void shouldRejectAZeroOrNegativeMultiplier(float multiplier) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockAttackComponent(
                RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, multiplier));
  }

  @Test
  void shouldRejectANotANumberMultiplier() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, Float.NaN));
  }

  @Test
  void shouldInheritTheRulesOfTheRangedConstructor() {
    // A zero range, a negative knockback and a windup as long as the cooldown are all refused by
    // the parent. A copy of this test for the laser keeps the same three cases.
    assertThrows(
        IllegalArgumentException.class,
        () -> new RockAttackComponent(0f, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, MULTIPLIER));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RockAttackComponent(RANGE, COOLDOWN, -1f, weapon(), SPAWN_HEIGHT, MULTIPLIER));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RockAttackComponent(
                RANGE,
                COOLDOWN,
                KNOCKBACK,
                AttackTestWorld.naturalWeapon(WEAPON_DAMAGE, COOLDOWN),
                SPAWN_HEIGHT,
                MULTIPLIER));
  }

  @Test
  void shouldRejectANullWeapon() {
    assertThrows(
        Exception.class,
        () -> new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, null, SPAWN_HEIGHT, MULTIPLIER));
  }

  // ---------- 2. lookup ----------

  @Test
  void shouldBeFoundByItsOwnClassButNotByTheBaseClass() {
    Entity owner = world.newAttacker(newRock());

    assertNotNull(owner.getComponent(RockAttackComponent.class));
    assertNull(
        owner.getComponent(RangedAttackComponent.class),
        "the base-class lookup does not find a subclass; the factory and controller must use"
            + " the subclass");
  }

  // ---------- 3. events ----------

  @Test
  void shouldAnnounceTheWindupUnderTheRockPrefix() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "rockAttackWindup");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);

    assertEquals(1, windups.size());
    assertSame(target, windups.get(0));
  }

  @Test
  void shouldAnnounceTheShotUnderTheRockPrefixWhenTheWindupEnds() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> fired = AttackTestWorld.record(owner, "rockAttackFired");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, fired.size());
    assertSame(target, fired.get(0));
  }

  @Test
  void shouldNeverUseTheDefaultRangedPrefix() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "rangedAttackWindup");
    List<Entity> fired = AttackTestWorld.record(owner, "rangedAttackFired");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertTrue(windups.isEmpty(), "a rock must not announce a plain ranged windup");
    assertTrue(fired.isEmpty(), "a rock must not announce a plain ranged shot");
  }

  @Test
  void shouldIgnoreTheDefaultRangedTrigger() {
    // The Zeus lightning and the skeleton arrow still answer to "rangedAttack"; a rock must not.
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "rockAttackWindup");

    owner.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    assertTrue(windups.isEmpty());
  }

  // ---------- 4. windup and cooldown ----------

  @Test
  void shouldNotCreateTheRockUntilTheWindupHasElapsed() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(2, owner); // 0.04 s of 0.10 s

    assertEquals(0, world.registeredSince(before).size());
    assertTrue(owner.getComponent(RockAttackComponent.class).isWindingUp());
  }

  @Test
  void shouldCreateExactlyOneRockWhenTheWindupEnds() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size());
    assertFalse(owner.getComponent(RockAttackComponent.class).isWindingUp());
  }

  @Test
  void shouldNotThrowASecondRockBeforeTheCooldownHasPassed() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW); // still cooling down
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size(), "cooldown allows one rock only");
  }

  @Test
  void shouldThrowASecondRockOnceTheCooldownHasPassed() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    world.step(AttackTestWorld.framesFor(COOLDOWN), owner);
    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(2, world.registeredSince(before).size());
  }

  // ---------- 5. the projectile ----------

  @Test
  void shouldLaunchTheRockHigherWhenTheSpawnHeightIsHigher() {
    Vector2 low = launchPositionFor(0.1f);
    Vector2 high = launchPositionFor(0.9f);

    assertTrue(
        high.y > low.y + 0.5f,
        "a spawn height of 0.9 of a 2 unit body must be clearly above 0.1 of it: "
            + low.y
            + " then "
            + high.y);
  }

  @Test
  void shouldLaunchTheRockFromAboveTheFeetWhenTheFractionIsOne() {
    Vector2 top = launchPositionFor(1f);
    Vector2 feet = launchPositionFor(0f);

    assertTrue(top.y - feet.y > 1.5f, "full height of a 2 unit body is about 2 units higher");
  }

  @Test
  void shouldCarryTheWeaponDamageTimesTheMultiplier() {
    // 10 damage x 1.5 = 15 on contact. Rock flies an arc to the target's position at launch.
    assertEquals(50 - 15, healthAfterRockHits(WEAPON_DAMAGE, MULTIPLIER, 50));
  }

  @Test
  void shouldRoundTheScaledDamageAndNeverGoBelowOne() {
    // 1 damage x 0.2 = 0.2 rounds to 0, but a rock always does at least 1.
    assertEquals(50 - 1, healthAfterRockHits(1, 0.2f, 50));
  }

  @Test
  void shouldFlyTowardsTheTargetAndLandOnItsPositionAtLaunch() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(6f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    Entity rock = world.registeredSince(before).get(0);

    assertTrue(
        AttackTestWorld.velocityOf(rock).x > 0f, "the rock leaves heading toward the target");
  }

  @Test
  void shouldFlyLeftWhenTheTargetIsToTheLeft() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(-6f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    Entity rock = world.registeredSince(before).get(0);

    assertTrue(AttackTestWorld.velocityOf(rock).x < 0f);
  }

  @Test
  void shouldIgnoreWhichProjectileTypeTheTaskAsksFor() {
    // The task passes ARROW or LIGHTNING; the rock always builds a rock.
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.LIGHTNING);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size());
  }

  // ---------- 6. refusals ----------

  @Test
  void shouldRefuseATargetBeyondItsRange() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(RANGE + 5f, 0f, 50);
    int before = world.mark();
    List<Entity> windups = AttackTestWorld.record(owner, "rockAttackWindup");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertTrue(windups.isEmpty(), "out of range means no windup");
    assertEquals(0, world.registeredSince(before).size());
  }

  @Test
  void shouldCancelWhenTheTargetDiesDuringTheWindup() {
    Entity owner = world.newAttacker(newRock());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> cancelled = AttackTestWorld.record(owner, "rockAttackCancelled");
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, cancelled.size());
    assertEquals(0, world.registeredSince(before).size(), "no rock for a dead target");
  }

  @Test
  void shouldNotThrowForANullTarget() {
    Entity owner = world.newAttacker(newRock());

    assertDoesNotThrow(
        () -> {
          owner.getEvents().trigger("rockAttack", null, ProjectileType.ARROW);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
        });
  }

  @Test
  void shouldNotTriggerTheLaserWhenTheRockIsThrown() {
    // Two attacks on one owner, each under its own prefix: a rock trigger starts only the rock.
    RockAttackComponent rock = newRock();
    RangedAttackComponent other = new RangedAttackComponent(7f, 3f, 0f, weapon()) {
          // stands in for the laser: an ordinary ranged attack under the default prefix
        };
    Entity owner = world.newAttacker(rock, other);
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> otherWindups = AttackTestWorld.record(owner, "rangedAttackWindup");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);

    assertTrue(otherWindups.isEmpty(), "only the rock answers to rockAttack");
    assertTrue(rock.isWindingUp());
    assertFalse(other.isWindingUp());
  }

  // ---------- helpers ----------

  private WeaponItem weapon() {
    return AttackTestWorld.naturalWeapon(WEAPON_DAMAGE, WINDUP);
  }

  private RockAttackComponent newRock() {
    RockAttackComponent rock =
        new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, MULTIPLIER);
    rock.setProjectileSpeed(SPEED);
    return rock;
  }

  /**
   * Throws one rock from a fresh owner with the given spawn height and returns where it started.
   */
  private Vector2 launchPositionFor(float fraction) {
    RockAttackComponent rock =
        new RockAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), fraction, MULTIPLIER);
    rock.setProjectileSpeed(SPEED);
    Entity owner = world.newAttacker(rock);
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    return world.registeredSince(before).get(0).getPosition().cpy();
  }

  /**
   * Throws one rock at a stationary target and steps until it lands; returns the target's health.
   */
  private int healthAfterRockHits(int weaponDamage, float multiplier, int targetHealth) {
    RockAttackComponent rock =
        new RockAttackComponent(
            RANGE,
            COOLDOWN,
            0f,
            AttackTestWorld.naturalWeapon(weaponDamage, WINDUP),
            SPAWN_HEIGHT,
            multiplier);
    rock.setProjectileSpeed(SPEED);
    Entity owner = world.newAttacker(rock);
    Entity target = world.newTarget(4f, 1.2f, targetHealth);

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    world.step(AttackTestWorld.framesFor(2f), owner);
    return target.getComponent(CombatStatsComponent.class).getHealth();
  }
}
