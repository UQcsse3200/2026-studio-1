package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a centaur entity's state and plays the animation when
 * one of the events is triggered. It also updates the animation state based on velocity, attacks,
 * and charging.
 */
public class CentaurAnimationController extends Component {
  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private ChargeComponent chargeComponent;
  private AnimationState currentAnimState = null;
  private boolean isSwinging = false;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    RUN_LEFT,
    RUN_RIGHT,
    SWING_LEFT,
    SWING_RIGHT,
    DEATH_LEFT,
    DEATH_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);
    chargeComponent = this.entity.getComponent(ChargeComponent.class);

    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener("idleRightStart", this::animateIdleR);
    entity.getEvents().addListener("runLeftStart", this::animateRunL);
    entity.getEvents().addListener("runRightStart", this::animateRunR);
    entity.getEvents().addListener("swingLeftStart", this::animateSwingL);
    entity.getEvents().addListener("swingRightStart", this::animateSwingR);
    entity.getEvents().addListener("deathLeftStart", this::animateDeathL);
    entity.getEvents().addListener("deathRightStart", this::animateDeathR);
    entity.getEvents().addListener("meleeAttack", (Entity target) -> onAttack(target));
    entity.getEvents().addListener("meleeAttackWindup", (Entity target) -> onAttack(target));
    entity.getEvents().addListener("rangedAttackFired", (Entity target) -> onAttack(target));
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

  private void onAttack(Entity target) {
    if (target != null && target.getPosition() != null && entity != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.SWING_LEFT
              : AnimationState.SWING_RIGHT;
    } else {
      if (currentAnimState == AnimationState.RUN_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT
          || currentAnimState == AnimationState.SWING_LEFT
          || currentAnimState == AnimationState.DEATH_LEFT) {
        currentAnimState = AnimationState.SWING_LEFT;
      } else {
        currentAnimState = AnimationState.SWING_RIGHT;
      }
    }
    isSwinging = true;
    triggerStateEvent(currentAnimState);
  }

  private void onChargeStart() {
    isSwinging = false;
    Vector2 targetPos = chargeComponent != null ? chargeComponent.getTargetPosition() : null;
    if (targetPos != null && entity != null) {
      currentAnimState =
          (targetPos.x < entity.getPosition().x)
              ? AnimationState.RUN_LEFT
              : AnimationState.RUN_RIGHT;
    } else {
      Vector2 velocity =
          (physicsComponent != null && physicsComponent.getBody() != null)
              ? physicsComponent.getBody().getLinearVelocity()
              : null;

      if (velocity != null && velocity.x < -0.05f) {
        currentAnimState = AnimationState.RUN_LEFT;
      } else if (velocity != null && velocity.x > 0.05f) {
        currentAnimState = AnimationState.RUN_RIGHT;
      } else {
        if (currentAnimState == AnimationState.RUN_LEFT
            || currentAnimState == AnimationState.IDLE_LEFT
            || currentAnimState == AnimationState.SWING_LEFT
            || currentAnimState == AnimationState.DEATH_LEFT) {
          currentAnimState = AnimationState.RUN_LEFT;
        } else {
          currentAnimState = AnimationState.RUN_RIGHT;
        }
      }
    }
    triggerStateEvent(currentAnimState);
  }

  private AnimationState getTargetState(Vector2 velocity) {
    final float runThreshold = 0.05f;
    if (velocity.x < -runThreshold) {
      return AnimationState.RUN_LEFT;
    } else if (velocity.x > runThreshold) {
      return AnimationState.RUN_RIGHT;
    } else {
      if (currentAnimState == AnimationState.RUN_LEFT
          || currentAnimState == AnimationState.IDLE_LEFT
          || currentAnimState == AnimationState.SWING_LEFT
          || currentAnimState == AnimationState.DEATH_LEFT) {
        return AnimationState.IDLE_LEFT;
      } else {
        return AnimationState.IDLE_RIGHT;
      }
    }
  }

  private void triggerStateEvent(AnimationState state) {
    switch (state) {
      case SWING_LEFT:
        entity.getEvents().trigger("swingLeftStart");
        break;
      case SWING_RIGHT:
        entity.getEvents().trigger("swingRightStart");
        break;
      case RUN_LEFT:
        entity.getEvents().trigger("runLeftStart");
        break;
      case RUN_RIGHT:
        entity.getEvents().trigger("runRightStart");
        break;
      case IDLE_LEFT:
        entity.getEvents().trigger("idleLeftStart");
        break;
      case IDLE_RIGHT:
        entity.getEvents().trigger("idleRightStart");
        break;
      case DEATH_LEFT:
        entity.getEvents().trigger("deathLeftStart");
        break;
      case DEATH_RIGHT:
        entity.getEvents().trigger("deathRightStart");
        break;
    }
  }

  void animateIdleL() {
    animator.startAnimation("centaur_idle_l");
  }

  void animateIdleR() {
    animator.startAnimation("centaur_idle_r");
  }

  void animateRunL() {
    animator.startAnimation("centaur_run_l");
  }

  void animateRunR() {
    animator.startAnimation("centaur_run_r");
  }

  void animateSwingL() {
    isSwinging = true;
    if (animator != null) {
      animator.startAnimation("centaur_swing_l");
    }
  }

  void animateSwingR() {
    isSwinging = true;
    if (animator != null) {
      animator.startAnimation("centaur_swing_r");
    }
  }

  void animateDeathL() {
    if (animator != null) {
      animator.startAnimation("centaur_death_l");
    }
  }

  void animateDeathR() {
    if (animator != null) {
      animator.startAnimation("centaur_death_r");
    }
  }

  public boolean isSwinging() {
    return isSwinging;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.IDLE_RIGHT
        || currentAnimState == AnimationState.RUN_RIGHT
        || currentAnimState == AnimationState.SWING_RIGHT
        || currentAnimState == AnimationState.DEATH_RIGHT;
  }
}
