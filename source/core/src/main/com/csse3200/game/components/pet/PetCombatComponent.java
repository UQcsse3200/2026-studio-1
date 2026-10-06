package com.csse3200.game.components.pet;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Repeatedly attacks the enemy selected by confirmed player hits.
 *
 * <p>Hits are queued until {@link #update()} so listeners can safely create projectile physics
 * bodies outside collision callbacks. The pet retains the selected enemy and emits {@code
 * petAttack(Entity target)} on its own entity every cooldown until that enemy dies or is removed. A
 * new player hit selects a new target without resetting the cooldown. Projectile creation and
 * damage are handled separately.
 */
public class PetCombatComponent extends Component {
  private static final float ATTACK_COOLDOWN = 0.8f;

  private final GameTime timeSource;
  private Entity owner;
  private Entity pendingTarget;
  private Entity currentTarget;
  private float cooldownRemaining;

  /** Creates pet combat using the game's time source. */
  public PetCombatComponent() {
    this(ServiceLocator.getTimeSource());
  }

  PetCombatComponent(GameTime timeSource) {
    if (timeSource == null) {
      throw new IllegalArgumentException("Pet combat requires a time source");
    }
    this.timeSource = timeSource;
  }

  @Override
  public void create() {
    PetComponent petComponent = entity.getComponent(PetComponent.class);
    if (petComponent == null) {
      throw new IllegalStateException("PetCombatComponent requires a PetComponent");
    }
    owner = petComponent.getOwner();
  }

  /**
   * Queues a target selection from an enemy just hit by the owner, without modifying physics.
   *
   * <p>If several enemies are hit before the next update, prefer the nearest living one; equal
   * distances are resolved by entity ID. A lethal hit with no surviving candidate clears the old
   * target. Only confirmed hit targets are considered, so other nearby enemies cannot steal focus.
   *
   * @param target enemy reported by the owner's {@code playerAttackHit} event
   */
  public void requestAttack(Entity target) {
    if (!enabled
        || entity.isDisposed()
        || !isAlive(owner)
        || target == null
        || target == owner
        || target == entity
        || target.getComponent(CombatStatsComponent.class) == null) {
      return;
    }

    if (pendingTarget == null
        || (isAlive(target) && (!isAlive(pendingTarget) || preferTarget(target)))) {
      pendingTarget = target;
    }
  }

  @Override
  public void update() {
    if (!enabled || entity.isDisposed() || !isAlive(owner)) {
      clearTarget();
      return;
    }

    float deltaTime = Math.max(0f, timeSource.getDeltaTime());
    cooldownRemaining = Math.max(0f, cooldownRemaining - deltaTime);

    // Accept target changes during cooldown; subsequent shots use the latest selection.
    if (pendingTarget != null) {
      currentTarget = pendingTarget;
      pendingTarget = null;
    }
    if (!isAlive(currentTarget)) {
      currentTarget = null;
      return;
    }
    if (deltaTime <= 0f || cooldownRemaining > 0f) {
      return;
    }

    cooldownRemaining = ATTACK_COOLDOWN;
    entity.getEvents().trigger("petAttack", currentTarget);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      clearTarget();
    }
  }

  @Override
  public void dispose() {
    setEnabled(false);
  }

  private void clearTarget() {
    pendingTarget = null;
    currentTarget = null;
  }

  private boolean preferTarget(Entity candidate) {
    float candidateDistance = owner.getCenterPosition().dst2(candidate.getCenterPosition());
    float pendingDistance = owner.getCenterPosition().dst2(pendingTarget.getCenterPosition());
    return candidateDistance < pendingDistance
        || (Float.compare(candidateDistance, pendingDistance) == 0
            && candidate.getId() < pendingTarget.getId());
  }

  private static boolean isAlive(Entity target) {
    if (target == null || target.isDisposed()) {
      return false;
    }
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    return stats != null && !stats.isDead();
  }
}
