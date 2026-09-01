package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.matchers.text.ValuePrinter.print;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MeleeAttackComponentTest {

  @BeforeEach
  void beforeEach() {
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
    Entity attacker = createAttacker(3, 2, 5);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    int targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    print(targetHealthBeforeAttack);
    print(target.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(
        target.getComponent(CombatStatsComponent.class).getHealth() < targetHealthBeforeAttack,
        "Expected Target's health to decrease from "
            + targetHealthBeforeAttack
            + " to "
            + (targetHealthBeforeAttack - knownBaseAttack)
            + " but got "
            + target.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void ShouldAttachToAttackEventOnCreate() {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(3, 2, 5))
            .addComponent(new CombatStatsComponent(20, 2));
    attacker.create();

    Entity target = new Entity().addComponent(new CombatStatsComponent(10, 0));
    attacker.getEvents().trigger("meleeAttack", target);

    assertEquals(
        8,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "expected 8 but got " + target.getComponent(CombatStatsComponent.class).getHealth());
  }

  /* This section tests the update() function logic */
  @Test
  void ShouldIncrementCooldownTimerEachUpdate() {
    // confirm update() advances the cooldown timer, verified indirectly by observing
    // whether a blocked attack becomes allowed after enough updates.
    // cooldown is represented in seconds
    float cooldownValue = 2;
    Entity attacker = createAttacker(3, cooldownValue, 1.0f);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
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
    assertEquals(8, targetCombat.getHealth());
    // second attack is blocked due to cooldown so targetHealth remains the same.
    // cycle through 2 seconds with mock game time set in beforeEach class.
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    // duration of cooldown has passed, 3rd attack
    attacker.getEvents().trigger("meleeAttack", target);
    assertTrue(targetHealth > targetCombat.getHealth());
    assertEquals(6, targetCombat.getHealth());
  }

  /* the following test the functionality, input base and edge cases of the
   * attemptAttack function
   */
  @Test
  void ShouldNotThrowWhenTargetIsNull() {
    // confirms a null target does not crash the component
    Entity attacker = createAttacker(2, 1, 0);
    attacker.create();
    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", null));
  }

  /* the following tests the cooldown element of the attemptAttack */
  @Test
  void ShouldNotAttackDuringCooldown() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack1 = targetStats.getHealth();
    // should not decrease in health due to cooldown
    attacker.getEvents().trigger("meleeAttack", target);
    assertEquals(
        targetHealthAfterAttack1,
        targetStats.getHealth(),
        "Expected second attack to not land due to cooldown in process. "
            + "Targets health expected to be "
            + targetHealthAfterAttack1
            + " but got "
            + targetStats.getHealth());
  }

  @Test
  void ShouldAttackAgainAfterCooldownElapses() {
    Entity attacker = createAttacker(3, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attackerStats = attacker.getComponent(CombatStatsComponent.class);
    // trigger first attacker
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack1 = targetStats.getHealth();

    // advance time past cooldown
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    // trigger second attacker
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack2 = targetStats.getHealth();

    assertTrue(
        targetHealthAfterAttack2 < targetHealthAfterAttack1,
        "Expected second attack after cooldown to successfully landed another hit."
            + "Expected health after second attack to be "
            + (targetHealthAfterAttack1 - attackerStats.getBaseAttack())
            + "and expected health after attack 1 is 8 but got "
            + targetHealthAfterAttack1);
  }

  @Test
  void shouldNotAttackWhenCooldownPartiallyElapsed() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack1 = targetStats.getHealth();
    // advance time to just before cooldown is complete
    for (int i = 0; i < 99; i++) {
      attacker.update();
    }
    // should not decrease in health due to cooldown not being completed
    attacker.getEvents().trigger("meleeAttack", target);
    assertEquals(
        targetHealthAfterAttack1,
        targetStats.getHealth(),
        "Expected second attack to not land due to cooldown in process. "
            + "Targets health expected to be "
            + targetHealthAfterAttack1
            + " but got "
            + targetStats.getHealth());
  }

  @Test
  void ShouldResetCooldownOnlyAfterSuccessfulHit() {
    Entity attacker = createAttacker(2, 5, 0);
    Entity targetWithoutCombatStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutCombatStats.create();
    attacker.setPosition(0, 0);
    targetWithoutCombatStats.setPosition(1, 0);

    // trigger attack on target
    attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats);
    // cooldown should not start - target has no stats component
    // target with stats = health should decrease
    Entity targetWithCombatStats = createTarget();
    attacker.setPosition(0, 0);
    targetWithCombatStats.setPosition(1, 0);
    float targetHealthBeforeAttack =
        targetWithCombatStats.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", targetWithCombatStats);
    float targetHealthAfterAttack =
        targetWithCombatStats.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected the attack on targetWithCombatStats to land (cooldown was not "
            + "consumed by the earlier failed attempt), reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  /* the following tests will test the range element of attemptAttack */

  @Test
  void ShouldAttackWhenTargetWithinRange() {

    Entity attacker = createAttacker(2, 1, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected the attack on target to land (cooldown was not "
            + "consumed by the earlier failed attempt), reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  @Test
  void ShouldNotAttackWhenTargetOutsideRange() {
    Entity attacker = createAttacker(2, 1, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(5, 0);

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        targetHealthBeforeAttack,
        targetHealthAfterAttack,
        "Expected the before attack health of the target: "
            + targetHealthBeforeAttack
            + " to match the after attack health of the target: "
            + targetHealthAfterAttack
            + "as the target is not within the required range of 2");
  }

  @Test
  void ShouldHandleTargetExactlyAtRangeBoundary() {
    Entity attacker = createAttacker(2, 1, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected the attack on target despite the target being at boundary of "
            + "the range of 2, reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  /* the following tests when the target is missing the CombatStatsComponent */

  @Test
  void ShouldNotAttackWhenTargetHasNoCombatStatsComponent() {
    Entity attacker = createAttacker(2, 5, 0);
    Entity targetWithoutCombatStats = new Entity().addComponent(new PhysicsComponent());
    targetWithoutCombatStats.create();
    attacker.setPosition(0, 0);
    targetWithoutCombatStats.setPosition(1, 0);

    // trigger attack on target
    attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats);
    // cooldown should not start - target has no stats component
    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", targetWithoutCombatStats));
  }

  /* The following tests the knockback element of the attemptAttack function */
  @Test
  void ShouldApplyKnockbackWhenPositiveAndTargetHasPhysicsComponent() {
    Entity attacker = createAttacker(3, 1, 3);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    PhysicsComponent targetPhysics = target.getComponent(PhysicsComponent.class);
    float targetLinearVelocityBeforeAttack = targetPhysics.getBody().getLinearVelocity().len();
    // checking velocity of target is at rest initially
    assertEquals(
        0,
        targetLinearVelocityBeforeAttack,
        "Velocity of target is expected to be at rest - 0 initially, "
            + "but was actually "
            + targetLinearVelocityBeforeAttack);
    // trigger attack on target
    attacker.getEvents().trigger("meleeAttack", target);
    float targetLinearVelocityAfterAttack = targetPhysics.getBody().getLinearVelocity().len();
    assertNotSame(
        targetLinearVelocityBeforeAttack,
        targetLinearVelocityAfterAttack,
        "Expected a change in velocity by a factor of 3 from knockback of attack, "
            + "thus expecting velocity before attack: "
            + targetLinearVelocityBeforeAttack
            + "doesn't equal velocity after attack: "
            + targetLinearVelocityAfterAttack
            + "but got "
            + (targetLinearVelocityBeforeAttack != targetLinearVelocityAfterAttack));
  }

  @Test
  void ShouldNotApplyKnockbackWhenValueIsZero() {
    Entity attacker = createAttacker(3, 1, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);
    PhysicsComponent targetPhysics = target.getComponent(PhysicsComponent.class);
    float targetLinearVelocityBeforeAttack = targetPhysics.getBody().getLinearVelocity().len();
    // checking velocity of target is at rest initially
    assertEquals(
        0,
        targetLinearVelocityBeforeAttack,
        "Velocity of target is expected to be at rest - 0 initially, "
            + "but was actually "
            + targetLinearVelocityBeforeAttack);
    // trigger attack on target
    attacker.getEvents().trigger("meleeAttack", target);
    float targetLinearVelocityAfterAttack = targetPhysics.getBody().getLinearVelocity().len();
    assertEquals(
        targetLinearVelocityBeforeAttack,
        targetLinearVelocityAfterAttack,
        "Expected no change in velocity from knockback as it is disabled"
            + "thus expecting velocity before attack: "
            + targetLinearVelocityBeforeAttack
            + "to equal velocity after attack: "
            + targetLinearVelocityAfterAttack
            + "but got "
            + (targetLinearVelocityBeforeAttack == targetLinearVelocityAfterAttack));
  }

  @Test
  void ShouldNotApplyKnockbackWhenTargetHasNoPhysicsComponent() {
    Entity attacker = createAttacker(2, 1, 3);
    Entity target = new Entity().addComponent(new CombatStatsComponent(10, 0));
    target.create();
    attacker.setPosition(0, 0);
    target.setPosition(2, 0);

    float healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    assertDoesNotThrow(() -> attacker.getEvents().trigger("meleeAttack", target));

    float healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();

    assertTrue(
        healthAfter < healthBefore,
        "Expected damage to still apply even though target has no PhysicsComponent"
            + "(knockback should be skipped but not damage), but health went from "
            + healthBefore
            + " to "
            + healthAfter);
  }

  /* the following test the canAttack() function logic */
  @Test
  void canAttack_immediatelyAfterAttack_returnsFalse() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", target);

    assertFalse(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to be false immediately after a successful attack.");
  }

  @Test
  void canAttack_beforeCooldownElapsed_returnsFalse() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", target);
    // advance time to just before the 2-second cooldown completes (99 * 20ms = 1.98s)
    for (int i = 0; i < 99; i++) {
      attacker.update();
    }

    assertFalse(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to still be false with cooldown partially elapsed.");
  }

  @Test
  void canAttack_atOrJustPastCooldownBoundary_returnsTrue() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", target);
    // advance time to just past the 2-second cooldown (101 * 20ms = 2.02s) — avoids
    // asserting exact float-accumulation equality at the boundary itself, which is
    // unreliable since deltaTime (0.02f) doesn't sum to a bit-exact 2.0f over 100 additions
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }

    assertTrue(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to be true once the cooldown timer has reached or passed the configured duration.");
  }

  @Test
  void canAttack_consistentWithAttemptAttackBehaviour() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    attacker.getEvents().trigger("meleeAttack", target);
    for (int i = 0; i < 101; i++) {
      attacker.update();
    }
    assertTrue(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to report true before the second attack is attempted.");

    float healthBeforeSecondAttack = targetStats.getHealth();
    attacker.getEvents().trigger("meleeAttack", target);
    float healthAfterSecondAttack = targetStats.getHealth();

    assertTrue(
        healthAfterSecondAttack < healthBeforeSecondAttack,
        "Expected the second attack to actually land (consistent with canAttack() reporting true beforehand), "
            + "reducing health from "
            + healthBeforeSecondAttack
            + " to below that value, but got "
            + healthAfterSecondAttack);
  }

  @Test
  void canAttack_afterCooldownElapsed_returnsTrue() {
    Entity attacker = createAttacker(2, 2, 0);
    Entity target = createTarget();
    attacker.setPosition(0, 0);
    target.setPosition(1, 0);

    attacker.getEvents().trigger("meleeAttack", target);
    // advance time well past the 2-second cooldown
    for (int i = 0; i < 150; i++) {
      attacker.update();
    }

    assertTrue(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected canAttack() to be true once the cooldown duration has been exceeded.");
  }

  /* ---------- Helpers ---------- */

  /**
   * Builds a fully created Entity representing an attack, with a {@link MeleeAttackComponent} and
   * the components it depends on, ready for use in a test.
   *
   * @param knockback - passed directly into {@link MeleeAttackComponent}'s constructor - the amount
   *     of force the target will experience from the attack
   * @param range - the distance between the target and the attacker that allows the attacker to use
   *     the component to attack the target i.e. within 1.5 m of the target, the attacker can use
   *     the melee component
   * @param cooldown - the duration of time the entity must wait before attacking the target again
   * @return - an entity that uses the {@link MeleeAttackComponent} to attack another entity with
   *     the {@link CombatStatsComponent} and {@link PhysicsComponent} added as well.
   *     <p><bold>Design Decision:</bold> given that for this test, it is only testing the melee
   *     component which doesn't use the physics layer checks, the target layer is not included as
   *     an input.
   */
  Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(range, cooldown, knockback))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent());
    attacker.create();
    return attacker;
  }

  /**
   * Builds a fully created Entity representing an attack, with a {@link MeleeAttackComponent} and
   * the components it depends on, ready for use in a test.
   *
   * @return a target entity that has the {@link CombatStatsComponent} and {@link PhysicsComponent}
   *     attached.
   */
  Entity createTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent());

    target.create();

    return target;
  }
}
