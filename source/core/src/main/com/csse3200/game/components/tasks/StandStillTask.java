package com.csse3200.game.components.tasks;

import com.csse3200.game.ai.tasks.DefaultTask;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

// Keeps a friendly NPC still and facing the player while they're in dialogue range.
public class StandStillTask extends DefaultTask implements PriorityTask {
  private final Entity player;
  private final int priority;

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
    DialogueProximityComponent proximity =
        owner.getEntity().getComponent(DialogueProximityComponent.class);
    if (player == null || proximity == null || !proximity.isPlayerInRange()) {
      return -1;
    }
    return priority;
  }

  // Shows the idle frame facing the player, via the animation controller's idle events.
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
