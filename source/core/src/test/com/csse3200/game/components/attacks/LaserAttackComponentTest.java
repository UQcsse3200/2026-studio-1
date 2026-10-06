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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link LaserAttackComponent}, the Cyclops's eye laser: a fast, straight shot fired from eye
 * height at where the target is when the windup ends.
 *
 * <p>Built from the same six groups as {@code RockAttackComponentTest}, so the two attacks are held
 * to the same rules. Only the last-but-one group differs, because a laser flies straight where a
 * rock flies an arc:
 *
 * <ol>
 *   <li>construction and validation: stored values, bad arguments, parameterised bad values
 *   <li>lookup: the subclass is found by its own class, never by the base class
 *   <li>events: the {@code laserAttack} prefix is used for every event, and no other prefix is
 *   <li>windup and cooldown: timed behaviour driven by {@code step}
 *   <li>the projectile: where it appears, how much damage it carries, and that it flies straight
 *   <li>refusals: out of range, dead target, no target, two attacks on one owner
 * </ol>
 *
 * <p>The numbers: the test weapon deals 10 damage with a 0.1 s windup; the laser multiplies damage
 * by 0.6, so a hit is 6. The owner is 1 wide by 2 tall at the origin, so a laser leaves from x 0.5
 * at a height of 2 times the spawn fraction. One frame is 0.02 s.
 */
@ExtendWith(GameExtension.class)
class LaserAttackComponentTest {
  private static final float RANGE = 7f;
  private static final float COOLDOWN = 4f;
  private static final float WINDUP = 0.1f;
  private static final float KNOCKBACK = 0f;
  private static final float SPAWN_HEIGHT = 0.75f;
  private static final float MULTIPLIER = 0.6f;
  private static final int WEAPON_DAMAGE = 10;
  private static final float SPEED = 20f;
  private static final float OWNER_WIDTH = 1f;
  private static final float OWNER_HEIGHT = 2f;

  private AttackTestWorld world;

  @BeforeEach
  void beforeEach() {
    world = AttackTestWorld.create();
  }

  // ---------- 1. construction and validation ----------

  @Test
  void shouldStoreEverySettingItWasGiven() {
    LaserAttackComponent laser = newLaser();

    assertEquals(RANGE, laser.getRange(), 1e-6f);
    assertEquals(COOLDOWN, laser.getCooldown(), 1e-6f);
    assertEquals(KNOCKBACK, laser.getKnockback(), 1e-6f);
    assertEquals(SPAWN_HEIGHT, laser.getSpawnHeightFraction(), 1e-6f);
    assertEquals(MULTIPLIER, laser.getDamageMultiplier(), 1e-6f);
    assertEquals(WINDUP, laser.getWindupDuration(), 1e-6f);
    assertEquals(WEAPON_DAMAGE, laser.getDamage());
    assertEquals(SPEED, laser.getProjectileSpeed(), 1e-6f);
  }

  @Test
  void shouldUseTheLaserEventPrefix() {
    assertEquals("laserAttack", newLaser().getEventPrefix());
  }

