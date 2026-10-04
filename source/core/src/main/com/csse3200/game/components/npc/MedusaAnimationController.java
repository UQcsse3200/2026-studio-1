package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to Medusa's state and plays the animation when one of the
 * events is triggered. It also updates the animation state based on velocity and attacks (melee
 * bite and ranged gaze), driven by the real {@code gorgon} atlas, which only has idle, walk, attack
 * and dead animations (no separate rock/laser/stomp states like the Cyclops).
 */
public class MedusaAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;
  private boolean isAttacking = false;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT,
    ATTACK_LEFT,
    ATTACK_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    // Directional movement & idle listeners
    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener("idleRightStart", this::animateIdleR);
    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);

    // Attack listeners - melee bite and ranged gaze both play the same attack animation, since
    // the gorgon atlas has one attack animation per side rather than separate melee/ranged ones.
    entity.getEvents().addListener("attackLeftStart", this::animateAttackL);
    entity.getEvents().addListener("attackRightStart", this::animateAttackR);
    entity.getEvents().addListener("meleeAttackWindup", this::onAttack);
    entity.getEvents().addListener("meleeAttack", this::onAttack);
    entity.getEvents().addListener("rangedAttackWindup", this::onAttack);
    entity.getEvents().addListener("rangedAttackFired", this::onAttack);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (isAttacking) {
      boolean finished = (animator == null || animator.isFinished());
      if (finished) {
        isAttacking = false;
        if (physicsComponent == null || physicsComponent.getBody() == null) {
          currentAnimState =
              isFacingLeft(currentAnimState) ? AnimationState.IDLE_LEFT : AnimationState.IDLE_RIGHT;
          triggerStateEvent(currentAnimState);
        }
      } else {
        return;
      }
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

  private boolean isFacingLeft(AnimationState state) {
    return state == AnimationState.WALK_LEFT
        || state == AnimationState.IDLE_LEFT
        || state == AnimationState.ATTACK_LEFT;
  }

  private boolean isTargetToLeft(Entity target) {
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      return target.getPosition().x < entity.getPosition().x;
    }
    return isFacingLeft(currentAnimState);
  }

  private AnimationState getTargetState(Vector2 velocity) {
    final float walkThreshold = 0.05f;
    if (velocity.x < -walkThreshold) {
      return AnimationState.WALK_LEFT;
    } else if (velocity.x > walkThreshold) {
      return AnimationState.WALK_RIGHT;
    } else {
      if (isFacingLeft(currentAnimState)) {
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
      case ATTACK_LEFT:
        entity.getEvents().trigger("attackLeftStart");
        break;
      case ATTACK_RIGHT:
        entity.getEvents().trigger("attackRightStart");
        break;
    }
  }

  private void onAttack(Entity target) {
    if (isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.ATTACK_LEFT : AnimationState.ATTACK_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  void animateIdleL() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_LEFT;
    if (animator != null) {
      animator.startAnimation("gorgon_idle_l");
    }
  }

  void animateIdleR() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_RIGHT;
    if (animator != null) {
      animator.startAnimation("gorgon_idle_r");
    }
  }

  void animateWalkL() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_LEFT;
    if (animator != null) {
      animator.startAnimation("gorgon_walk_l");
    }
  }

  void animateWalkR() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_RIGHT;
    if (animator != null) {
      animator.startAnimation("gorgon_walk_r");
    }
  }

  void animateAttackL() {
    isAttacking = true;
    currentAnimState = AnimationState.ATTACK_LEFT;
    if (animator != null) {
      animator.startAnimation("gorgon_attack_l");
    }
  }

  void animateAttackR() {
    isAttacking = true;
    currentAnimState = AnimationState.ATTACK_RIGHT;
    if (animator != null) {
      animator.startAnimation("gorgon_attack_r");
    }
  }

  public boolean isAttacking() {
    return isAttacking;
  }

  public boolean isFacingLeft() {
    return isFacingLeft(currentAnimState);
  }

  public boolean isFacingRight() {
    return !isFacingLeft(currentAnimState);
  }
}
