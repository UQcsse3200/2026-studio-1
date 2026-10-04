package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;

/**
 * This class listens to events relevant to a cyclops entity's state and plays the animation when
 * one of the events is triggered. It also updates the animation state based on velocity, melee
 * stomp attacks, rock throwing, and laser attacks (with laser overlay rendered via {@link
 * EnemyWeaponAnimationComponent}).
 */
public class CyclopsAnimationController extends Component {
  private AnimationRenderComponent animator;
  private EnemyWeaponAnimationComponent weaponAnimator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;
  private boolean isAttacking = false;

  private enum AnimationState {
    IDLE_LEFT,
    IDLE_RIGHT,
    WALK_LEFT,
    WALK_RIGHT,
    ROCK_LEFT,
    ROCK_RIGHT,
    LASER_LEFT,
    LASER_RIGHT,
    STOMP_LEFT,
    STOMP_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    weaponAnimator = this.entity.getComponent(EnemyWeaponAnimationComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    // Directional movement & idle listeners
    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener("idleRightStart", this::animateIdleR);
    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);

    // Rock throwing listeners
    entity.getEvents().addListener("rockThrowLeftStart", this::animateRockThrowL);
    entity.getEvents().addListener("rockThrowRightStart", this::animateRockThrowR);
    entity.getEvents().addListener("rockLeftStart", this::animateRockThrowL);
    entity.getEvents().addListener("rockRightStart", this::animateRockThrowR);
    entity.getEvents().addListener("rockThrow", this::onRockThrow);

    // Laser attack listeners
    entity.getEvents().addListener("laserLeftStart", this::animateLaserL);
    entity.getEvents().addListener("laserRightStart", this::animateLaserR);
    entity.getEvents().addListener("laserAttack", this::onLaserAttack);
    entity.getEvents().addListener("laserAttackStart", this::onLaserAttack);
    entity.getEvents().addListener("laserStart", this::onLaserStart);

    // Melee stomp listeners
    entity.getEvents().addListener("stompLeftStart", this::animateStompL);
    entity.getEvents().addListener("stompRightStart", this::animateStompR);
    entity.getEvents().addListener("stomp", this::onMeleeAttack);
    entity.getEvents().addListener("meleeAttack", this::onMeleeAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onMeleeAttack);

    // Ranged attack listeners
    entity.getEvents().addListener("rangedAttackWindup", this::onRockThrow);
    entity.getEvents().addListener("rangedAttackFired", this::onRockThrow);
    entity
        .getEvents()
        .addListener(
            "rangedAttack",
          this::onRangedAttack);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  @Override
  public void update() {
    if (weaponAnimator == null && entity != null) {
      weaponAnimator = entity.getComponent(EnemyWeaponAnimationComponent.class);
    }

    if (isAttacking) {
      boolean finished;
      if (currentAnimState == AnimationState.LASER_LEFT
          || currentAnimState == AnimationState.LASER_RIGHT) {
        if (weaponAnimator != null && weaponAnimator.getCurrentAnimation() != null) {
          finished = weaponAnimator.isFinished();
        } else {
          finished = (animator == null || animator.isFinished());
        }
      } else {
        finished = (animator == null || animator.isFinished());
      }

      if (finished) {
        isAttacking = false;
        if (weaponAnimator != null && weaponAnimator.getCurrentAnimation() != null) {
          weaponAnimator.stopAnimation();
        }
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
        || state == AnimationState.ROCK_LEFT
        || state == AnimationState.LASER_LEFT
        || state == AnimationState.STOMP_LEFT;
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
      case ROCK_LEFT:
        entity.getEvents().trigger("rockThrowLeftStart");
        break;
      case ROCK_RIGHT:
        entity.getEvents().trigger("rockThrowRightStart");
        break;
      case LASER_LEFT:
        entity.getEvents().trigger("laserLeftStart");
        break;
      case LASER_RIGHT:
        entity.getEvents().trigger("laserRightStart");
        break;
      case STOMP_LEFT:
        entity.getEvents().trigger("stompLeftStart");
        break;
      case STOMP_RIGHT:
        entity.getEvents().trigger("stompRightStart");
        break;
    }
  }

  private void onRockThrow(Entity target) {
    if (isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.ROCK_LEFT : AnimationState.ROCK_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onLaserAttack(Entity target) {
    if (isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.LASER_LEFT : AnimationState.LASER_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onLaserStart() {
    onLaserAttack(null);
  }

  private void onMeleeAttack(Entity target) {
    if (isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.STOMP_LEFT : AnimationState.STOMP_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onRangedAttack(Entity target, ProjectileType projectile) {
    if (projectile == ProjectileType.LIGHTNING) {
      onLaserAttack(target);
    } else {
      onRockThrow(target);
    }
  }

  void animateIdleL() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_LEFT;
    if (animator != null) {
      animator.startAnimation("cyclops_idle_l");
    }
  }

  void animateIdleR() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_RIGHT;
    if (animator != null) {
      animator.startAnimation("cyclops_idle_r");
    }
  }

  void animateWalkL() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_LEFT;
    if (animator != null) {
      animator.startAnimation("cyclops_walk_l");
    }
  }

  void animateWalkR() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_RIGHT;
    if (animator != null) {
      animator.startAnimation("cyclops_walk_r");
    }
  }

  void animateRockThrowL() {
    isAttacking = true;
    currentAnimState = AnimationState.ROCK_LEFT;
    if (animator != null) {
      animator.startAnimation("cyclops_rock_l");
    }
  }

  void animateRockThrowR() {
    isAttacking = true;
    currentAnimState = AnimationState.ROCK_RIGHT;
    if (animator != null) {
      animator.startAnimation("cyclops_rock_r");
    }
  }

  void animateLaserL() {
    isAttacking = true;
    currentAnimState = AnimationState.LASER_LEFT;
    if (weaponAnimator != null) {
      if (weaponAnimator.hasAnimation("cyclops_laser_l")) {
        weaponAnimator.startAnimation("cyclops_laser_l");
      } else if (weaponAnimator.hasAnimation("laser_l")) {
        weaponAnimator.startAnimation("laser_l");
      }
    }
    if (animator != null) {
      if (animator.hasAnimation("cyclops_taunt_l")) {
        animator.startAnimation("cyclops_taunt_l");
      } else if (animator.hasAnimation("cyclops_idle_l")) {
        animator.startAnimation("cyclops_idle_l");
      }
    }
  }

  void animateLaserR() {
    isAttacking = true;
    currentAnimState = AnimationState.LASER_RIGHT;
    if (weaponAnimator != null) {
      if (weaponAnimator.hasAnimation("cyclops_laser_r")) {
        weaponAnimator.startAnimation("cyclops_laser_r");
      } else if (weaponAnimator.hasAnimation("laser_r")) {
        weaponAnimator.startAnimation("laser_r");
      }
    }
    if (animator != null) {
      if (animator.hasAnimation("cyclops_taunt_r")) {
        animator.startAnimation("cyclops_taunt_r");
      } else if (animator.hasAnimation("cyclops_idle_r")) {
        animator.startAnimation("cyclops_idle_r");
      }
    }
  }

  void animateStompL() {
    isAttacking = true;
    currentAnimState = AnimationState.STOMP_LEFT;
    if (animator != null) {
      animator.startAnimation("cyclops_stomp_l");
    }
  }

  void animateStompR() {
    isAttacking = true;
    currentAnimState = AnimationState.STOMP_RIGHT;
    if (animator != null) {
      animator.startAnimation("cyclops_stomp_r");
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
