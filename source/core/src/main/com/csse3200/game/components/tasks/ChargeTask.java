package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;

/**
 * AI priority task governing aggro-and-charge behaviour, shared by Minotaur and Centaur. Manages
 * only the charge itself — has no opinion on what attack follows; that's decided by whichever
 * attack component (Melee or Ranged) is also attached. Active/inactive priority split mirrors
 * MeleeAttackTask's, per the Sprint 1 flicker fix.
 */
public class ChargeTask extends DefaultTask implements PriorityTask {
  private final Entity target;
  private final ChargeComponent chargeComponent;
  private final float aggroRadius;
  private final int activePriority;
  private final int inactivePriority;

  /**
   * Creates a charge task.
   *
   * @param target entity this task tracks and charges toward (typically the player)
   * @param chargeComponent the charge state this task drives; must already be attached to the same
   *     entity
   * @param aggroRadius distance within which this task becomes active
   * @param activePriority priority returned while the target is within aggroRadius and alive
   * @param inactivePriority priority returned otherwise
   */
  public ChargeTask(
      Entity target,
      ChargeComponent chargeComponent,
      float aggroRadius,
      int activePriority,
      int inactivePriority) {
    this.target = target;
    this.chargeComponent = chargeComponent;
    this.aggroRadius = aggroRadius;
    this.activePriority = activePriority;
    this.inactivePriority = inactivePriority;
  }

  @Override
  public void start() {
    super.start();
    if (chargeComponent != null && chargeComponent.canCharge() && target != null) {
      chargeComponent.startCharge(target.getPosition());
    }
  }

  /**
   * Determines this task's current priority. Returns activePriority while actively charging, or
   * when ready to charge and the target is alive and within aggroRadius; otherwise returns
   * inactivePriority so other tasks (such as ChaseTask or MeleeAttackTask) can run during cooldown.
   *
   * @return activePriority if charging or ready to charge within aggroRadius, otherwise
   *     inactivePriority
   */
  @Override
  public int getPriority() {
    if (this.target == null
        || this.target.getComponent(CombatStatsComponent.class) == null
        || this.target.getComponent(CombatStatsComponent.class).getHealth() <= 0) {
      return this.inactivePriority;
    }
    if (chargeComponent != null && chargeComponent.isCharging()) {
      return this.activePriority;
    }
    if (chargeComponent != null && chargeComponent.canCharge()) {
      float distance = owner.getEntity().getPosition().dst(target.getPosition());
      if (distance <= this.aggroRadius) {
        return this.activePriority;
      }
    }
    return this.inactivePriority;
  }

  /**
   * Starts a charge toward the target when off cooldown and not already charging; otherwise don't
   * do anything.
   */
  @Override
  public void update() {
    if (this.target == null
        || target.getComponent(CombatStatsComponent.class) == null
        || target.getComponent(CombatStatsComponent.class).getHealth() <= 0) {
      return;
    }
    if (chargeComponent == null || chargeComponent.isCharging()) {
      return;
    }
    if (chargeComponent.canCharge()) {
      chargeComponent.startCharge(target.getPosition());
    }
  }

  @Override
  public void stop() {
    super.stop();
    if (owner != null && owner.getEntity() != null) {
      PhysicsMovementComponent movement =
          owner.getEntity().getComponent(PhysicsMovementComponent.class);
      if (movement != null) {
        movement.setSpeedMultiplier(1.0f);
        movement.setMoving(false);
      }
    }
  }
}
