package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests how {@link ChargeComponent} drives {@link TouchAttackComponent}: the reworked charge, where
 * a Minotaur or Centaur does no touch damage while winding up, lands one boosted hit when the rush
 * begins, and ends the rush on that hit.
 *
 * <p>The existing {@code ChargeComponentTest} covers construction, timing and movement; this class
 * covers only the damage and the ending of a charge, so the two do not overlap.
 *
 * <p>Fixture: an attacker with base attack 10, a hitbox, a movement controller and a charge of 1.0
 * s with a 0.2 s windup, a 1.0 s cooldown, damage x1.5 and speed x2. The target (100 health, on the
 * layer the touch attack looks for) overlaps the attacker, so it is "already touching" when the
 * rush starts. Frames are 0.02 s: the windup ends on frame 10, so 11 frames guarantees the rush
 * began.
 */
@ExtendWith(GameExtension.class)
class ChargeComponentTouchDamageTest {
  private static final float DELTA = 0.02f;
  private static final float CHARGE = 1.0f;
  private static final float WINDUP = 0.2f;
  private static final float COOLDOWN = 1.0f;
  private static final float DAMAGE_MULTIPLIER = 1.5f;
  private static final float SPEED_MULTIPLIER = 2f;
  private static final int BASE_ATTACK = 10;
  private static final int WINDUP_AND_ONE_FRAMES = 11;
  private static final Vector2 FAR_TARGET = new Vector2(20f, 0f);

  @BeforeEach
  void beforeEach() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(DELTA);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  // ---------- set-up done by create() ----------

  @Test
  void shouldSwitchTouchDamageOffAndLimitItToOneHitWhenCreated() {
    Entity attacker = createAttacker();

    TouchAttackComponent touch = attacker.getComponent(TouchAttackComponent.class);

    assertFalse(touch.isActive(), "no damage until a rush begins");
    assertEquals(1, touch.getMaxHitsPerActivation(), "one hit per charge");
  }

  @Test
  void shouldDoNothingHarmfulWhenThereIsNoTouchAttack() {
    Entity noTouch =
        new Entity()
            .addComponent(new CombatStatsComponent(0, BASE_ATTACK))
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(newCharge());
    noTouch.create();

    assertDoesNotThrow(
        () -> {
          noTouch.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
          step(noTouch, WINDUP_AND_ONE_FRAMES + 5);
        });
  }

  // ---------- no damage while winding up ----------

  @Test
  void shouldNotHurtAnOverlappingTargetDuringTheWindup() {
    Entity attacker = createAttacker();
    Entity target = createTarget(100);
    stepWorld(2); // contact exists

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, 5); // 0.1 s of a 0.2 s windup

