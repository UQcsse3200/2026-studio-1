package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.entities.Entity;

/**
 * AI priority task governing aggro-and-charge behaviour, shared by Minotaur
 * and Centaur. Manages only the charge itself — has no opinion on what
 * attack follows; that's decided by whichever attack component (Melee or
 * Ranged) is also attached. Active/inactive priority split mirrors
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
   * @param target           entity this task tracks and charges toward (typically the player)
   * @param chargeComponent  the charge state this task drives; must already be attached to the same entity
   * @param aggroRadius      distance within which this task becomes active
   * @param activePriority   priority returned while the target is within aggroRadius and alive
   * @param inactivePriority priority returned otherwise
   */
  public ChargeTask(Entity target, ChargeComponent chargeComponent, float aggroRadius,
                    int activePriority, int inactivePriority) {
    this.target = target;
    this.chargeComponent = chargeComponent;
    this.aggroRadius = aggroRadius;
    this.activePriority = activePriority;
    this.inactivePriority = inactivePriority;
  }

  /**
   * Determines this task's current priority based only on distance and target liveness — never
   * on {@code chargeComponent}'s cooldown state, per the Sprint 1 priority-flicker fix.
   *
   * @return activePriority if the target is alive and within aggroRadius, otherwise inactivePriority
   */
  @Override
  public int getPriority() {
//    IF target IS NULL OR target has no CombatStatsComponent OR target.getCombatStatsComponent().getHealth() <= 0 THEN
//    RETURN inactivePriority
//    END IF
//    distance = DISTANCE(owner.getPosition(), target.getPosition())
//    IF distance <= aggroRadius THEN RETURN activePriority END IF
//    RETURN inactivePriority
    if (this.target == null || this.target.getComponent(CombatStatsComponent.class) == null
        || this.target.getComponent(CombatStatsComponent.class).getHealth() <= 0) {
      return this.inactivePriority;
    }
    float distance = owner.getEntity().getPosition().dst(target.getPosition());
    if (distance <= this.aggroRadius) {
      return this.activePriority;
    }
    return this.inactivePriority;
  }

  /**
   * Starts a charge toward the target when off cooldown and not already charging;
   * otherwise a no-op.
   */
  @Override
  public void update() {
    if (this.target == null || target.getComponent(CombatStatsComponent.class).getHealth() <= 0) {
      return;
    }
    if (chargeComponent.isCharging()) {
      return;
    }
    if (chargeComponent.canCharge()) {
      chargeComponent.startCharge(target.getPosition());
    }
  }
}