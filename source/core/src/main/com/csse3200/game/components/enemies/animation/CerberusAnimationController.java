package com.csse3200.game.components.enemies.animation;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Cerberus entity's state and plays the corresponding
 * animations (idle, walk, attack, death) from the cerberus atlas. It updates facing direction and
 * animation state based on velocity, melee attacks, and death events.
 */
public class CerberusAnimationController extends Component {
  private static final String IDLE_LEFT_START = "idleLeftStart";
  private static final String IDLE_RIGHT_START = "idleRightStart";
  private static final String WALK_LEFT_START = "walkLeftStart";
  private static final String WALK_RIGHT_START = "walkRightStart";
  private static final String ATTACK_LEFT_START = "attackLeftStart";
  private static final String ATTACK_RIGHT_START = "attackRightStart";
  private static final String DEATH_LEFT_START = "deathLeftStart";
  private static final String DEATH_RIGHT_START = "deathRightStart";
  private static final String MELEE_ATTACK = "meleeAttack";
  private static final String MELEE_ATTACK_WINDUP = "meleeAttackWindup";
  private static final String BITE = "bite";
  private static final String DEATH = "death";

  private AnimationRenderComponent animator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;
  private boolean isAttacking = false;
  private boolean isDead = false;

  public enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT,
    ATTACK_LEFT,
    ATTACK_RIGHT,
    DEATH_LEFT,
    DEATH_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    // Idle & walk listeners
    entity.getEvents().addListener(IDLE_LEFT_START, this::animateIdleL);
    entity.getEvents().addListener(IDLE_RIGHT_START, this::animateIdleR);
    entity.getEvents().addListener(WALK_LEFT_START, this::animateWalkL);
    entity.getEvents().addListener(WALK_RIGHT_START, this::animateWalkR);

    // Attack listeners
    entity.getEvents().addListener(ATTACK_LEFT_START, this::animateAttackL);
    entity.getEvents().addListener(ATTACK_RIGHT_START, this::animateAttackR);
    entity.getEvents().addListener(MELEE_ATTACK, this::onMeleeAttack);
    entity.getEvents().addListener(MELEE_ATTACK_WINDUP, this::onAttack);
    entity.getEvents().addListener(BITE, this::onAttack);

    // Death listeners
    entity.getEvents().addListener(DEATH_LEFT_START, this::animateDeathL);
    entity.getEvents().addListener(DEATH_RIGHT_START, this::animateDeathR);
    entity.getEvents().addListener(DEATH, this::onDeath);

    // Trigger a default starting state
    entity.getEvents().trigger(IDLE_RIGHT_START);
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (isDead) {
      return;
    }
    if (isAttacking) {
      if (animator != null && animator.isFinished()) {
        isAttacking = false;
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
    } else {
      if (!isAttacking
          && (currentAnimState == AnimationState.ATTACK_LEFT
              || currentAnimState == AnimationState.ATTACK_RIGHT)) {
        currentAnimState = isFacingLeft() ? AnimationState.IDLE_LEFT : AnimationState.IDLE_RIGHT;
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
      return isFacingLeft() ? AnimationState.IDLE_LEFT : AnimationState.IDLE_RIGHT;
    }
  }

  private void triggerStateEvent(AnimationState state) {
    if (state == null) {
      return;
    }
    switch (state) {
      case IDLE_LEFT:
        entity.getEvents().trigger(IDLE_LEFT_START);
        break;
      case IDLE_RIGHT:
        entity.getEvents().trigger(IDLE_RIGHT_START);
        break;
      case WALK_LEFT:
        entity.getEvents().trigger(WALK_LEFT_START);
        break;
      case WALK_RIGHT:
        entity.getEvents().trigger(WALK_RIGHT_START);
        break;
      case ATTACK_LEFT:
        entity.getEvents().trigger(ATTACK_LEFT_START);
        break;
      case ATTACK_RIGHT:
        entity.getEvents().trigger(ATTACK_RIGHT_START);
        break;
      case DEATH_LEFT:
        entity.getEvents().trigger(DEATH_LEFT_START);
        break;
      case DEATH_RIGHT:
        entity.getEvents().trigger(DEATH_RIGHT_START);
        break;
    }
  }

  private void onAttack(Entity target) {
    if (isDead || isAttacking) {
      return;
    }
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.ATTACK_LEFT
              : AnimationState.ATTACK_RIGHT;
    } else {
      currentAnimState = isFacingLeft() ? AnimationState.ATTACK_LEFT : AnimationState.ATTACK_RIGHT;
    }
    isAttacking = true;
    triggerStateEvent(currentAnimState);
  }

  private void onMeleeAttack(Entity target) {
    if (entity != null && entity.getComponent(MeleeAttackComponent.class) != null) {
      return;
    }
    onAttack(target);
  }

  private void onDeath() {
    isDead = true;
    isAttacking = false;
    currentAnimState = isFacingLeft() ? AnimationState.DEATH_LEFT : AnimationState.DEATH_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  void animateIdleL() {
    currentAnimState = AnimationState.IDLE_LEFT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_idle_l");
    }
  }

  void animateIdleR() {
    currentAnimState = AnimationState.IDLE_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_idle_r");
    }
  }

  void animateWalkL() {
    currentAnimState = AnimationState.WALK_LEFT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_walk_l");
    }
  }

  void animateWalkR() {
    currentAnimState = AnimationState.WALK_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_walk_r");
    }
  }

  void animateAttackL() {
    currentAnimState = AnimationState.ATTACK_LEFT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("cerberus_attack_l");
    }
  }

  void animateAttackR() {
    currentAnimState = AnimationState.ATTACK_RIGHT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("cerberus_attack_r");
    }
  }

  void animateDeathL() {
    currentAnimState = AnimationState.DEATH_LEFT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_death_l");
    }
  }

  void animateDeathR() {
    currentAnimState = AnimationState.DEATH_RIGHT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("cerberus_death_r");
    }
  }

  public boolean isFacingLeft() {
    return currentAnimState == AnimationState.IDLE_LEFT
        || currentAnimState == AnimationState.WALK_LEFT
        || currentAnimState == AnimationState.ATTACK_LEFT
        || currentAnimState == AnimationState.DEATH_LEFT;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.IDLE_RIGHT
        || currentAnimState == AnimationState.WALK_RIGHT
        || currentAnimState == AnimationState.ATTACK_RIGHT
        || currentAnimState == AnimationState.DEATH_RIGHT;
  }

  public boolean isAttacking() {
    return isAttacking;
  }

  public boolean isDead() {
    return isDead;
  }

  public AnimationState getCurrentState() {
    return currentAnimState;
  }
}
