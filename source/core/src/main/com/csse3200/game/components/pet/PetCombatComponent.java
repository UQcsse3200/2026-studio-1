package com.csse3200.game.components.pet;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;

/**
 * Converts confirmed player hits into individual pet attack requests.
 *
 * <p>Hits are queued until {@link #update()} so listeners can safely create projectile physics
 * bodies outside collision callbacks. A ready pet emits {@code petAttack(Entity target)} on its own
 * entity once, then waits for another player hit. Projectile creation and damage are handled
 * separately.
 */
public class PetCombatComponent extends Component {
  private static final float ATTACK_COOLDOWN = 0.8f;

  private final GameTime timeSource;
  private Entity owner;
  private Entity pendingTarget;
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
   * Queues an assist against an enemy just hit by the owner, without modifying the physics world.
   *
   * <p>If several enemies are hit before the next update, prefer the one nearest to the owner;
   * equal distances are resolved by entity ID. Only confirmed hit targets are considered. This
   * keeps area attacks independent of the order in which their hit events arrive.
   *
   * @param target enemy reported by the owner's {@code playerAttackHit} event
   */
  public void requestAttack(Entity target) {
    if (!enabled
        || entity.isDisposed()
        || !isAlive(owner)
        || target == owner
        || target == entity
        || !isAlive(target)) {
      return;
    }

    if (!isAlive(pendingTarget) || preferTarget(target)) {
      pendingTarget = target;
    }
  }

  @Override
  public void update() {
    float deltaTime = Math.max(0f, timeSource.getDeltaTime());
    cooldownRemaining = Math.max(0f, cooldownRemaining - deltaTime);

    // Consume the request even during cooldown, so it cannot cause a delayed automatic attack.
    Entity target = pendingTarget;
    pendingTarget = null;
    if (!enabled
        || entity.isDisposed()
        || cooldownRemaining > 0f
        || !isAlive(owner)
        || !isAlive(target)) {
      return;
    }

    cooldownRemaining = ATTACK_COOLDOWN;
    entity.getEvents().trigger("petAttack", target);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      pendingTarget = null;
    }
  }

  @Override
  public void dispose() {
    setEnabled(false);
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
