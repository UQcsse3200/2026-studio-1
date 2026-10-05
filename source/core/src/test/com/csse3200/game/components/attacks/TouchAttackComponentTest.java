package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link TouchAttackComponent}: contact damage with an on/off switch, a per-activation hit
 * limit, a damage multiplier, knockback, and {@code activate()} catching a target already inside.
 *
 * <p>This REPLACES the three-case file of the same name; the original three cases are kept first
 * (renamed only in the group heading). Two styles are used on purpose:
 *
 * <ul>
 *   <li><b>Event style</b>, for rules: the collision event is triggered by hand with two fixtures,
 *       so no physics stepping is needed and the result is exact.
 *   <li><b>Real-contact style</b>, for {@code activate()} and knockback: two real bodies overlap in
 *       a real physics world, and the world is stepped so the contact exists.
 * </ul>
 *
 * <p>The attacker has base attack 10; the target has 100 health unless a test says otherwise. Layer
 * bit 1 << 3 is used for the target, as in the original tests.
 */
@ExtendWith(GameExtension.class)
class TouchAttackComponentTest {
  private static final short TARGET_LAYER = (1 << 3);
  private static final int BASE_ATTACK = 10;

  @BeforeEach
  void beforeEach() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  // ---------- the original three cases ----------

  @Test
  void shouldAttack() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 10);

    touch(attacker, target);

    assertEquals(0, healthOf(target));
  }

  @Test
  void shouldNotAttackOtherLayer() {
    short attackLayer = (1 << 4);
    Entity attacker = createAttacker(attackLayer, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 10);

    touch(attacker, target);

    assertEquals(10, healthOf(target));
  }

  @Test
  void shouldNotAttackWithoutCombatComponent() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(TARGET_LAYER));
    target.create();

    assertDoesNotThrow(() -> touch(attacker, target));
  }

  // ---------- on/off switch ----------

  @Test
  void shouldBeActiveByDefault() {
    assertTrue(touchOf(createAttacker(TARGET_LAYER, 0, BASE_ATTACK)).isActive());
  }

  @Test
  void shouldIgnoreContactsWhileInactive() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setActive(false);

    touch(attacker, target);

    assertFalse(touchOf(attacker).isActive());
    assertEquals(100, healthOf(target));
  }

  @Test
  void shouldHitAgainAfterBeingSwitchedBackOn() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setActive(false);
    touch(attacker, target);

    touchOf(attacker).setActive(true);
    touch(attacker, target);

    assertEquals(90, healthOf(target));
  }

  @Test
  void shouldNotResetTheHitCounterWhenSwitchedOffAndOn() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setMaxHitsPerActivation(1);
    touch(attacker, target);

    touchOf(attacker).setActive(false);
    touchOf(attacker).setActive(true);
    touch(attacker, target);

    assertEquals(90, healthOf(target), "setActive keeps the count, so the limit still blocks");
    assertEquals(1, touchOf(attacker).getHitsThisActivation());
  }

  @Test
  void shouldBeSafeToDeactivateTwice() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);

    assertDoesNotThrow(
        () -> {
          touchOf(attacker).deactivate();
          touchOf(attacker).deactivate();
        });

    assertFalse(touchOf(attacker).isActive());
  }

  // ---------- hit limit ----------

  @Test
  void shouldHaveNoHitLimitByDefault() {
    assertEquals(
        0, touchOf(createAttacker(TARGET_LAYER, 0, BASE_ATTACK)).getMaxHitsPerActivation());
  }

  @Test
  void shouldAllowUnlimitedHitsWhenTheLimitIsZero() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);

    for (int i = 0; i < 5; i++) {
      touch(attacker, target);
    }

    assertEquals(50, healthOf(target));
    assertEquals(5, touchOf(attacker).getHitsThisActivation());
  }

  @ParameterizedTest(name = "limit {0}: {1} contacts cause {2} hits")
  @CsvSource({
    "1, 3, 1",
    "2, 5, 2",
    "3, 3, 3",
    "3, 2, 2", // fewer contacts than the limit
    "5, 10, 5"
  })
  void shouldStopHittingOnceTheLimitIsReached(int limit, int contacts, int expectedHits) {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 1000);
    touchOf(attacker).setMaxHitsPerActivation(limit);

    for (int i = 0; i < contacts; i++) {
      touch(attacker, target);
    }

    assertEquals(1000 - expectedHits * BASE_ATTACK, healthOf(target));
    assertEquals(expectedHits, touchOf(attacker).getHitsThisActivation());
  }

  @Test
  void shouldRejectANegativeLimit() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);

    assertThrows(
        IllegalArgumentException.class, () -> touchOf(attacker).setMaxHitsPerActivation(-1));
  }

  @Test
  void shouldNotCountAContactWithATargetThatCannotBeHurt() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    touchOf(attacker).setMaxHitsPerActivation(1);
    Entity noStats =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(TARGET_LAYER));
    noStats.create();
    Entity realTarget = createTarget(TARGET_LAYER, 100);

    touch(attacker, noStats);
    touch(attacker, realTarget);

    assertEquals(
        90, healthOf(realTarget), "the wasted contact must not use up the one allowed hit");
  }

  @Test
  void shouldAnnounceEachHitWithTheTarget() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    List<Entity> hits = new ArrayList<>();
    attacker.getEvents().addListener("touchAttackHit", (EventListener1<Entity>) hits::add);

    touch(attacker, target);

    assertEquals(1, hits.size());
    assertSame(target, hits.get(0));
  }

  @Test
  void shouldNotAnnounceAHitThatTheLimitBlocked() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setMaxHitsPerActivation(1);
    List<Entity> hits = new ArrayList<>();
    attacker.getEvents().addListener("touchAttackHit", (EventListener1<Entity>) hits::add);

    touch(attacker, target);
    touch(attacker, target);

    assertEquals(1, hits.size());
  }

  // ---------- damage multiplier ----------

  @Test
  void shouldHaveAMultiplierOfOneByDefault() {
    assertEquals(
        1f, touchOf(createAttacker(TARGET_LAYER, 0, BASE_ATTACK)).getDamageMultiplier(), 1e-6f);
  }

  @ParameterizedTest(name = "base {0} x {1} deals {2}")
  @CsvSource({
    "10, 1.5, 15",
    "10, 2.0, 20",
    "10, 3.0, 30",
    "5, 1.5, 7", // 7.5 rounds down
    "7, 1.5, 10", // 10.5 rounds down
    "10, 0.5, 5", // below one is allowed: only zero and negative are not
    "1, 1.5, 1"
  })
  void shouldScaleTheBaseAttackAndRoundDown(int baseAttack, float multiplier, int expectedDamage) {
    Entity attacker = createAttacker(TARGET_LAYER, 0, baseAttack);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setDamageMultiplier(multiplier);

    touch(attacker, target);

    assertEquals(100 - expectedDamage, healthOf(target));
  }

  @Test
  void shouldNeverChangeTheAttackersOwnBaseAttack() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setDamageMultiplier(3f);

    touch(attacker, target);

    assertEquals(BASE_ATTACK, attacker.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @ParameterizedTest(name = "multiplier {0} is rejected")
  @ValueSource(floats = {0f, -1f, -0.5f})
  void shouldRejectAZeroOrNegativeMultiplier(float multiplier) {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);

    assertThrows(
        IllegalArgumentException.class, () -> touchOf(attacker).setDamageMultiplier(multiplier));
  }

  @Test
  void shouldRejectANotANumberOrInfiniteMultiplier() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);

    assertThrows(
        IllegalArgumentException.class, () -> touchOf(attacker).setDamageMultiplier(Float.NaN));
    assertThrows(
        IllegalArgumentException.class,
        () -> touchOf(attacker).setDamageMultiplier(Float.POSITIVE_INFINITY));
  }

  @Test
  void shouldKeepTheOldMultiplierWhenANewOneIsRejected() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    touchOf(attacker).setDamageMultiplier(2f);

    assertThrows(IllegalArgumentException.class, () -> touchOf(attacker).setDamageMultiplier(-1f));

    assertEquals(2f, touchOf(attacker).getDamageMultiplier(), 1e-6f);
  }

  // ---------- activate() ----------

  @Test
  void shouldClearTheHitCounterAndSwitchOnWhenActivated() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setMaxHitsPerActivation(1);
    touch(attacker, target);
    touchOf(attacker).setActive(false);

    touchOf(attacker).activate();

    assertTrue(touchOf(attacker).isActive());
    assertEquals(0, touchOf(attacker).getHitsThisActivation());
  }

  @Test
  void shouldAllowAFreshHitAfterActivateEvenAfterTheLimitWasUsed() {
    Entity attacker = createAttacker(TARGET_LAYER, 0, BASE_ATTACK);
    Entity target = createTarget(TARGET_LAYER, 100);
    touchOf(attacker).setMaxHitsPerActivation(1);
    touch(attacker, target);
    touch(attacker, target); // blocked

    touchOf(attacker).activate();
    touch(attacker, target);

    assertEquals(80, healthOf(target));
  }

  @Test
  void shouldHitATargetThatIsAlreadyTouchingWhenActivated() {
    // A collision-start event only fires when shapes BEGIN to overlap, so a target already
    // inside would never be hit. activate() looks at the live contacts instead.
    Entity attacker = createPhysicalAttacker(0f, 0f);
    Entity target = createPhysicalTarget(0f, 0f, 100);
    touchOf(attacker).setActive(false);
    stepWorld(3); // the contact now exists, but touch damage is off

    assertEquals(100, healthOf(target));
    touchOf(attacker).activate();

    assertEquals(90, healthOf(target));
    assertEquals(1, touchOf(attacker).getHitsThisActivation());
  }

  @Test
  void shouldNotHitATargetThatIsNotTouchingWhenActivated() {
    Entity attacker = createPhysicalAttacker(0f, 0f);
    Entity target = createPhysicalTarget(50f, 50f, 100);
    touchOf(attacker).setActive(false);
    stepWorld(3);

    touchOf(attacker).activate();

    assertEquals(100, healthOf(target));
  }

  @Test
  void shouldNotHitAnAlreadyTouchingTargetOnTheWrongLayerWhenActivated() {
    Entity attacker = createPhysicalAttacker(0f, 0f);
    Entity wrongLayer = createPhysicalTarget(0f, 0f, 100, (short) (1 << 6));
    touchOf(attacker).setActive(false);
    stepWorld(3);

    touchOf(attacker).activate();

    assertEquals(100, healthOf(wrongLayer));
  }

  @Test
  void shouldRespectTheHitLimitWhenActivatingOverSeveralTouchingTargets() {
    Entity attacker = createPhysicalAttacker(0f, 0f);
    Entity first = createPhysicalTarget(0f, 0f, 100);
    Entity second = createPhysicalTarget(0f, 0f, 100);
    touchOf(attacker).setMaxHitsPerActivation(1);
    touchOf(attacker).setActive(false);
    stepWorld(3);

    touchOf(attacker).activate();

    int hurt = (100 - healthOf(first) > 0 ? 1 : 0) + (100 - healthOf(second) > 0 ? 1 : 0);
    assertEquals(1, hurt, "exactly one of the two touching targets takes the single allowed hit");
  }

  @Test
  void shouldNotThrowWhenActivatedWithoutAHitbox() {
    Entity noHitbox =
        new Entity()
            .addComponent(new TouchAttackComponent(TARGET_LAYER))
            .addComponent(new CombatStatsComponent(0, BASE_ATTACK))
            .addComponent(new PhysicsComponent());
    noHitbox.create();

    assertDoesNotThrow(() -> noHitbox.getComponent(TouchAttackComponent.class).activate());
  }

  // ---------- knockback ----------

  @Test
  void shouldPushARealTargetAwayFromTheAttackerWhenKnockbackIsSet() {
    Entity attacker = createPhysicalAttacker(0f, 0f, 5f);
    Entity target = createPhysicalTarget(0.2f, 0f, 100);
    touchOf(attacker).setActive(false);
    stepWorld(3);

    touchOf(attacker).activate();

    float vx = target.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    assertTrue(vx > 0f, "a target to the right is pushed right: " + vx);
  }

  @Test
  void shouldNotPushAnythingWhenKnockbackIsZero() {
    Entity attacker = createPhysicalAttacker(0f, 0f, 0f);
    Entity target = createPhysicalTarget(0.2f, 0f, 100);
    touchOf(attacker).setActive(false);
    stepWorld(3);
    target.getComponent(PhysicsComponent.class).getBody().setLinearVelocity(0f, 0f);

    touchOf(attacker).activate();

    float vx = target.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    assertEquals(0f, vx, 1e-4f);
  }

  // ---------- helpers ----------

  /** Triggers the collision event the physics world would, with both entities' hitbox fixtures. */
  private void touch(Entity attacker, Entity target) {
    Fixture attackerFixture = attacker.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    attacker.getEvents().trigger("collisionStart", attackerFixture, targetFixture);
  }

  private TouchAttackComponent touchOf(Entity attacker) {
    return attacker.getComponent(TouchAttackComponent.class);
  }

  private int healthOf(Entity entity) {
    return entity.getComponent(CombatStatsComponent.class).getHealth();
  }

  private Entity createAttacker(short targetLayer, int health, int baseAttack) {
    Entity attacker =
        new Entity()
            .addComponent(new TouchAttackComponent(targetLayer))
            .addComponent(new CombatStatsComponent(health, baseAttack))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    attacker.create();
    return attacker;
  }

  private Entity createTarget(short layer, int health) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    target.create();
    return target;
  }

  private Entity createPhysicalAttacker(float x, float y) {
    return createPhysicalAttacker(x, y, 0f);
  }

  private Entity createPhysicalAttacker(float x, float y, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(new TouchAttackComponent(TARGET_LAYER, knockback))
            .addComponent(new CombatStatsComponent(0, BASE_ATTACK))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    attacker.setScale(1f, 1f);
    attacker.create();
    attacker.setPosition(x, y);
    attacker.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return attacker;
  }

  private Entity createPhysicalTarget(float x, float y, int health) {
    return createPhysicalTarget(x, y, health, TARGET_LAYER);
  }

  private Entity createPhysicalTarget(float x, float y, int health, short layer) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    target.setScale(1f, 1f);
    target.create();
    target.setPosition(x, y);
    target.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return target;
  }

  private void stepWorld(int frames) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
    }
  }
}
