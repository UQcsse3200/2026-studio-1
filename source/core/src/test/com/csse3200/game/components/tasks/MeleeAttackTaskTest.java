package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
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
class MeleeAttackTaskTest {

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(20f / 1000);
    ServiceLocator.registerTimeSource(gameTime);
  }

  // Constructor rejects a null target.
  @Test
  void shouldThrowWhenConstructedWithNullTarget() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackTask(null, 10, 2f),
        "Expected constructor to throw IllegalArgumentException for a null target, but it did not.");
  }

  // Constructor rejects a negative attackRange but accepts exactly zero.
  @Test
  void shouldValidateAttackRangeBoundary() {
    Entity target = createTarget();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackTask(target, 10, -1f),
        "Expected constructor to throw IllegalArgumentException for attackRange = "
            + -1
            + ", but it did not.");
    assertDoesNotThrow(
        () -> new MeleeAttackTask(target, 10, 0f),
        "Expected attackRange = 0 to be accepted as a valid boundary value.");
  }

  // While inactive, priority is the configured value within range and at the boundary, -1 outside
  // it.
  @Test
  void getPriority_inactiveAcrossWithinAtBoundaryAndOutOfRange() {
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);

    Entity targetWithin = createTarget();
    targetWithin.setPosition(1, 0);
    MeleeAttackTask taskWithin = attachTask(attacker, targetWithin);
    assertEquals(
        10,
        taskWithin.getPriority(),
        "Expected priority of 10 as target is within attackRange while task is inactive, but got "
            + taskWithin.getPriority());

    Entity targetAtBoundary = createTarget();
    targetAtBoundary.setPosition(2, 0);
    MeleeAttackTask taskAtBoundary = attachTask(attacker, targetAtBoundary);
    assertEquals(
        10,
        taskAtBoundary.getPriority(),
        "Expected priority of 10 as target sits exactly at the attackRange boundary, but got "
            + taskAtBoundary.getPriority());

    Entity targetOutOfRange = createTarget();
    targetOutOfRange.setPosition(10, 0);
    MeleeAttackTask taskOutOfRange = attachTask(attacker, targetOutOfRange);
    assertTrue(
        taskOutOfRange.getPriority() < 0,
        "Expected an inactive priority as target is outside attackRange, but got "
            + taskOutOfRange.getPriority());
  }

  // Priority is inactive once the target's health reaches zero.
  @Test
  void shouldReturnInactivePriorityWhenTargetHasNoHealthRemaining() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target);

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attackerStats = attacker.getComponent(CombatStatsComponent.class);
    while (targetStats.getHealth() > 0) {
      targetStats.hit(attackerStats);
    }

    assertTrue(
        task.getPriority() < 0,
        "Expected an inactive priority once target's health reaches 0, but got "
            + task.getPriority());
  }

  // While active, priority stays at the configured value in range, and drops once the target moves
  // out of range.
  @Test
  void getPriority_activeReflectsRangeChanges() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target);
    task.start();

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority of 10 while active and target remains in range, but got "
            + task.getPriority());

    target.setPosition(10, 0);
    assertTrue(
        task.getPriority() < 0,
        "Expected an inactive priority once target moves out of attackRange while active, but got "
            + task.getPriority());
  }

  // Priority stays at the configured value even while the attacker's MeleeAttackComponent is on
  // cooldown.
  @Test
  void shouldNotConsiderCooldownWhenCalculatingPriority() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 5, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target);
    task.start();

    // timeSinceLastAttack resets to 0 synchronously once the windup starts, so canAttack() is
    // already false here without needing to resolve the windup via update()
    attacker.getEvents().trigger("meleeAttack", target);
    assertFalse(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected the attacker's MeleeAttackComponent to now be on cooldown after the windup started.");

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority to remain 10 despite the attacker being on cooldown, but got "
            + task.getPriority());
  }

  // start(), update() before start, and stop() (with or without a prior start) never throw.
  @Test
  void lifecycleMethodsDoNotThrowAcrossStartStopStates() {
    Entity target = createTarget();
    target.setPosition(1, 0);

    Entity attackerForStart = createAttacker(2, 1, 0);
    attackerForStart.setPosition(0, 0);
    MeleeAttackTask taskForStart = attachTask(attackerForStart, target);
    assertDoesNotThrow(taskForStart::start, "Expected start() not to throw.");

    Entity attackerForUpdateBeforeStart = createAttacker(2, 1, 0);
    attackerForUpdateBeforeStart.setPosition(0, 0);
    MeleeAttackTask taskForUpdateBeforeStart = attachTask(attackerForUpdateBeforeStart, target);
    assertDoesNotThrow(
        taskForUpdateBeforeStart::update,
        "Expected update() not to throw when called before start().");

    Entity attackerForStopAfterStart = createAttacker(2, 1, 0);
    attackerForStopAfterStart.setPosition(0, 0);
    MeleeAttackTask taskForStopAfterStart = attachTask(attackerForStopAfterStart, target);
    taskForStopAfterStart.start();
    assertDoesNotThrow(taskForStopAfterStart::stop, "Expected stop() not to throw after start().");

    Entity attackerForStopWithoutStart = createAttacker(2, 1, 0);
    attackerForStopWithoutStart.setPosition(0, 0);
    MeleeAttackTask taskForStopWithoutStart = attachTask(attackerForStopWithoutStart, target);
    assertDoesNotThrow(
        taskForStopWithoutStart::stop, "Expected stop() not to throw without a prior start().");
  }

  // update() triggers meleeAttack on an eligible target, landing the hit once the windup resolves.
  @Test
  void shouldLandAttackOnUpdateWhenEligible() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target);
    task.start();

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    task.update();
    attacker.update(); // let MeleeAttackComponent resolve the zero-length windup the task started
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();

    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected update() to land an attack on an eligible target, reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  // update() does not attack when the target is out of range or has no health remaining.
  @Test
  void shouldNotAttackOnUpdateWhenIneligible() {
    // out of range
    Entity targetOutOfRange = createTarget();
    targetOutOfRange.setPosition(10, 0);
    Entity attackerForRangeCheck = createAttacker(2, 1, 0);
    attackerForRangeCheck.setPosition(0, 0);
    MeleeAttackTask taskForRangeCheck = attachTask(attackerForRangeCheck, targetOutOfRange);
    taskForRangeCheck.start();

    float healthBeforeRangeCheck =
        targetOutOfRange.getComponent(CombatStatsComponent.class).getHealth();
    taskForRangeCheck.update();
    attackerForRangeCheck.update();
    float healthAfterRangeCheck =
        targetOutOfRange.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBeforeRangeCheck,
        healthAfterRangeCheck,
        "Expected no attack to land as target is outside attackRange, but health changed from "
            + healthBeforeRangeCheck
            + " to "
            + healthAfterRangeCheck);

    // no health remaining
    Entity targetWithNoHealth = createTarget();
    targetWithNoHealth.setPosition(1, 0);
    Entity attackerForHealthCheck = createAttacker(2, 1, 0);
    attackerForHealthCheck.setPosition(0, 0);
    MeleeAttackTask taskForHealthCheck = attachTask(attackerForHealthCheck, targetWithNoHealth);
    taskForHealthCheck.start();

    CombatStatsComponent targetStats = targetWithNoHealth.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attackerStats =
        attackerForHealthCheck.getComponent(CombatStatsComponent.class);
    while (targetStats.getHealth() > 0) {
      targetStats.hit(attackerStats);
    }

    assertDoesNotThrow(
        taskForHealthCheck::update,
        "Expected update() not to throw when target has no health remaining.");
  }

  // A blocked-then-eligible attack sequence: lands, stays blocked during cooldown, lands again once
  // ready.
  @Test
  void shouldStayEligibleThroughCooldownThenAttackAgainOnceReady() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 2, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target);
    task.start();

    task.update();
    attacker.update(); // resolve first attack's windup
    float targetHealthAfterFirstAttack =
        target.getComponent(CombatStatsComponent.class).getHealth();

    task.update(); // still within cooldown - canAttack() is false, no event is triggered
    attacker.update();
    float targetHealthDuringCooldown = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        targetHealthAfterFirstAttack,
        targetHealthDuringCooldown,
        "Expected no additional damage while still on cooldown, but health changed from "
            + targetHealthAfterFirstAttack
            + " to "
            + targetHealthDuringCooldown);

    for (int i = 0; i < 101; i++) {
      attacker.update();
    }

    task.update(); // cooldown has elapsed - triggers a new windup
    attacker.update(); // resolve it
    float targetHealthAfterCooldown = target.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterCooldown < targetHealthDuringCooldown,
        "Expected a second attack to land once cooldown elapsed, reducing health from "
            + targetHealthDuringCooldown
            + " to below that value, but got "
            + targetHealthAfterCooldown);
  }

  /* ---------- Helpers ---------- */

  /**
   * Builds the default weapon used by {@link #createAttacker(float, float, float)}: a non-BOW type
   * with zero windup, so an attack resolves on the very next {@code update()} call after being
   * triggered.
   *
   * @return a fresh weapon item suitable for most tests
   */
  // The real, currently-compiling WeaponItem computes damage as baseDamage * tier, so passing
  // tier = 1 for a DAGGER (base damage 3) deals 3 damage.
  WeaponItem createInstantWeapon() {
    return new WeaponItem("Test Dagger", WeaponType.DAGGER, 1, 1, 1, 0f);
  }

  /**
   * Builds a fully created Entity representing an attacker, with a {@link MeleeAttackComponent}
   * (equipped with the default {@link #createInstantWeapon()}), an (initially empty) {@link
   * AITaskComponent}, and the components it depends on.
   *
   * @param range melee reach passed directly into {@link MeleeAttackComponent}'s constructor
   * @param cooldown minimum time, in seconds, between successive melee attacks
   * @param knockback knockback magnitude applied to the target on a successful hit
   * @return an entity carrying {@link MeleeAttackComponent}, {@link CombatStatsComponent}, {@link
   *     PhysicsComponent}, and an empty {@link AITaskComponent}
   */
  Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(
                new MeleeAttackComponent(range, cooldown, knockback, createInstantWeapon()))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent())
            .addComponent(new AITaskComponent());
    attacker.create();
    return attacker;
  }

  /**
   * Builds a fully created Entity representing a target.
   *
   * @return a target entity that has {@link CombatStatsComponent} and {@link PhysicsComponent}
   *     attached
   */
  Entity createTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(10, 0))
            .addComponent(new PhysicsComponent());
    target.create();
    return target;
  }

  /**
   * Creates a {@link MeleeAttackTask} for the given target and wires it to the attacker's existing
   * {@link AITaskComponent}.
   *
   * @param attacker an entity already created via {@link #createAttacker(float, float, float)}
   * @param target the entity the task will attempt to melee attack
   * @return the constructed and wired {@link MeleeAttackTask}, not yet started
   */
  MeleeAttackTask attachTask(Entity attacker, Entity target) {
    AITaskComponent aiTaskComponent = attacker.getComponent(AITaskComponent.class);
    MeleeAttackTask task = new MeleeAttackTask(target, 10, 2f);
    task.create(aiTaskComponent);
    return task;
  }
}
