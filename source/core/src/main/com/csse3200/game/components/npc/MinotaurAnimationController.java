package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a minotaur entity's state and plays the animation when
 * one of the events is triggered. It also updates the animation state based on velocity and
 * charging.
 */
public class MinotaurAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private ChargeComponent chargeComponent;
  private AnimationState currentAnimState = null;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT,
    CHARGE_LEFT,
    CHARGE_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);
    chargeComponent = this.entity.getComponent(ChargeComponent.class);

    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener("idleRightStart", this::animateIdleR);
    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);
    entity.getEvents().addListener("chargeLeftStart", this::animateChargeL);
    entity.getEvents().addListener("chargeRightStart", this::animateChargeR);
    entity.getEvents().addListener("chargeStart", this::onChargeStart);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (chargeComponent == null && entity != null) {
      chargeComponent = entity.getComponent(ChargeComponent.class);
    }
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

  private void onChargeStart() {
    Vector2 targetPos = chargeComponent != null ? chargeComponent.getTargetPosition() : null;
    if (targetPos != null && entity != null) {
      currentAnimState =
          (targetPos.x < entity.getPosition().x)
              ? AnimationState.CHARGE_LEFT
              : AnimationState.CHARGE_RIGHT;
    } else {
      Vector2 velocity =
          (physicsComponent != null && physicsComponent.getBody() != null)
              ? physicsComponent.getBody().getLinearVelocity()
              : null;

      if (velocity != null && velocity.x < -0.05f) {
        currentAnimState = AnimationState.CHARGE_LEFT;
      } else if (velocity != null && velocity.x > 0.05f) {
        currentAnimState = AnimationState.CHARGE_RIGHT;
      } else {
        if (currentAnimState == AnimationState.WALK_LEFT
            || currentAnimState == AnimationState.IDLE_LEFT
            || currentAnimState == AnimationState.CHARGE_LEFT) {
          currentAnimState = AnimationState.CHARGE_LEFT;
        } else {
          currentAnimState = AnimationState.CHARGE_RIGHT;
        }
      }
    }
    triggerStateEvent(currentAnimState);
  }

  private AnimationState getTargetState(Vector2 velocity) {
    final float walkThreshold = 0.05f;
    boolean isCharging = chargeComponent != null && chargeComponent.isCharging();
    boolean isRushing = chargeComponent != null && chargeComponent.isRushing();

    // While in the stationary charging (windup) phase, display charge animation facing target
    if (isCharging && !isRushing) {
      Vector2 targetPos = chargeComponent.getTargetPosition();
      if (targetPos != null && entity != null) {
        return (targetPos.x < entity.getPosition().x)
            ? AnimationState.CHARGE_LEFT
            : AnimationState.CHARGE_RIGHT;
      }
      if (currentAnimState == AnimationState.WALK_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT
          || currentAnimState == AnimationState.CHARGE_LEFT) {
        return AnimationState.CHARGE_LEFT;
      } else {
        return AnimationState.CHARGE_RIGHT;
      }
    }

    if (velocity.x < -walkThreshold) {
      return AnimationState.WALK_LEFT;
    } else if (velocity.x > walkThreshold) {
      return AnimationState.WALK_RIGHT;
    } else {
      if (currentAnimState == AnimationState.WALK_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT
          || currentAnimState == AnimationState.CHARGE_LEFT) {
        return AnimationState.IDLE_LEFT;
      } else {
        return AnimationState.IDLE_RIGHT;
      }
    }
  }

  private void triggerStateEvent(AnimationState state) {
    switch (state) {
      case CHARGE_LEFT:
        entity.getEvents().trigger("chargeLeftStart");
        break;
      case CHARGE_RIGHT:
        entity.getEvents().trigger("chargeRightStart");
        break;
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
    animator.startAnimation("minotaur_idle_l");
  }

  void animateIdleR() {
    animator.startAnimation("minotaur_idle_r");
  }

  void animateWalkL() {
    animator.startAnimation("minotaur_walk_l");
  }

  void animateWalkR() {
    animator.startAnimation("minotaur_walk_r");
  }

  void animateChargeL() {
    animator.startAnimation("minotaur_charge_l");
  }

  void animateChargeR() {
    animator.startAnimation("minotaur_charge_r");
  }
}
