package com.csse3200.game.components.tasks;

import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.entities.Entity;

/** Starts retaliation attacks on the player while provoked and they are within range. */
public class RetaliateTask extends StandStillTask {
  private final Entity player;
  private final int priority;
  private final float range;
  private final String attackEvent;

  /**
   * @param player The entity to attack.
   * @param priority Task priority while an attack is ready, in progress, or between repeats.
   * @param range Distance from the player at which an attack can start.
   * @param attackEvent Event triggered on this entity to start the attack.
   */
  public RetaliateTask(Entity player, int priority, float range, String attackEvent) {
    super(player, priority);
    this.player = player;
    this.priority = priority;
    this.range = range;
    this.attackEvent = attackEvent;
  }

  @Override
  public void update() {
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    if (provoked != null && provoked.isAttacking()) {
      return;
    }
    super.update();
    if (provoked != null && provoked.isAttackPending() && isInRange()) {
      owner.getEntity().getEvents().trigger(attackEvent, player);
      provoked.useAttack();
    }
  }

  @Override
  public int getPriority() {
    return isAttacking() || isHoldingFight() || (isAttackReady() && isInRange()) ? priority : -1;
  }

  /** Checks whether a retaliation attack is currently in progress. */
  private boolean isAttacking() {
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    return provoked != null && provoked.isAttacking();
  }

  /** Checks whether this entity is between repeated attacks and should stay in fight mode. */
  private boolean isHoldingFight() {
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    return player != null && provoked != null && provoked.isHoldingFight();
  }

  /** Checks whether this fight's next attack is ready to start. */
  private boolean isAttackReady() {
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    return player != null && provoked != null && provoked.isAttackPending();
  }

  /** Checks whether the player is within attack range. */
  private boolean isInRange() {
    return owner.getEntity().getPosition().dst(player.getPosition()) <= range;
  }
}
