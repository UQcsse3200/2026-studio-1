package com.csse3200.game.components.tasks;

import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.entities.Entity;

/** Throws a poison potion at the player once per fight, while they are within range. */
public class RetaliateTask extends StandStillTask {
  private final Entity player;
  private final int priority;
  private final float range;

  /**
   * @param player The entity to throw at.
   * @param priority Task priority while a throw is ready and the player is in range.
   * @param range Distance from the player at which this task becomes active.
   */
  public RetaliateTask(Entity player, int priority, float range) {
    super(player, priority);
    this.player = player;
    this.priority = priority;
    this.range = range;
  }

  @Override
  public void update() {
    super.update();
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    if (provoked != null && provoked.isThrowPending()) {
      owner.getEntity().getEvents().trigger("throwPoisonPotion", player);
      provoked.useThrow();
    }
  }

  @Override
  public int getPriority() {
    return isThrowReady() && isInRange() ? priority : -1;
  }

  /** Checks whether this fight's throw has not been used yet. */
  private boolean isThrowReady() {
    ProvokedComponent provoked = owner.getEntity().getComponent(ProvokedComponent.class);
    return player != null && provoked != null && provoked.isThrowPending();
  }

  /** Checks whether the player is within throwing range. */
  private boolean isInRange() {
    return owner.getEntity().getPosition().dst(player.getPosition()) <= range;
  }
}