    assertTrue(attacker.getComponent(ChargeComponent.class).isWindup());
    assertEquals(100, healthOf(target));
  }

  // ---------- the boosted hit ----------

  @Test
  void shouldHitAnAlreadyTouchingTargetWhenTheRushBegins() {
    Entity attacker = createAttacker();
    Entity target = createTarget(100);
    stepWorld(2);

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(100 - (int) (BASE_ATTACK * DAMAGE_MULTIPLIER), healthOf(target));
    assertEquals(1, attacker.getComponent(TouchAttackComponent.class).getHitsThisActivation());
  }

  @Test
  void shouldApplyTheChargeMultiplierOnlyDuringTheRush() {
    Entity attacker = createAttacker(); // nothing to hit, so the rush lasts
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    TouchAttackComponent touch = attacker.getComponent(TouchAttackComponent.class);
    assertEquals(1.0f, touch.getDamageMultiplier(), 1e-6f);

    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES - 3);
    assertEquals(1.0f, touch.getDamageMultiplier(), 1e-6f, "not yet: still in the windup");

    step(attacker, 4);
    assertTrue(charge.isRushing());
    assertEquals(DAMAGE_MULTIPLIER, touch.getDamageMultiplier(), 1e-6f, "boosted during the rush");

    charge.endCharge();
    assertEquals(1.0f, touch.getDamageMultiplier(), 1e-6f, "back to one once the charge is over");
  }

  @Test
  void shouldReportTheChargeMultiplierWhileChargingAndOneAfterwards() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    assertEquals(1.0f, charge.getDamageMultiplier(), 1e-6f);

    charge.startCharge(FAR_TARGET);

    assertEquals(DAMAGE_MULTIPLIER, charge.getDamageMultiplier(), 1e-6f);
  }

  @ParameterizedTest(name = "base attack {0} x1.5 deals {1}")
  @CsvSource({"10, 15", "5, 7", "7, 10", "20, 30", "1, 1"})
  void shouldScaleTheHitByTheChargeMultiplierAndRoundDown(int baseAttack, int expected) {
    Entity attacker = createAttacker(baseAttack);
    Entity target = createTarget(100);
    stepWorld(2);

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(100 - expected, healthOf(target));
  }

  // ---------- ending the charge on a hit ----------

  @Test
  void shouldEndTheChargeOnTheHitWhenEndOnHitIsOn() {
    Entity attacker = createAttacker();
    createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    AtomicInteger ended = new AtomicInteger();
    attacker.getEvents().addListener("chargeEnd", (EventListener0) ended::incrementAndGet);

    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertFalse(charge.isCharging());
    assertEquals(1, ended.get(), "chargeEnd fires exactly once, not once for the hit and again");
    assertFalse(attacker.getComponent(TouchAttackComponent.class).isActive());
  }

  @Test
  void shouldStartTheCooldownWhenAHitEndsTheCharge() {
    Entity attacker = createAttacker();
    createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);

    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertFalse(charge.canCharge(), "cooling down straight after the hit");
    step(attacker, (int) (COOLDOWN / DELTA) + 2);
    assertTrue(charge.canCharge(), "ready again after the cooldown");
  }

  @Test
  void shouldRestoreSpeedAndStopMovingWhenAHitEndsTheCharge() {
    Entity attacker = createAttacker();
    createTarget(100);
    stepWorld(2);
    PhysicsMovementComponent movement = attacker.getComponent(PhysicsMovementComponent.class);

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(1.0f, movement.getSpeedMultiplier(), 1e-6f);
    assertFalse(movement.getMoving());
  }

  @Test
  void shouldKeepRushingAfterAHitWhenEndOnHitIsOff() {
    Entity attacker = createAttacker();
    attacker.getComponent(ChargeComponent.class).setEndOnHit(false);
    Entity target = createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);

    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(85, healthOf(target), "the hit still lands");
    assertTrue(charge.isRushing(), "but the rush carries on");
  }

  @Test
  void shouldLandOnlyOneHitPerChargeEvenWhenEndOnHitIsOff() {
    Entity attacker = createAttacker();
    attacker.getComponent(ChargeComponent.class).setEndOnHit(false);
    Entity first = createTarget(100);
    Entity second = createTarget(100);
    stepWorld(2);

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);
    touch(attacker, second); // a later contact in the same rush
    touch(attacker, first);

    int hurt = (100 - healthOf(first) > 0 ? 1 : 0) + (100 - healthOf(second) > 0 ? 1 : 0);
    assertEquals(1, hurt, "the hit limit of one per charge holds");
  }

  @Test
  void shouldHitAgainOnTheNextChargeAfterTheCooldown() {
    Entity attacker = createAttacker();
    Entity target = createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);
    assertEquals(85, healthOf(target));
    step(attacker, (int) (COOLDOWN / DELTA) + 2);

    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(70, healthOf(target), "a fresh charge gets a fresh hit");
  }

  // ---------- charges that do not hit ----------

  @Test
  void shouldEndAfterTheFullDurationWhenNothingIsHit() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    AtomicInteger ended = new AtomicInteger();
    attacker.getEvents().addListener("chargeEnd", (EventListener0) ended::incrementAndGet);

    charge.startCharge(FAR_TARGET);
    step(attacker, (int) (CHARGE / DELTA) + 3);

    assertFalse(charge.isCharging());
    assertEquals(1, ended.get());
    assertFalse(attacker.getComponent(TouchAttackComponent.class).isActive());
  }

  @Test
  void shouldStillBeChargingJustBeforeTheDurationEnds() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);

    charge.startCharge(FAR_TARGET);
    step(attacker, (int) (CHARGE / DELTA) - 5);

    assertTrue(charge.isCharging());
  }

  @Test
  void shouldNotHurtATargetOnTheWrongLayer() {
    Entity attacker = createAttacker();
    Entity wrongLayer = createTarget(100, (short) (1 << 6));
    stepWorld(2);

    attacker.getComponent(ChargeComponent.class).startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertEquals(100, healthOf(wrongLayer));
  }

  // ---------- cancelling ----------

  @Test
  void shouldDoNothingWhenEndChargeIsCalledWhileNotCharging() {
    Entity attacker = createAttacker();
    AtomicInteger ended = new AtomicInteger();
    attacker.getEvents().addListener("chargeEnd", (EventListener0) ended::incrementAndGet);

    assertDoesNotThrow(() -> attacker.getComponent(ChargeComponent.class).endCharge());

    assertEquals(0, ended.get());
  }

  @Test
  void shouldCancelCleanlyWhenEndChargeIsCalledDuringTheWindup() {
    Entity attacker = createAttacker();
    Entity target = createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    charge.startCharge(FAR_TARGET);
    step(attacker, 3);

    charge.endCharge();
    step(attacker, WINDUP_AND_ONE_FRAMES);

    assertFalse(charge.isCharging());
    assertEquals(100, healthOf(target), "a cancelled windup never reaches the rush");
  }

  @Test
  void shouldNotStartAChargeWhileOnCooldown() {
    Entity attacker = createAttacker();
    createTarget(100);
    stepWorld(2);
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES); // hit, charge over, cooling down
    AtomicInteger started = new AtomicInteger();
    attacker.getEvents().addListener("chargeStart", (EventListener0) started::incrementAndGet);

    charge.startCharge(FAR_TARGET);

    assertEquals(0, started.get());
    assertFalse(charge.isCharging());
  }

  @Test
  void shouldSwitchTouchDamageOffWhenDisposed() {
    Entity attacker = createAttacker();
    attacker.getComponent(TouchAttackComponent.class).setActive(true);

    attacker.getComponent(ChargeComponent.class).dispose();

    assertFalse(attacker.getComponent(TouchAttackComponent.class).isActive());
  }

  // ---------- a blocked rush (hazard avoidance) ----------
  // These need the "movementBlocked" listener from hazard_specs/05_wiring.md. Until that is
  // written, these three fail on purpose: they are the acceptance test for it.

  @Test
  void shouldEndTheRushWhenMovementIsBlocked() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    charge.startCharge(FAR_TARGET);
    step(attacker, WINDUP_AND_ONE_FRAMES); // now rushing, nothing hit
    assertTrue(charge.isRushing());

    attacker.getEvents().trigger("movementBlocked");

    assertFalse(charge.isCharging(), "a rush stopped by a hazard ends instead of burning its time");
    assertFalse(charge.canCharge(), "and the cooldown starts");
  }

  @Test
  void shouldIgnoreMovementBlockedDuringTheWindup() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    charge.startCharge(FAR_TARGET);
    step(attacker, 3);

    attacker.getEvents().trigger("movementBlocked");

    assertTrue(charge.isWindup(), "the enemy is standing still anyway, so the windup goes on");
  }

  @Test
  void shouldIgnoreMovementBlockedWhenNotCharging() {
    Entity attacker = createAttacker();
    ChargeComponent charge = attacker.getComponent(ChargeComponent.class);
    AtomicInteger ended = new AtomicInteger();
    attacker.getEvents().addListener("chargeEnd", (EventListener0) ended::incrementAndGet);

    assertDoesNotThrow(() -> attacker.getEvents().trigger("movementBlocked"));

    assertEquals(0, ended.get());
    assertTrue(charge.canCharge());
  }

  // ---------- helpers ----------

  private ChargeComponent newCharge() {
    return new ChargeComponent(CHARGE, WINDUP, COOLDOWN, DAMAGE_MULTIPLIER, SPEED_MULTIPLIER);
  }

  private Entity createAttacker() {
    return createAttacker(BASE_ATTACK);
  }

  /**
   * An attacker at the origin: touch attack on the player layer, hitbox, movement controller and a
   * charge, with gravity off so only the charge moves it.
   */
  private Entity createAttacker(int baseAttack) {
    Entity attacker =
        new Entity()
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(0, baseAttack))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(newCharge());
    attacker.setScale(1f, 1f);
    attacker.create();
    attacker.setPosition(0f, 0f);
    attacker.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    attacker.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return attacker;
  }

  private Entity createTarget(int health) {
    return createTarget(health, PhysicsLayer.PLAYER);
  }

  private Entity createTarget(int health, short layer) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    target.setScale(1f, 1f);
    target.create();
    target.setPosition(0.1f, 0f);
    target.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return target;
  }

  private int healthOf(Entity entity) {
    return entity.getComponent(CombatStatsComponent.class).getHealth();
  }

  /** Triggers the collision event the physics world would, between the attacker and a target. */
  private void touch(Entity attacker, Entity target) {
    Fixture mine = attacker.getComponent(HitboxComponent.class).getFixture();
    Fixture theirs = target.getComponent(HitboxComponent.class).getFixture();
    attacker.getEvents().trigger("collisionStart", mine, theirs);
  }

  private void stepWorld(int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
    }
  }

  /** One frame: the world steps, then the attacker updates (the charge and movement run in it). */
  private void step(Entity attacker, int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      attacker.earlyUpdate();
      attacker.update();
    }
  }
}
