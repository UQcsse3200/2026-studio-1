package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.RangedAttackComponent;
import com.csse3200.game.entities.Entity;

/**
 * Fires a ranged attack at a target entity whenever it's within range.
 *
 * <p>{@link AITaskComponent} only ever runs the single highest-priority task each frame, so giving
 * this task a higher priority than a movement task like {@link ChaseTask} means the owning entity
 * automatically stops closing the distance and starts attacking once in range, then resumes chasing
 * the moment the target moves back out of range — no changes are needed to the movement tasks
 * themselves.
 *
 * <p>Does not itself apply damage, knockback, or enforce a cooldown — it just triggers a {@code
 * "rangedAttack"} event every frame while in range. A {@link RangedAttackComponent} on the same
 * entity is what actually resolves the attack (including its own cooldown check), so triggering the
 * event repeatedly here is safe and intentional.
 */
public class RangedAttackTask extends DefaultTask implements PriorityTask {
  private final Entity target;
  private final int priority;
  private final float range;

  /**
   * @param target The entity to attack once in range.
   * @param priority Task priority while in range. Should be higher than any movement task's
   *     priority (e.g. {@link ChaseTask}) so the entity stops moving to attack instead of walking
   *     through its target.
   * @param range Distance from the target at which this task becomes active. Should normally match
   *     the {@link RangedAttackComponent}'s configured range on the same entity.
   */
  public RangedAttackTask(Entity target, int priority, float range) {
    this.target = target;
    this.priority = priority;
    this.range = range;
  }

  @Override
  public void start() {
    super.start();
    owner.getEntity().getEvents().trigger("rangedAttackStart");
  }

  @Override
  public void update() {
    owner.getEntity().getEvents().trigger("rangedAttack", target);
  }

  @Override
  public int getPriority() {
    return isInRange() ? priority : -1;
  }

  private boolean isInRange() {
    return owner.getEntity().getPosition().dst(target.getPosition()) <= range;
  }
}
