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
  private static final String IDLE_LEFT_START = "idleLeftStart";
  private static final String IDLE_RIGHT_START = "idleRightStart";
  private static final String RUN_LEFT_START = "runLeftStart";
  private static final String RUN_RIGHT_START = "runRightStart";
  private static final String SWING_LEFT_START = "swingLeftStart";
  private static final String SWING_RIGHT_START = "swingRightStart";
  private static final String DEATH_LEFT_START = "deathLeftStart";
  private static final String DEATH_RIGHT_START = "deathRightStart";
  private static final String MELEE_ATTACK = "meleeAttack";
  private static final String MELEE_ATTACK_WINDUP = "meleeAttackWindup";
  private static final String RANGED_ATTACK_FIRED = "rangedAttackFired";
  private static final String CHARGE_START = "chargeStart";

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

    entity.getEvents().addListener(IDLE_LEFT_START, this::animateIdleL);
    entity.getEvents().addListener(IDLE_RIGHT_START, this::animateIdleR);
    entity.getEvents().addListener(RUN_LEFT_START, this::animateRunL);
    entity.getEvents().addListener(RUN_RIGHT_START, this::animateRunR);
    entity.getEvents().addListener(SWING_LEFT_START, this::animateSwingL);
    entity.getEvents().addListener(SWING_RIGHT_START, this::animateSwingR);
    entity.getEvents().addListener(DEATH_LEFT_START, this::animateDeathL);
    entity.getEvents().addListener(DEATH_RIGHT_START, this::animateDeathR);
    entity.getEvents().addListener(MELEE_ATTACK, (Entity target) -> onAttack(target));
    entity.getEvents().addListener(MELEE_ATTACK_WINDUP, (Entity target) -> onAttack(target));
    entity.getEvents().addListener(RANGED_ATTACK_FIRED, (Entity target) -> onAttack(target));
    entity.getEvents().addListener(CHARGE_START, this::onChargeStart);

    // Trigger a default starting state
    entity.getEvents().trigger(IDLE_RIGHT_START);
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
        entity.getEvents().trigger(SWING_LEFT_START);
        break;
      case SWING_RIGHT:
        entity.getEvents().trigger(SWING_RIGHT_START);
        break;
      case RUN_LEFT:
        entity.getEvents().trigger(RUN_LEFT_START);
        break;
      case RUN_RIGHT:
        entity.getEvents().trigger(RUN_RIGHT_START);
        break;
      case IDLE_LEFT:
        entity.getEvents().trigger(IDLE_LEFT_START);
        break;
      case IDLE_RIGHT:
        entity.getEvents().trigger(IDLE_RIGHT_START);
        break;
      case DEATH_LEFT:
        entity.getEvents().trigger(DEATH_LEFT_START);
        break;
      case DEATH_RIGHT:
        entity.getEvents().trigger(DEATH_RIGHT_START);
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
