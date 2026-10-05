package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;

/**
 * Fires a ranged attack at a target entity whenever it's within range.
 *
 * <p>{@link AITaskComponent} only ever runs the single highest-priority task each frame, so giving
 * this task a higher priority than a movement task like {@link ChaseTask} means the owning entity
 * automatically stops closing the distance and starts attacking once in range, then resumes chasing
 * the moment the target moves back out of range. No changes are needed to the movement tasks
 * themselves.
 *
 * <p>Does not itself apply damage, knockback, or enforce a cooldown. It just triggers an event
 * every frame while in range, {@code "rangedAttack"} unless another event name is given. A {@link
 * RangedAttackComponent} on the same entity that listens for that name is what actually resolves
 * the attack (including its own cooldown check), so triggering the event repeatedly here is safe
 * and intentional. An entity with two ranged components uses one task per component, each with the
 * event name of its own component.
 */
public class RangedAttackTask extends DefaultTask implements PriorityTask {
  private static final String DEFAULT_EVENT_NAME = "rangedAttack";

  private final Entity target;
  private final int priority;
  private final float range;
  private final ProjectileType projectile;
  private final String eventName;

  /**
   * Creates a task that triggers {@code "rangedAttack"}.
   *
   * @param target The entity to attack once in range.
   * @param priority Task priority while in range. Should be higher than any movement task's
   *     priority (e.g. {@link ChaseTask}) so the entity stops moving to attack instead of walking
   *     through its target.
   * @param range Distance from the target at which this task becomes active. Should normally match
   *     the {@link RangedAttackComponent}'s configured range on the same entity.
   * @param projectile Type of projectile being launched
   */
  public RangedAttackTask(Entity target, int priority, float range, ProjectileType projectile) {
    this(target, priority, range, projectile, DEFAULT_EVENT_NAME);
  }

  /**
   * Creates a ranged attack task that triggers a chosen event name.
   *
   * @param target The entity to attack once in range.
   * @param priority Task priority while in range.
   * @param range Distance from the target at which this task becomes active.
   * @param projectile Type of projectile being launched
   * @param eventName the event the owner's ranged component listens for, for example {@code
   *     "laserAttack"}; not blank
   * @throws IllegalArgumentException if the event name is null or blank
   */
  public RangedAttackTask(
      Entity target, int priority, float range, ProjectileType projectile, String eventName) {
    if (eventName == null || eventName.isBlank()) {
      throw new IllegalArgumentException("eventName must not be blank");
    }
    this.target = target;
    this.priority = priority;
    this.range = range;
    this.projectile = projectile;
    this.eventName = eventName;
  }

  /** Announces eventName + "Start" when the task starts. */
  @Override
  public void start() {
    super.start();
    this.eventName.concat("Start");
    owner.getEntity().getEvents().trigger(eventName);
  }

  /** Triggers the stored event name with the target and the projectile type. */
  @Override
  public void update() {
    owner.getEntity().getEvents().trigger(eventName, target, projectile);
  }

  @Override
  public int getPriority() {
    return isInRange() ? priority : -1;
  }

  /**
   * @return the event name this task triggers
   */
  public String getEventName() {
    return eventName;
  }

  private boolean isInRange() {
    return owner.getEntity().getPosition().dst(target.getPosition()) <= range;
  }
}
