package com.csse3200.game.components.enemies.animation;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.enemies.attacks.ChargeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a minotaur entity's state and plays the animation when
 * one of the events is triggered. It also updates the animation state based on velocity, charging,
 * and melee attacks.
 */
public class MinotaurAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private ChargeComponent chargeComponent;
  private AnimationState currentAnimState = null;
  private boolean isSwinging = false;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT,
    CHARGE_LEFT,
    CHARGE_RIGHT,
    SWING_LEFT,
    SWING_RIGHT
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
    entity.getEvents().addListener("swingLeftStart", this::animateSwingL);
    entity.getEvents().addListener("swingRightStart", this::animateSwingR);
    entity.getEvents().addListener("meleeAttack", this::onMeleeAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onMeleeAttack);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (chargeComponent == null && entity != null) {
      chargeComponent = entity.getComponent(ChargeComponent.class);
    }
    if (isSwinging) {
      if (animator != null && animator.isFinished()) {
        isSwinging = false;
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

  private void onMeleeAttack(Entity target) {
    if (target != null && target.getPosition() != null && entity != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.SWING_LEFT
              : AnimationState.SWING_RIGHT;
    } else {
      if (currentAnimState == AnimationState.WALK_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT
          || currentAnimState == AnimationState.CHARGE_LEFT
          || currentAnimState == AnimationState.SWING_LEFT) {
        currentAnimState = AnimationState.SWING_LEFT;
      } else {
        currentAnimState = AnimationState.SWING_RIGHT;
      }
    }
    isSwinging = true;
    triggerStateEvent(currentAnimState);
  }

  /** Starts the charge animation facing the target, the velocity, or the current facing. */
  private void onChargeStart() {
    isSwinging = false;
    Vector2 targetPos = chargeComponent != null ? chargeComponent.getTargetPosition() : null;
    if (targetPos != null && entity != null) {
      currentAnimState = chargeStateTowards(targetPos.x);
    } else {
      currentAnimState = chargeStateFromVelocityOrFacing();
    }
    triggerStateEvent(currentAnimState);
  }

  /** Returns the charge state that faces the given x position. */
  private AnimationState chargeStateTowards(float targetX) {
    return targetX < entity.getPosition().x
        ? AnimationState.CHARGE_LEFT
        : AnimationState.CHARGE_RIGHT;
  }

  /** Returns the charge state from the velocity, or from the current facing when still. */
  private AnimationState chargeStateFromVelocityOrFacing() {
    final float chargeThreshold = 0.05f;
    Vector2 velocity = currentVelocityOrNull();
    if (velocity != null && velocity.x < -chargeThreshold) {
      return AnimationState.CHARGE_LEFT;
    }
    if (velocity != null && velocity.x > chargeThreshold) {
      return AnimationState.CHARGE_RIGHT;
    }
    return isFacingLeftState() ? AnimationState.CHARGE_LEFT : AnimationState.CHARGE_RIGHT;
  }

  /** Returns the current linear velocity, or null when there is no physics body. */
  private Vector2 currentVelocityOrNull() {
    if (physicsComponent != null && physicsComponent.getBody() != null) {
      return physicsComponent.getBody().getLinearVelocity();
    }
    return null;
  }

  /** Returns true when the current state faces left. */
  private boolean isFacingLeftState() {
    return currentAnimState == AnimationState.WALK_LEFT
        || currentAnimState == AnimationState.IDLE_LEFT
        || currentAnimState == AnimationState.CHARGE_LEFT
        || currentAnimState == AnimationState.SWING_LEFT;
  }

  /** Returns the state to show for this frame given the velocity. */
  private AnimationState getTargetState(Vector2 velocity) {
    if (isChargeWindup()) {
      return chargeStateDuringWindup();
    }
    return stateFromVelocity(velocity);
  }

  /** Returns true during the stationary windup phase of a charge. */
  private boolean isChargeWindup() {
    return chargeComponent != null && chargeComponent.isCharging() && !chargeComponent.isRushing();
  }

  /** Returns the charge state to show during the windup. */
  private AnimationState chargeStateDuringWindup() {
    Vector2 targetPos = chargeComponent.getTargetPosition();
    if (targetPos != null && entity != null) {
      return chargeStateTowards(targetPos.x);
    }
    return isFacingLeftState() ? AnimationState.CHARGE_LEFT : AnimationState.CHARGE_RIGHT;
  }

  /** Returns the walk or idle state that matches the velocity. */
  private AnimationState stateFromVelocity(Vector2 velocity) {
    final float walkThreshold = 0.05f;
    if (velocity.x < -walkThreshold) {
      return AnimationState.WALK_LEFT;
    }
    if (velocity.x > walkThreshold) {
      return AnimationState.WALK_RIGHT;
    }
    return isFacingLeftState() ? AnimationState.IDLE_LEFT : AnimationState.IDLE_RIGHT;
  }

  private void triggerStateEvent(AnimationState state) {
    switch (state) {
      case SWING_LEFT:
        entity.getEvents().trigger("swingLeftStart");
        break;
      case SWING_RIGHT:
        entity.getEvents().trigger("swingRightStart");
        break;
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

  void animateSwingL() {
    isSwinging = true;
    if (animator != null) {
      animator.startAnimation("minotaur_swing_l");
    }
  }

  void animateSwingR() {
    isSwinging = true;
    if (animator != null) {
      animator.startAnimation("minotaur_swing_r");
    }
  }

  public boolean isSwinging() {
    return isSwinging;
  }
}
