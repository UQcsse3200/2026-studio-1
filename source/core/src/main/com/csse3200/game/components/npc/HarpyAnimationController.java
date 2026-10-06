package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Harpy (melee or ranged) entity's state and plays the
 * corresponding movement animation (left or right). It updates facing direction and animation state
 * based on velocity and attacks.
 */
public class HarpyAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;

  public enum AnimationState {
    MOVE_LEFT,
    MOVE_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    entity.getEvents().addListener("walkLeftStart", this::animateMoveL);
    entity.getEvents().addListener("walkRightStart", this::animateMoveR);
    entity.getEvents().addListener("moveLeftStart", this::animateMoveL);
    entity.getEvents().addListener("moveRightStart", this::animateMoveR);
    entity.getEvents().addListener("flyLeftStart", this::animateMoveL);
    entity.getEvents().addListener("flyRightStart", this::animateMoveR);
    entity.getEvents().addListener("idleLeftStart", this::animateMoveL);
    entity.getEvents().addListener("idleRightStart", this::animateMoveR);

    // Melee attack listeners
    entity.getEvents().addListener("meleeAttack", this::onAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onAttack);

    // Ranged attack listeners
    entity.getEvents().addListener("rangedAttackWindup", this::onAttack);
    entity.getEvents().addListener("rangedAttackFired", this::onAttack);
    entity
        .getEvents()
        .addListener(
            "rangedAttack", (Entity target, ProjectileType projectile) -> onAttack(target));

    // Trigger a default starting state
    entity.getEvents().trigger("moveRightStart");
    currentAnimState = AnimationState.MOVE_RIGHT;
  }

  @Override
  public void update() {
    if (physicsComponent != null && physicsComponent.getBody() != null) {
      Vector2 velocity = physicsComponent.getBody().getLinearVelocity();
      if (velocity != null) {
        AnimationState targetState = getTargetState(velocity);
        if (targetState != currentAnimState) {
          currentAnimState = targetState;
          triggerStateEvent(currentAnimState);
        }
      }
    }
  }

  private AnimationState getTargetState(Vector2 velocity) {
    final float moveThreshold = 0.05f;
    if (velocity.x < -moveThreshold) {
      return AnimationState.MOVE_LEFT;
    } else if (velocity.x > moveThreshold) {
      return AnimationState.MOVE_RIGHT;
    } else {
      return currentAnimState == null ? AnimationState.MOVE_RIGHT : currentAnimState;
    }
  }

  private void triggerStateEvent(AnimationState state) {
    switch (state) {
      case MOVE_LEFT:
        entity.getEvents().trigger("moveLeftStart");
        break;
      case MOVE_RIGHT:
        entity.getEvents().trigger("moveRightStart");
        break;
    }
  }

  private void onAttack(Entity target) {
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.MOVE_LEFT
              : AnimationState.MOVE_RIGHT;
    }
    triggerStateEvent(currentAnimState);
  }

  void animateMoveL() {
    currentAnimState = AnimationState.MOVE_LEFT;
    if (animator != null) {
      animator.startAnimation("harpy_y_l");
    }
  }

  void animateMoveR() {
    currentAnimState = AnimationState.MOVE_RIGHT;
    if (animator != null) {
      animator.startAnimation("harpy_y_r");
    }
  }

  public boolean isFacingLeft() {
    return currentAnimState == AnimationState.MOVE_LEFT;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.MOVE_RIGHT;
  }

  public AnimationState getCurrentState() {
    return currentAnimState;
  }
}
