package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a skeleton entity's state and plays the animation when
 * one of the events is triggered. It also updates the animation state based on velocity.
 */
public class SkeletonAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);
    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener("idleRightStart", this::animateIdleR);
    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (physicsComponent != null) {
      Vector2 velocity = physicsComponent.getBody().getLinearVelocity();
      AnimationState targetState = getTargetState(velocity);
      if (targetState != currentAnimState) {
        currentAnimState = targetState;
        triggerStateEvent(currentAnimState);
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
      if (currentAnimState == AnimationState.WALK_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT) {
        return AnimationState.IDLE_LEFT;
      } else {
        return AnimationState.IDLE_RIGHT;
      }
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
      case IDLE_LEFT:
        entity.getEvents().trigger("idleLeftStart");
        break;
      case IDLE_RIGHT:
        entity.getEvents().trigger("idleRightStart");
        break;
    }
  }

  void animateIdleL() {
    animator.startAnimation("idlel");
  }

  void animateIdleR() {
    animator.startAnimation("idler");
  }

  void animateWalkL() {
    animator.startAnimation("walkl");
  }

  void animateWalkR() {
    animator.startAnimation("walkr");
  }
}
