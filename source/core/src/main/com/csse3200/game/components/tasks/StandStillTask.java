package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/** Stands still and faces the player while they are within dialogue range. */
public class StandStillTask extends DefaultTask implements PriorityTask {
  private final Entity player;
  private final int priority;

  /**
   * @param player The entity to face.
   * @param priority Task priority while the player is in dialogue range.
   */
  public StandStillTask(Entity player, int priority) {
    this.player = player;
    this.priority = priority;
  }

  @Override
  public void start() {
    super.start();
    PhysicsMovementComponent movement =
        owner.getEntity().getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setMoving(false);
    }
  }

  @Override
  public void update() {
    facePlayer();
  }

  @Override
  public int getPriority() {
    return isPlayerInRange() ? priority : -1;
  }

  /** Checks whether the player is within this entity's dialogue range. */
  private boolean isPlayerInRange() {
    DialogueProximityComponent proximity =
        owner.getEntity().getComponent(DialogueProximityComponent.class);
    return player != null && proximity != null && proximity.isPlayerInRange();
  }

  /** Shows the idle animation facing the player. */
  private void facePlayer() {
    AnimationRenderComponent animator =
        owner.getEntity().getComponent(AnimationRenderComponent.class);
    if (animator == null || player == null) {
      return;
    }

    boolean playerOnLeft = player.getCenterPosition().x < owner.getEntity().getCenterPosition().x;
    String idleAnimation = playerOnLeft ? "idlel" : "idler";
    if (!idleAnimation.equals(animator.getCurrentAnimation())) {
      owner.getEntity().getEvents().trigger(playerOnLeft ? "idleLeftStart" : "idleRightStart");
    }
  }
}