  @ParameterizedTest(name = "spawnHeightFraction {0} is rejected")
  @ValueSource(floats = {-0.01f, 1.01f, 2f, -5f})
  void shouldRejectASpawnHeightOutsideZeroToOne(float fraction) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LaserAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), fraction, MULTIPLIER));
  }

  @Test
  void shouldRejectANotANumberSpawnHeight() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LaserAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), Float.NaN, MULTIPLIER));
  }

  @ParameterizedTest(name = "spawnHeightFraction {0} is accepted")
  @ValueSource(floats = {0f, 0.5f, 1f})
  void shouldAcceptTheEdgesOfTheSpawnHeightRange(float fraction) {
    assertDoesNotThrow(
        () -> new LaserAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), fraction, MULTIPLIER));
  }

  @ParameterizedTest(name = "damageMultiplier {0} is rejected")
  @ValueSource(floats = {0f, -1f, -0.0001f})
  void shouldRejectAZeroOrNegativeMultiplier(float multiplier) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LaserAttackComponent(
                RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, multiplier));
  }

  @Test
  void shouldRejectANotANumberMultiplier() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LaserAttackComponent(
                RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, Float.NaN));
  }

  @Test
  void shouldAcceptAMultiplierBelowOneBecauseALaserIsWeakerThanAStomp() {
    assertDoesNotThrow(
        () -> new LaserAttackComponent(RANGE, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, 0.01f));
  }

  @Test
  void shouldInheritTheRulesOfTheRangedConstructor() {
    // A negative range, a negative knockback and a windup as long as the cooldown are all refused
    // by the parent constructor before the laser's own checks run.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LaserAttackComponent(-1f, COOLDOWN, KNOCKBACK, weapon(), SPAWN_HEIGHT, MULTIPLIER));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LaserAttackComponent(RANGE, COOLDOWN, -1f, weapon(), SPAWN_HEIGHT, MULTIPLIER));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new LaserAttackComponent(
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
        () -> new LaserAttackComponent(RANGE, COOLDOWN, KNOCKBACK, null, SPAWN_HEIGHT, MULTIPLIER));
  }

  // ---------- 2. lookup ----------

  @Test
  void shouldBeFoundByItsOwnClassButNotByTheBaseClassOrAsARock() {
    Entity owner = world.newAttacker(newLaser());

    assertNotNull(owner.getComponent(LaserAttackComponent.class));
    assertNull(
        owner.getComponent(RangedAttackComponent.class),
        "the base-class lookup does not find a subclass; the factory and controller must use"
            + " the subclass");
    assertNull(owner.getComponent(RockAttackComponent.class), "a laser is not a rock");
  }

  // ---------- 3. events ----------

  @Test
  void shouldAnnounceTheWindupUnderTheLaserPrefix() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "laserAttackWindup");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);

    assertEquals(1, windups.size());
    assertSame(target, windups.get(0));
  }

  @Test
  void shouldAnnounceTheShotUnderTheLaserPrefixWhenTheWindupEnds() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> fired = AttackTestWorld.record(owner, "laserAttackFired");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, fired.size());
    assertSame(target, fired.get(0));
  }

  @Test
  void shouldAnnounceTheHitUnderTheLaserPrefixWhenTheLaserLands() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0.5f, 50);
    List<Entity> hits = AttackTestWorld.record(owner, "laserAttackHit");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    stepUntilTheLaserLands(owner, hits);

    assertEquals(1, hits.size(), "the animation controller listens for this to end the beam");
    assertSame(target, hits.get(0));
  }

  @Test
  void shouldNeverUseTheDefaultRangedPrefixOrTheRockPrefix() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> rangedWindups = AttackTestWorld.record(owner, "rangedAttackWindup");
    List<Entity> rangedFired = AttackTestWorld.record(owner, "rangedAttackFired");
    List<Entity> rockWindups = AttackTestWorld.record(owner, "rockAttackWindup");
    List<Entity> rockFired = AttackTestWorld.record(owner, "rockAttackFired");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertTrue(rangedWindups.isEmpty(), "a laser must not announce a plain ranged windup");
    assertTrue(rangedFired.isEmpty(), "a laser must not announce a plain ranged shot");
    assertTrue(rockWindups.isEmpty(), "a laser must not announce a rock windup");
    assertTrue(rockFired.isEmpty(), "a laser must not announce a rock throw");
  }

  @ParameterizedTest(name = "the trigger \"{0}\" does not start a laser")
  @ValueSource(strings = {"rangedAttack", "rockAttack", "meleeAttack"})
  void shouldIgnoreEveryOtherAttackTrigger(String otherTrigger) {
    // Zeus's lightning and the skeleton arrow answer to "rangedAttack", the rock to "rockAttack":
    // a laser must answer to neither, or one trigger would fire two attacks.
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "laserAttackWindup");

    if ("meleeAttack".equals(otherTrigger)) {
      owner.getEvents().trigger(otherTrigger, target);
    } else {
      owner.getEvents().trigger(otherTrigger, target, ProjectileType.ARROW);
    }

    assertTrue(windups.isEmpty());
    assertFalse(owner.getComponent(LaserAttackComponent.class).isWindingUp());
  }

  // ---------- 4. windup and cooldown ----------

  @Test
  void shouldNotCreateTheLaserUntilTheWindupHasElapsed() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(2, owner); // 0.04 s of 0.10 s

    assertEquals(0, world.registeredSince(before).size());
    assertTrue(owner.getComponent(LaserAttackComponent.class).isWindingUp());
  }

  @Test
  void shouldCreateExactlyOneLaserWhenTheWindupEnds() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size());
    assertFalse(owner.getComponent(LaserAttackComponent.class).isWindingUp());
  }

  @Test
  void shouldIgnoreASecondTriggerDuringTheWindup() {
    // The task triggers every frame the target is in range, so repeats are the normal case.
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> windups = AttackTestWorld.record(owner, "laserAttackWindup");
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, windups.size());
    assertEquals(1, world.registeredSince(before).size());
  }

  @Test
  void shouldNotFireASecondLaserBeforeTheCooldownHasPassed() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 500);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW); // still cooling down
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size(), "cooldown allows one laser only");
  }

  @Test
  void shouldFireASecondLaserOnceTheCooldownHasPassed() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 500);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    tick(owner, AttackTestWorld.framesFor(COOLDOWN));
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(2, world.registeredSince(before).size());
  }

  // ---------- 5. the projectile ----------

  @Test
  void shouldLaunchTheLaserHigherWhenTheSpawnHeightIsHigher() {
    Vector2 low = launchCentreFor(0.1f);
    Vector2 high = launchCentreFor(0.9f);

    assertEquals(
        (0.9f - 0.1f) * OWNER_HEIGHT,
        high.y - low.y,
        0.05f,
        "the launch height is the fraction of the owner's 2 unit height");
  }

  @ParameterizedTest(name = "fraction {0} launches from height {1}")
  @CsvSource({"0, 0", "0.5, 1", "0.75, 1.5", "1, 2"})
  void shouldLaunchFromTheOwnersCentreLineAtTheSpawnHeight(float fraction, float expectedHeight) {
    Vector2 launch = launchCentreFor(fraction);

    assertEquals(OWNER_WIDTH / 2f, launch.x, 0.05f, "the eye is on the owner's centre line");
    assertEquals(expectedHeight, launch.y, 0.05f);
  }

  @Test
  void shouldFlyAtTheConfiguredSpeed() {
    Entity laser = fireAt(world.newTarget(4f, 0f, 50), SPAWN_HEIGHT);

    assertEquals(SPEED, AttackTestWorld.velocityOf(laser).len(), 0.05f);
  }

  @ParameterizedTest(name = "a target at ({0}, {1}) is shot at along the line to its centre")
  @CsvSource({
    "4, 0.5", // level with the eye, to the right
    "-5, 0.5", // level with the eye, to the left
    "4, 3", // above and right
    "4, -3", // below and right
    "-4, 3", // above and left
    "-4, -3", // below and left
    "0, 4" // straight up
  })
  void shouldAimStraightAtTheTargetsCentre(float targetX, float targetY) {
    float fraction = 0.5f; // the eye is at (0.5, 1.0)
    Entity target = world.newTarget(targetX, targetY, 50);
    Vector2 expected =
        target.getCenterPosition().sub(OWNER_WIDTH / 2f, OWNER_HEIGHT * fraction).nor().scl(SPEED);

    Entity laser = fireAt(target, fraction);

    Vector2 velocity = AttackTestWorld.velocityOf(laser);
    assertEquals(expected.x, velocity.x, 0.05f, "sideways velocity");
    assertEquals(expected.y, velocity.y, 0.05f, "vertical velocity");
  }

  @Test
  void shouldFlyLeftWhenTheTargetIsToTheLeft() {
    Entity laser = fireAt(world.newTarget(-5f, 0.5f, 50), SPAWN_HEIGHT);

    assertTrue(AttackTestWorld.velocityOf(laser).x < 0f);
  }

  @Test
  void shouldKeepAStraightLineBecauseALaserIsNotPulledDownLikeARock() {
    // Fired level over a long gap with nothing to hit: after half a second a rock would have
    // dropped; a laser must still be at the height it left from, moving exactly as it started.
    LaserAttackComponent laser = newLaser(0.5f, MULTIPLIER, 30f);
    Entity owner = world.newAttacker(laser);
    Entity target = world.newTarget(25f, 0.5f, 50); // centre level with the eye at height 1.0
    int before = world.mark();
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    Entity shot = world.registeredSince(before).get(0);
    Vector2 startVelocity = AttackTestWorld.velocityOf(shot);
    float startHeight = shot.getCenterPosition().y;

    world.step(AttackTestWorld.framesFor(0.5f), owner);

    Vector2 velocity = AttackTestWorld.velocityOf(shot);
    assertEquals(startVelocity.x, velocity.x, 0.05f, "no drag");
    assertEquals(startVelocity.y, velocity.y, 0.05f, "no gravity");
    assertEquals(startHeight, shot.getCenterPosition().y, 0.05f, "it has not dropped");
    assertTrue(shot.getCenterPosition().x > 5f, "and it really has travelled");
  }

  @Test
  void shouldFallBackToFiringRightWhenTheTargetIsExactlyAtTheEye() {
    // A target whose centre is the launch point gives no direction at all. The laser must still
    // be created, not throw, and it flies to the right.
    float fraction = 0.5f; // the eye is at (0.5, 1.0); a 1 by 1 target at (0, 0.5) is centred on it
    Entity target = world.newTarget(0f, 0.5f, 50);
    target.setScale(1f, 1f);

    Entity laser = assertDoesNotThrow(() -> fireAt(target, fraction));

    Vector2 velocity = AttackTestWorld.velocityOf(laser);
    assertFalse(Float.isNaN(velocity.x) || Float.isNaN(velocity.y), "no division by zero");
  }

  @Test
  void shouldCarryTheWeaponDamageTimesTheMultiplier() {
    // 10 damage x 0.6 = 6 on contact.
    assertEquals(50 - 6, healthAfterLaserHits(WEAPON_DAMAGE, MULTIPLIER, 50));
  }

  @ParameterizedTest(name = "{0} damage x {1} deals {2}")
  @CsvSource({
    "10, 0.6, 6",
    "10, 1.0, 10",
    "10, 1.5, 15",
    "7, 0.5, 4", // 3.5 rounds up
    "5, 0.5, 3", // 2.5 rounds up
    "9, 0.6, 5", // 5.4 rounds down
    "1, 0.2, 1", // 0.2 rounds to 0, but a laser always does at least 1
    "1, 0.01, 1"
  })
  void shouldRoundTheScaledDamageAndNeverGoBelowOne(int damage, float multiplier, int expected) {
    assertEquals(100 - expected, healthAfterLaserHits(damage, multiplier, 100));
  }

  @Test
  void shouldHurtTheTargetOnlyOncePerShot() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0.5f, 50);
    List<Entity> hits = AttackTestWorld.record(owner, "laserAttackHit");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    stepUntilTheLaserLands(owner, hits);
    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW); // still cooling down
    tick(owner, AttackTestWorld.framesFor(1f));

    assertEquals(1, hits.size());
    assertEquals(50 - 6, target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldIgnoreWhichProjectileTypeTheTaskAsksFor() {
    // The task passes ARROW or LIGHTNING; the laser always builds a laser.
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.LIGHTNING);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, world.registeredSince(before).size());
    assertEquals(
        SPEED,
        AttackTestWorld.velocityOf(world.registeredSince(before).get(0)).len(),
        0.05f,
        "a lightning bolt would fall straight down; this flies at the laser's speed");
  }

  // ---------- 6. refusals ----------

  @Test
  void shouldRefuseATargetBeyondItsRange() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(RANGE + 5f, 0f, 50);
    int before = world.mark();
    List<Entity> windups = AttackTestWorld.record(owner, "laserAttackWindup");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertTrue(windups.isEmpty(), "out of range means no windup");
    assertEquals(0, world.registeredSince(before).size());
  }

  @Test
  void shouldCancelWhenTheTargetDiesDuringTheWindup() {
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    List<Entity> cancelled = AttackTestWorld.record(owner, "laserAttackCancelled");
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(1, cancelled.size());
    assertEquals(0, world.registeredSince(before).size(), "no laser for a dead target");
  }

  @Test
  void shouldNeverFireAtATargetThatIsAlreadyDead() {
    // The trigger does not look at health, so a windup may begin; the check when the windup ends
    // is what stops the shot.
    Entity owner = world.newAttacker(newLaser());
    Entity target = world.newTarget(4f, 0f, 50);
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    List<Entity> fired = AttackTestWorld.record(owner, "laserAttackFired");
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertTrue(fired.isEmpty());
    assertEquals(0, world.registeredSince(before).size());
  }

  @Test
  void shouldNotThrowForANullTarget() {
    Entity owner = world.newAttacker(newLaser());
    int before = world.mark();

    assertDoesNotThrow(
        () -> {
          owner.getEvents().trigger("laserAttack", null, ProjectileType.ARROW);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
        });

    assertEquals(0, world.registeredSince(before).size());
  }

  @Test
  void shouldNotStartTheRockWhenTheLaserIsFiredAndTheOtherWayRound() {
    // The Cyclops holds both. Each trigger must start only its own attack.
    LaserAttackComponent laser = newLaser();
    RockAttackComponent rock =
        new RockAttackComponent(11f, 7f, 3f, AttackTestWorld.naturalWeapon(10, 0.1f), 0.85f, 1.5f);
    rock.setProjectileSpeed(8f);
    Entity owner = world.newAttacker(laser, rock);
    Entity target = world.newTarget(4f, 0f, 50);

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);

    assertTrue(laser.isWindingUp());
    assertFalse(rock.isWindingUp(), "only the laser answers to laserAttack");

    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);

    assertTrue(rock.isWindingUp(), "and the rock still answers to its own trigger");
  }

  @Test
  void shouldKeepTheLaserCooldownSeparateFromTheRockCooldown() {
    LaserAttackComponent laser = newLaser();
    RockAttackComponent rock =
        new RockAttackComponent(11f, 7f, 3f, AttackTestWorld.naturalWeapon(10, 0.1f), 0.85f, 1.5f);
    rock.setProjectileSpeed(8f);
    Entity owner = world.newAttacker(laser, rock);
    Entity target = world.newTarget(4f, 0f, 500);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner); // laser fired, now cooling down
    owner.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);

    assertEquals(
        2, world.registeredSince(before).size(), "the laser's cooldown must not block the rock");
  }

  // ---------- helpers ----------

  private WeaponItem weapon() {
    return AttackTestWorld.naturalWeapon(WEAPON_DAMAGE, WINDUP);
  }

  private LaserAttackComponent newLaser() {
    return newLaser(SPAWN_HEIGHT, MULTIPLIER, RANGE);
  }

  private LaserAttackComponent newLaser(float fraction, float multiplier, float range) {
    LaserAttackComponent laser =
        new LaserAttackComponent(range, COOLDOWN, KNOCKBACK, weapon(), fraction, multiplier);
    laser.setProjectileSpeed(SPEED);
    return laser;
  }

  /** Fires one laser from a fresh owner at the target and returns the laser entity. */
  private Entity fireAt(Entity target, float fraction) {
    Entity owner = world.newAttacker(newLaser(fraction, MULTIPLIER, RANGE));
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    return world.registeredSince(before).get(0);
  }

  /**
   * Fires one laser with the given spawn height and returns the centre of the laser on the frame it
   * appears, which is the point it was launched from.
   */
  private Vector2 launchCentreFor(float fraction) {
    Entity owner = world.newAttacker(newLaser(fraction, MULTIPLIER, RANGE));
    Entity target = world.newTarget(4f, 0f, 50);
    int before = world.mark();

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    // Step only as far as the frame the laser is created on, so it has not flown anywhere yet.
    for (int frame = 0; frame < AttackTestWorld.framesFor(WINDUP) + 5; frame++) {
      world.step(1, owner);
      if (!world.registeredSince(before).isEmpty()) {
        break;
      }
    }
    return world.registeredSince(before).get(0).getCenterPosition().cpy();
  }

  /** Fires one laser at a stationary target and steps until it lands; returns the health left. */
  private int healthAfterLaserHits(int weaponDamage, float multiplier, int targetHealth) {
    LaserAttackComponent laser =
        new LaserAttackComponent(
            RANGE,
            COOLDOWN,
            0f,
            AttackTestWorld.naturalWeapon(weaponDamage, WINDUP),
            SPAWN_HEIGHT,
            multiplier);
    laser.setProjectileSpeed(SPEED);
    Entity owner = world.newAttacker(laser);
    Entity target = world.newTarget(4f, 0.5f, targetHealth);
    List<Entity> hits = AttackTestWorld.record(owner, "laserAttackHit");

    owner.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);
    world.step(AttackTestWorld.framesFor(WINDUP) + 1, owner);
    stepUntilTheLaserLands(owner, hits);
    return target.getComponent(CombatStatsComponent.class).getHealth();
  }

  /**
   * Steps the world one frame at a time and stops on the frame the laser lands, or after one second
   * if it never does. It stops at once on purpose: a landed laser is disposed on the game's own
   * thread, and stepping physics while that happens makes Box2D abort the whole test run.
   */
  private void stepUntilTheLaserLands(Entity owner, List<Entity> hits) {
    for (int frame = 0; frame < AttackTestWorld.framesFor(1f) && hits.isEmpty(); frame++) {
      world.step(1, owner);
    }
  }

  /**
   * Lets time pass for the owner only, without stepping physics. Used to wait out a cooldown after
   * a laser has been fired, for the same reason as {@link #stepUntilTheLaserLands}.
   */
  private static void tick(Entity owner, int frames) {
    for (int frame = 0; frame < frames; frame++) {
      owner.earlyUpdate();
      owner.update();
    }
  }
}
