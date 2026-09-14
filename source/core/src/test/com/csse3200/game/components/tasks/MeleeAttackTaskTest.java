package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.MeleeAttackComponent;
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

  /* testing the constructor class */
  @Test
  void shouldThrowWhenConstructedWithNullTarget() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackTask(null, 10, 2f),
        "Expected constructor to throw IllegalArgumentException for a null target, but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithNegativeAttackRange() {
    Entity target = createTarget();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MeleeAttackTask(target, 10, -1f),
        "Expected constructor to throw IllegalArgumentException for attackRange = "
            + -1
            + ", but it did not.");
  }

  @Test
  void shouldAcceptZeroAttackRange() {
    Entity target = createTarget();
    assertDoesNotThrow(
        () -> new MeleeAttackTask(target, 10, 0f),
        "Expected attackRange = 0 to be accepted as a valid boundary value.");
  }

  /* the following tests the getPriority() function's inactive-status branch, i.e. before the
   * task has been started by an AITaskComponent
   */
  @Test
  void shouldReturnPriorityWhenInactiveAndTargetWithinRange() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority of 10 as target is within attackRange while task is inactive, but got "
            + task.getPriority());
  }

  @Test
  void shouldReturnInactivePriorityWhenInactiveAndTargetOutOfRange() {
    Entity target = createTarget();
    target.setPosition(10, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertTrue(
        task.getPriority() < 0,
        "Expected an inactive priority as target is outside attackRange, but got "
            + task.getPriority());
  }

  @Test
  void shouldReturnPriorityWhenInactiveAndTargetExactlyAtRangeBoundary() {
    Entity target = createTarget();
    target.setPosition(2, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority of 10 as target sits exactly at the attackRange boundary, but got "
            + task.getPriority());
  }

  @Test
  void shouldReturnInactivePriorityWhenTargetHasNoHealthRemaining() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

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

  /* the following tests the getPriority() function's active-status branch, i.e. after the
   * task has been started (as would happen once AITaskComponent selects it as highest priority)
   */
  @Test
  void shouldReturnPriorityWhenActiveAndTargetWithinRange() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority of 10 while active and target remains in range, but got "
            + task.getPriority());
  }

  @Test
  void shouldReturnInactivePriorityWhenActiveAndTargetMovesOutOfRange() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    target.setPosition(10, 0);

    assertTrue(
        task.getPriority() < 0,
        "Expected an inactive priority once target moves out of attackRange while active, but got "
            + task.getPriority());
  }

  @Test
  void shouldNotConsiderCooldownWhenCalculatingPriority() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 5, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    attacker.getEvents().trigger("meleeAttack", target);
    assertFalse(
        attacker.getComponent(MeleeAttackComponent.class).canAttack(),
        "Expected the attacker's MeleeAttackComponent to now be on cooldown after a successful hit.");

    assertEquals(
        10,
        task.getPriority(),
        "Expected priority to remain 10 despite the attacker being on cooldown, but got "
            + task.getPriority());
  }

  /* the following tests the start() function */
  @Test
  void shouldNotThrowOnStart() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertDoesNotThrow(task::start);
  }

  /* the following tests the update() function, including its interaction with
   * MeleeAttackComponent's cooldown and the target's range/health
   */
  @Test
  void shouldNotThrowWhenUpdateCalledBeforeStart() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertDoesNotThrow(task::update);
  }

  @Test
  void shouldLandAttackOnUpdateWhenEligible() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    task.update();
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();

    assertTrue(
        targetHealthAfterAttack < targetHealthBeforeAttack,
        "Expected update() to land an attack on an eligible target, reducing health from "
            + targetHealthBeforeAttack
            + " to below that value, but got "
            + targetHealthAfterAttack);
  }

  @Test
  void shouldNotAttackOnUpdateWhenOutOfRange() {
    Entity target = createTarget();
    target.setPosition(10, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    float targetHealthBeforeAttack = target.getComponent(CombatStatsComponent.class).getHealth();
    task.update();
    float targetHealthAfterAttack = target.getComponent(CombatStatsComponent.class).getHealth();

    assertEquals(
        targetHealthBeforeAttack,
        targetHealthAfterAttack,
        "Expected no attack to land as target is outside attackRange, but health changed from "
            + targetHealthBeforeAttack
            + " to "
            + targetHealthAfterAttack);
  }

  @Test
  void shouldNotAttackOnUpdateWhenTargetHasNoHealthRemaining() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attackerStats = attacker.getComponent(CombatStatsComponent.class);
    while (targetStats.getHealth() > 0) {
      targetStats.hit(attackerStats);
    }

    assertDoesNotThrow(
        task::update, "Expected update() not to throw when target has no health remaining.");
  }

  @Test
  void shouldStayEligibleThroughCooldownThenAttackAgainOnceReady() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 2, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    task.update();
    float targetHealthAfterFirstAttack =
        target.getComponent(CombatStatsComponent.class).getHealth();

    task.update();
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

    task.update();
    float targetHealthAfterCooldown = target.getComponent(CombatStatsComponent.class).getHealth();
    assertTrue(
        targetHealthAfterCooldown < targetHealthDuringCooldown,
        "Expected a second attack to land once cooldown elapsed, reducing health from "
            + targetHealthDuringCooldown
            + " to below that value, but got "
            + targetHealthAfterCooldown);
  }

  /* the following tests the stop() function */
  @Test
  void shouldNotThrowWhenStoppedAfterStart() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);
    task.start();

    assertDoesNotThrow(task::stop);
  }

  @Test
  void shouldNotThrowWhenStoppedWithoutPriorStart() {
    Entity target = createTarget();
    target.setPosition(1, 0);
    Entity attacker = createAttacker(2, 1, 0);
    attacker.setPosition(0, 0);
    MeleeAttackTask task = attachTask(attacker, target, 10, 2f);

    assertDoesNotThrow(task::stop);
  }

  /* ---------- Helpers ---------- */

  /**
   * Builds a fully created Entity representing an attacker, with a {@link MeleeAttackComponent}, an
   * (initially empty) {@link AITaskComponent}, and the components it depends on, ready for use in a
   * test.
   *
   * @param range melee reach passed directly into {@link MeleeAttackComponent}'s constructor
   * @param cooldown minimum time, in seconds, between successive melee attacks
   * @param knockback knockback magnitude applied to the target on a successful hit
   * @return an entity carrying {@link MeleeAttackComponent}, {@link CombatStatsComponent}, {@link
   *     PhysicsComponent}, and an empty {@link AITaskComponent}.
   */
  Entity createAttacker(float range, float cooldown, float knockback) {
    Entity attacker =
        new Entity()
            .addComponent(new MeleeAttackComponent(range, cooldown, knockback))
            .addComponent(new CombatStatsComponent(20, 2))
            .addComponent(new PhysicsComponent())
            .addComponent(new AITaskComponent());
    attacker.create();
    return attacker;
  }

  /**
   * Builds a fully created Entity representing a target, with the components {@link
   * MeleeAttackTask} tests depend on.
   *
   * @return a target entity that has {@link CombatStatsComponent} and {@link PhysicsComponent}
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

  /**
   * Creates a {@link MeleeAttackTask} for the given target and wires it to the attacker's existing
   * {@link AITaskComponent} (which implements {@code TaskRunner}), matching how production code
   * would attach it via {@code AITaskComponent.addTask(...)}, but keeping a direct reference to the
   * task itself so its lifecycle methods can be called and asserted on directly in tests.
   *
   * @param attacker an entity already created via {@link #createAttacker(float, float, float)}
   * @param target the entity the task will attempt to melee attack
   * @param priority the priority value to return from getPriority() when eligible
   * @param attackRange the distance, in world units, at or under which the task is eligible
   * @return the constructed and wired {@link MeleeAttackTask}, not yet started
   */
  MeleeAttackTask attachTask(Entity attacker, Entity target, int priority, float attackRange) {
    AITaskComponent aiTaskComponent = attacker.getComponent(AITaskComponent.class);
    MeleeAttackTask task = new MeleeAttackTask(target, priority, attackRange);
    task.create(aiTaskComponent);
    return task;
  }
}
