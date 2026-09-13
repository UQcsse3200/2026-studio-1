package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.MeleeAttackComponent;
import com.csse3200.game.entities.Entity;

/**
 * An AI task that decides when an entity with a MeleeAttackComponent should attempt a melee attack
 * against a target entity.
 *
 * <p>MeleeAttackTask does not perform the attack itself — it triggers the {@code "meleeAttack"}
 * event on the owning entity, which {@link MeleeAttackComponent} subscribes to and resolves. This
 * keeps attack decision-making (this task) and attack execution ({@code MeleeAttackComponent})
 * decoupled.
 *
 * <p>Mirrors the structure of ChaseTask: getPriority() only evaluates whether the situation is
 * melee-relevant (target alive, in range), split into getActivePriority()/getInactivePriority()
 * depending on current task status. Cooldown readiness (canAttack()) is deliberately NOT checked in
 * getPriority() — only in update() — so a momentary cooldown does not cause this task to lose
 * priority and get swapped out for another task every tick the attacker is on cooldown.
 *
 * @see MeleeAttackComponent
 * @see PriorityTask
 * @see DefaultTask
 */
public class MeleeAttackTask extends DefaultTask implements PriorityTask {
  private final Entity target;
  private final int priority;
  private final float attackRange;
  private MeleeAttackComponent meleeAttackComponent;

  /**
   * Creates a melee attack task for the given target.
   *
   * @param target the entity to attempt to melee attack. Must not be null.
   * @param priority the priority value to return from getPriority() when this task is eligible to
   *     run.
   * @param attackRange the distance, in world units, at or under which this task becomes eligible.
   *     Must be &gt;= 0.
   * @throws IllegalArgumentException if target is null or attackRange is negative.
   */
  public MeleeAttackTask(Entity target, int priority, float attackRange) {
    if (target == null) {
      throw new IllegalArgumentException("Target must not be null");
    }
    if (attackRange < 0) {
      throw new IllegalArgumentException("attackRange must be >= 0");
    }
    this.target = target;
    this.priority = priority;
    this.attackRange = attackRange;
  }

  /**
   * Called once when this task becomes the active task on the owning entity's AITaskComponent.
   * Resolves this entity's MeleeAttackComponent for use in update(). Does not itself attempt an
   * attack — that only happens in update().
   */
  @Override
  public void start() {
    super.start();
    meleeAttackComponent = owner.getEntity().getComponent(MeleeAttackComponent.class);
  }

  /**
   * Called every tick while this task is active. Re-checks target liveness, cooldown, and range; if
   * all are satisfied, triggers the {@code "meleeAttack"} event on the owning entity with {@code
   * target} as the event payload.
   *
   * <p>Does not call MeleeAttackComponent.attemptAttack directly — this preserves the decoupling
   * between task (decision) and component (execution).
   */
  @Override
  public void update() {
    if (meleeAttackComponent == null || !isTargetAlive()) {
      return;
    }

    if (!meleeAttackComponent.canAttack()) {
      return;
    }

    if (getDistanceToTarget() > attackRange) {
      return;
    }

    owner.getEntity().getEvents().trigger("meleeAttack", target);
  }

  /**
   * Determines this task's current priority for AITaskComponent's task-selection logic.
   *
   * @return an active or inactive priority value depending on current task status.
   */
  @Override
  public int getPriority() {
    if (status == Status.ACTIVE) {
      return getActivePriority();
    }
    return getInactivePriority();
  }

  /**
   * Computes priority while this task is the currently active task. Active evaluation still
   * requires the target to be alive and within range — if either condition fails, this task yields
   * priority so another task (e.g. a chase task) can take over.
   *
   * @return {@code priority} if the target is alive and within {@code attackRange}, {@code -1}
   *     otherwise.
   */
  private int getActivePriority() {
    if (!isTargetAlive()) {
      return -1;
    }
    if (getDistanceToTarget() > attackRange) {
      return -1;
    }
    return priority;
  }

  /**
   * Computes priority while this task is not currently active, i.e. whether it should be selected
   * to start running. Uses the same alive/in-range conditions as {@link #getActivePriority()}; the
   * two are currently identical since melee range has no hysteresis (unlike, for example,
   * ChaseTask's separate viewDistance/maxChaseDistance thresholds).
   *
   * @return {@code priority} if the target is alive and within {@code attackRange}, {@code -1}
   *     otherwise.
   */
  private int getInactivePriority() {
    if (!isTargetAlive()) {
      return -1;
    }
    if (getDistanceToTarget() <= attackRange) {
      return priority;
    }
    return -1;
  }

  /**
   * Checks whether {@code target} is still a meaningful entity to attack. There is no queryable
   * "disposed" flag on {@code Entity} in this engine, so liveness is inferred from health instead —
   * a target with no {@link CombatStatsComponent}, or with zero or negative health, is treated as
   * no longer alive.
   *
   * @return true if target has a {@link CombatStatsComponent} reporting health &gt; 0, false
   *     otherwise.
   */
  private boolean isTargetAlive() {
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    return targetStats != null && targetStats.getHealth() > 0;
  }

  /**
   * Computes the straight-line distance between the owning entity and {@code target}, based on
   * their current positions.
   *
   * @return the distance, in world units, between the owning entity and target.
   */
  private float getDistanceToTarget() {
    return owner.getEntity().getPosition().dst(target.getPosition());
  }
}
