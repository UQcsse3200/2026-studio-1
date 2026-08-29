package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.matchers.text.ValuePrinter.print;

import com.badlogic.gdx.Game;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.internal.creation.settings.CreationSettings;

import javax.security.auth.login.CredentialException;

@ExtendWith(GameExtension.class)
public class MeleeAttackComponentTest {

  @BeforeEach
  public void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);
    // should reject negative range

  }

  /* testing the constructor class */
  @Test
  void shouldStoreConstructorValuesCorrectly() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertEquals(
        rangeValue,
        meleeAttack.getRange(),
        "Range stats from meleeAttack expected: "
            + rangeValue
            + " but got: "
            + meleeAttack.getRange());
    assertEquals(
        cooldownValue,
        meleeAttack.getCooldown(),
        "Cooldown stats from meleeAttack expected: "
            + cooldownValue
            + " but got"
            + meleeAttack.getCooldown());
    assertEquals(
        knockbackValue,
        meleeAttack.getKnockback(),
        "Knockback stats from meleeAttack expected: "
            + knockbackValue
            + " but got"
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidRange() {
    /* Confirm an invalid range passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(-1, 10, 5f),
        "Expected constructor to throw IllegalArgumentException for range = "
            + -1
            + ", but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidCooldown() {
    /* Confirm an invalid cooldown passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, -10, 5f),
        "Expected constructor to throw IllegalArgumentException for cooldown = "
            + -10
            + ", but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithInvalidKnockback() {
    /* Confirm an invalid knockback passed directly to the constructor is rejected. */
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackComponent(1, 10, -5f),
        "Expected constructor to throw IllegalArgumentException for knockback = "
            + -5f
            + ", but it did not.");
  }

  /* testing the setters and base and edge cases of inputs
  * i.e. cooldown cannot be zero or negative, knockback can be 0 but not negative.
  * range cannot be 0 or negative
  */
  @Test
  void ShouldUpdateStatsViaSetter() {
    /*
     */
    float oldRange = 0.5f;
    float oldCooldown = 10;
    float oldKnockback = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(oldRange, oldCooldown, oldKnockback);
    float newRange = 0.1f;
    float newCooldown = 50;
    float newKnockback = 5.5f;
    meleeAttack.setRange(newRange);
    meleeAttack.setCooldown(newCooldown);
    meleeAttack.setKnockback(newKnockback);
    assertEquals(
        newRange,
        meleeAttack.getRange(),
        "meleeAttack Range stats from changed from "
            + oldRange
            + " to expected: "
            + newRange
            + " but got: "
            + meleeAttack.getRange());
    assertEquals(
        newCooldown,
        meleeAttack.getCooldown(),
        "meleeAttack Cooldown stats from changed from "
            + oldCooldown
            + " to expected: "
            + newCooldown
            + " but got: "
            + meleeAttack.getCooldown());
    assertEquals(
        newKnockback,
        meleeAttack.getKnockback(),
        "meleeAttack Range stats from changed from "
            + oldKnockback
            + " to expected: "
            + newKnockback
            + " but got: "
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldRejectZeroOrNegativeCooldown() {
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    // negative value
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(-30));

    // base case also throws exception
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setCooldown(0));

    // original value has not been modified
    assertEquals(
        cooldownValue,
        meleeAttack.getCooldown(),
        "Cooldown stats from meleeAttack expected: "
            + cooldownValue
            + " but got"
            + meleeAttack.getCooldown());
  }

  @Test
  void shouldRejectNegativeRange() {
    /* Confirm negative */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setRange(-1));
    assertEquals(
        rangeValue,
        meleeAttack.getRange(),
        "Range stats from meleeAttack expected: "
            + rangeValue
            + " but got: "
            + meleeAttack.getRange());
  }

  @Test
  void shouldRejectNegativeKnockback() {
    /* Confirm negative */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    assertThrows(IllegalArgumentException.class, () -> meleeAttack.setKnockback(-5f));
    assertEquals(
        knockbackValue,
        meleeAttack.getKnockback(),
        "Knockback stats from meleeAttack expected: "
            + knockbackValue
            + " but got"
            + meleeAttack.getKnockback());
  }

  @Test
  void shouldAcceptZeroKnockback() {
    /* Confirm 0 is treated as valid since it disables knockback rather than being invalid. */
    float rangeValue = 0.5f;
    float cooldownValue = 10;
    float knockbackValue = 2.0f;
    MeleeAttackComponent meleeAttack =
        new MeleeAttackComponent(rangeValue, cooldownValue, knockbackValue);
    meleeAttack.setKnockback(0f);
    assertEquals(
        0f,
        meleeAttack.getKnockback(),
        "meleeAttack Knockback value should accept "
            + 0f
            + " but got a value of "
            + meleeAttack.getKnockback());
  }

  /* the following test the create() function logic */
  @Test
  void shouldResolveCombatStatsComponentOnCreate() {
    int knownBaseAttack = 2;
    Entity attacker = createAttacker(3,2,5);
    Entity target = createTarget();
    attacker.setPosition(0,0);
    target.setPosition(2,0);
    int targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    print(targetHealthBeforeAttack);
    print(target.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(target.getComponent(CombatStatsComponent.class).getHealth()
            < targetHealthBeforeAttack, "Expected Target's health to decrease from "
            + targetHealthBeforeAttack + " to "
            + (targetHealthBeforeAttack - knownBaseAttack) + " but got "
            + target.getComponent(CombatStatsComponent.class).getHealth()
    );
  }

  @Test
  void ShouldAttachToAttackEventOnCreate() {
    Entity attacker = new Entity()
            .addComponent(new MeleeAttackComponent(3,2,5))
            .addComponent(new CombatStatsComponent(20, 2));
    attacker.create();

    Entity target = new Entity()
            .addComponent(new CombatStatsComponent(10, 0));
    attacker.getEvents().trigger("meleeAttack", target);

    assertEquals(8, target.getComponent(CombatStatsComponent.class).getHealth(),
            "expected 8 but got "
                    + target.getComponent(CombatStatsComponent.class).getHealth()
    );
  }

//  SECTION: update()
//===========================================================
//
//  TEST: ShouldIncrementCooldownTimerEachUpdate
//  PURPOSE:
//  Confirm update() advances the cooldown timer, verified indirectly by
//  observing whether a blocked attack becomes allowed after enough updates.
//
//  BEGIN
//  SET cooldownValue = 2.0   // seconds
//  SET attacker = CreateAttacker(range=1, cooldown=cooldownValue, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target within range
//
//  TRIGGER attack -- SHOULD succeed (first attack, no cooldown yet)
//  ASSERT target's health decreased by one hit's worth of damage
//
//  TRIGGER attack AGAIN immediately -- SHOULD be blocked (still on cooldown)
//  ASSERT target's health UNCHANGED since the previous check
//
//  CALL attacker's update() repeatedly / with enough elapsed time to
//  exceed cooldownValue
//
//  TRIGGER attack a third time -- SHOULD succeed now
//  ASSERT target's health decreased again
//  END
  /* This section tests the update() function logic */
  @Test
  void ShouldIncrementCooldownTimerEachUpdate() {
    // confirm update() advances trhe cooldown timer, verified indirectly by observing
    // whether a blocked attack becomes allowed after enough updates.
    // cooldown is represented in seconds
    float cooldownValue = 2;
    Entity attacker = createAttacker(3, cooldownValue, 1.0f);
    Entity target = createTarget();
    attacker.setPosition(0,0);
    target.setPosition(2,0);
    CombatStatsComponent targetCombat = target.getComponent(CombatStatsComponent.class);
    // health prior to 1st attack
    int targetHealth = targetCombat.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    assertTrue(targetHealth > targetCombat.getHealth());
    assertEquals(8, targetCombat.getHealth());
    // health after 1st attack before second attack
    targetHealth = targetCombat.getHealth();
    // the values should be the same despite being called as cooldown should still be running
    attacker.getEvents().trigger("meleeAttack", target);
    //assertTrue(targetHealth > targetCombat.getHealth());
    assertEquals(8, targetCombat.getHealth());
    // second attack is blocked due to cooldown so targetHealth remains the same.
    // cycle through 2 seconds with mock gametime set in beforeEach class.
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    // duration of cooldown has passed, 3rd attack
    attacker.getEvents().trigger("meleeAttack", target);
    assertTrue(targetHealth > targetCombat.getHealth());
    assertEquals(6, targetCombat.getHealth());

  }
//
//===========================================================
//  SECTION: attemptAttack -- null target
//===========================================================
//
//  TEST: ShouldNotThrowWhenTargetIsNull
//  PURPOSE:
//  Confirm a null target does not crash the component.
//
//  BEGIN
//  SET attacker = CreateAttacker(range=1, cooldown=1, knockback=0)
//  CALL attacker.create()
//
//  ASSERT that TRIGGERING "meleeAttack" event on attacker, WITH target = NULL,
//  DOES NOT THROW any exception
//          END
//
//
//===========================================================
//  SECTION: attemptAttack -- cooldown
//===========================================================
//
//  TEST: ShouldNotAttackDuringCooldown
//          BEGIN
//  SET attacker = CreateAttacker(range=1, cooldown=5, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target within range
//
//  TRIGGER attack -- succeeds
//  RECORD healthAfterFirstAttack = target's current health
//
//  TRIGGER attack again immediately (no time elapsed)
//
//  ASSERT target's health STILL EQUALS healthAfterFirstAttack
//  // second attack did not land
//  END
//
//
//  TEST: ShouldAttackAgainAfterCooldownElapses
//          BEGIN
//  SET attacker = CreateAttacker(range=1, cooldown=2, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target within range
//
//  TRIGGER attack -- succeeds
//  RECORD healthAfterFirstAttack
//
//  ADVANCE time past cooldown (via repeated update() calls)
//
//  TRIGGER attack again
//
//  ASSERT target's health is LESS THAN healthAfterFirstAttack
//  // second attack landed
//  END
//
//
//  TEST: ShouldNotAttackWhenCooldownPartiallyElapsed
//          BEGIN
//  SET attacker = CreateAttacker(range=1, cooldown=5, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target within range
//
//  TRIGGER attack -- succeeds
//  RECORD healthAfterFirstAttack
//
//  ADVANCE time to JUST UNDER cooldown (e.g. cooldown - 0.1)
//
//  TRIGGER attack again
//
//  ASSERT target's health STILL EQUALS healthAfterFirstAttack
//  // still blocked
//  END
//
//
//  TEST: ShouldResetCooldownOnlyAfterASuccessfulHit
//          BEGIN
//  SET attacker = CreateAttacker(range=1, cooldown=5, knockback=0)
//
//  SET targetWithoutStats = CreateTarget(matching layer)
//  // built WITHOUT a CombatStatsComponent
//  POSITION targetWithoutStats within range
//
//  TRIGGER attack on targetWithoutStats
//  // fails at the "missing CombatStatsComponent" check, cooldown
//  // should NOT have been reset
//
//  SET targetWithStats = CreateTarget(matching layer)
//  // built WITH a CombatStatsComponent
//  POSITION targetWithStats within range
//
//  TRIGGER attack on targetWithStats, IMMEDIATELY (no time elapsed)
//
//  ASSERT targetWithStats' health DECREASED
//  // proves cooldown was NOT consumed by the earlier failed attempt --
//  // if it had been, this attack would be blocked too
//  END
//
//
//===========================================================
//  SECTION: attemptAttack -- range
//===========================================================
//
//  TEST: ShouldAttackWhenTargetWithinRange
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target at distance = 1 (within the range of 2)
//
//  TRIGGER attack
//
//  ASSERT target's health DECREASED by exactly attacker's baseAttack value
//          END
//
//
//  TEST: ShouldNotAttackWhenTargetOutsideRange
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target at distance = 5 (outside the range of 2)
//
//  TRIGGER attack
//
//  ASSERT target's health UNCHANGED
//  END
//
//
//  TEST: ShouldHandleTargetExactlyAtRangeBoundary
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target at distance = EXACTLY 2 (equal to range)
//
//  TRIGGER attack
//
//  ASSERT target's health DECREASED
//  // per the pseudocode's "distance > range" rejection, distance == range
//  // is NOT rejected, so boundary is inclusive -- attack should land
//  END
//
//
//===========================================================
//  SECTION: attemptAttack -- missing CombatStatsComponent on target
//===========================================================
//
//  TEST: ShouldNotAttackWhenTargetHasNoCombatStatsComponent
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=0)
//  SET target = new Entity WITH PhysicsComponent only
//  // no CombatStatsComponent attached
//  POSITION target within range
//  CALL target.create()
//
//  ASSERT that TRIGGERING attack DOES NOT THROW any exception
//          // no health to check -- target has no CombatStatsComponent at all
//          END
//
//
//===========================================================
//  SECTION: attemptAttack -- knockback
//===========================================================
//
//  TEST: ShouldApplyKnockbackWhenPositiveAndTargetHasPhysicsComponent
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=3)
//  SET target = CreateTarget(matching layer)
//  // includes both CombatStatsComponent AND PhysicsComponent
//  POSITION target within range
//
//  RECORD velocityBeforeAttack = target's PhysicsComponent's body velocity
//  // expected to be zero/at rest initially
//
//  TRIGGER attack
//
//  RECORD velocityAfterAttack = target's PhysicsComponent's body velocity
//
//  ASSERT velocityAfterAttack IS NOT EQUAL TO velocityBeforeAttack
//          // behavioural check only -- do NOT assert an exact velocity value
//          END
//
//
//  TEST: ShouldNotApplyKnockbackWhenValueIsZero
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=0)
//  SET target = CreateTarget(matching layer)
//  POSITION target within range
//
//  RECORD velocityBeforeAttack
//
//  TRIGGER attack
//
//  ASSERT target's health DECREASED   // the attack itself still happened
//  RECORD velocityAfterAttack
//  ASSERT velocityAfterAttack EQUALS velocityBeforeAttack
//  // no knockback applied, despite a successful hit
//  END
//
//
//  TEST: ShouldNotApplyKnockbackWhenTargetHasNoPhysicsComponent
//          BEGIN
//  SET attacker = CreateAttacker(range=2, cooldown=1, knockback=3)
//  SET target = new Entity WITH CombatStatsComponent only
//  // no PhysicsComponent attached
//  POSITION target within range
//  CALL target.create()
//
//  ASSERT target's health DECREASED
//  ASSERT that TRIGGERING attack DOES NOT THROW any exception
//  END
  // damage still applies even though knockback cannot
  /* ---------- Helpers ---------- */

  /**
   * Builds a fully created Entity representing an attack, with a {@link MeleeAttackComponent} and
   * the components it depends on, reafy for use in a test.
   *
   * @param knockback - passed directly into melee's component constyrcutor - the amount of force the target will experience from the attack
   * @param range - the distance between the target and the attacker that allows the attacker to use the compoennt to attack the target
   *              i.e within 1.5 m of the target, the attacker can use the melee component
   * @param cooldown - the duration of time the entity must wait before attacking the target again
   * @return
   *
   * <p><bold>Design Decision:</bold> given that for this test, it is only testing the melee
   * component which doesn't use the physics layer checks, the target layer is not included
   * as an input.</p>
   */
  Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker = new Entity()
            .addComponent(new MeleeAttackComponent(range, cooldown, knockback))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent());
    attacker.create();
    return attacker;
  }

  Entity createTarget() {
    Entity target = new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent());

    target.create();

    return target;
  }

}
