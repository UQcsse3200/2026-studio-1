package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Cerberus entity's state and plays the corresponding
 * walking animation (left or right). It updates facing direction and animation state based on
 * velocity and melee attacks.
 */
public class CerberusAnimationController extends Component {
  private static final String STARTING_STATE = "walkRightStart";

  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;

  public enum AnimationState {
    WALK_LEFT,
    WALK_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);
    entity.getEvents().addListener("idleLeftStart", this::animateWalkL);
    entity.getEvents().addListener("idleRightStart", this::animateWalkR);
    entity.getEvents().addListener("meleeAttack", this::onAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onAttack);

    // Trigger a default starting state
    entity.getEvents().trigger(STARTING_STATE);
    currentAnimState = AnimationState.WALK_RIGHT;
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
    final float walkThreshold = 0.05f;
    if (velocity.x < -walkThreshold) {
      return AnimationState.WALK_LEFT;
    } else if (velocity.x > walkThreshold) {
      return AnimationState.WALK_RIGHT;
    } else {
      return currentAnimState == null ? AnimationState.WALK_RIGHT : currentAnimState;
    }
  }

  private void triggerStateEvent(AnimationState state) {
    switch (state) {
      case WALK_LEFT:
        entity.getEvents().trigger("walkLeftStart");
        break;
      case WALK_RIGHT:
        entity.getEvents().trigger("walkRightStart");
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
              ? AnimationState.WALK_LEFT
              : AnimationState.WALK_RIGHT;
    }
    triggerStateEvent(currentAnimState);
  }

  void animateWalkL() {
    currentAnimState = AnimationState.WALK_LEFT;
    if (animator != null) {
      animator.startAnimation("cerberus_l");
    }
  }

  void animateWalkR() {
    currentAnimState = AnimationState.WALK_RIGHT;
    if (animator != null) {
      animator.startAnimation("cerberus_r");
    }
  }

  public boolean isFacingLeft() {
    return currentAnimState == AnimationState.WALK_LEFT;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.WALK_RIGHT;
  }

  public AnimationState getCurrentState() {
    return currentAnimState;
  }
}
