package com.csse3200.game.components.enemies.animation;

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
  private static final String IDLE_R = "cyclops_idle_r";
  private static final String IDLE_L = "cyclops_idle_l";
  private static final String WALK_L = "cyclops_walk_l";
  private static final String WALK_R = "cyclops_walk_r";
  private static final String ROCK_L = "cyclops_rock_l";
  private static final String ROCK_R = "cyclops_rock_r";
  private static final String LASER_L = "cyclops_laser_l";
  private static final String LASER_R = "cyclops_laser_r";
  private static final String TAUNT_L = "cyclops_taunt_l";
  private static final String TAUNT_R = "cyclops_taunt_r";
  private static final String STOMP_L = "cyclops_stomp_l";
  private static final String STOMP_R = "cyclops_stomp_r";
  private static final String DEATH_L = "cyclops_death_l";
  private static final String DEATH_R = "cyclops_death_r";
  private static final String IDLE_RIGHT_START_EVENT = "idleRightStart";

  private AnimationRenderComponent animator;
  private EnemyWeaponAnimationComponent weaponAnimator;
  private PhysicsComponent physicsComponent;
  private AnimationState currentAnimState = null;
  private boolean isAttacking = false;
  private boolean isDead = false;

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
    STOMP_RIGHT,
    DEATH_LEFT,
    DEATH_RIGHT
  }

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(AnimationRenderComponent.class);
    weaponAnimator = this.entity.getComponent(EnemyWeaponAnimationComponent.class);
    physicsComponent = this.entity.getComponent(PhysicsComponent.class);

    // Directional movement & idle listeners
    entity.getEvents().addListener("idleLeftStart", this::animateIdleL);
    entity.getEvents().addListener(IDLE_RIGHT_START_EVENT, this::animateIdleR);
    entity.getEvents().addListener("walkLeftStart", this::animateWalkL);
    entity.getEvents().addListener("walkRightStart", this::animateWalkR);

    // Rock throwing listeners
    entity.getEvents().addListener("rockThrowLeftStart", this::animateRockThrowL);
    entity.getEvents().addListener("rockThrowRightStart", this::animateRockThrowR);
    entity.getEvents().addListener("rockAttackWindup", this::onRockThrow);

    // Laser attack listeners
    entity.getEvents().addListener("laserLeftStart", this::animateLaserL);
    entity.getEvents().addListener("laserRightStart", this::animateLaserR);
    entity.getEvents().addListener("laserAttackWindup", this::onLaserAttack);

    // Melee stomp listeners
    entity.getEvents().addListener("stompLeftStart", this::animateStompL);
    entity.getEvents().addListener("stompRightStart", this::animateStompR);
    entity.getEvents().addListener("meleeAttack", this::onMeleeAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onMeleeAttack);

    // Ranged attack listeners
    entity.getEvents().addListener("rangedAttackWindup", this::onRangedAttack);
    entity.getEvents().addListener("rangedAttackFired", this::onRangedAttack);
    entity.getEvents().addListener("rangedAttack", this::onRangedAttack);
    entity.getEvents().addListener("rangedAttackStart", this::onRockStart);

    // Death listeners
    entity.getEvents().addListener("deathLeftStart", this::animateDeathL);
    entity.getEvents().addListener("deathRightStart", this::animateDeathR);
    entity.getEvents().addListener("death", this::onDeath);

    // Trigger a default starting state
    entity.getEvents().trigger(IDLE_RIGHT_START_EVENT);
    currentAnimState = AnimationState.IDLE_RIGHT;
  }

  /** Returns true while an attack is still playing, so update() can skip movement. */
  private boolean attackingHandler() {
    if (!isAttacking) {
      return false;
    }
    if (!hasAttackFinished()) {
      return true;
    }
    finishAttack();
    return false;
  }

  @Override
  public void update() {
    if (isDead) {
      return;
    }

    if (weaponAnimator == null && entity != null) {
      weaponAnimator = entity.getComponent(EnemyWeaponAnimationComponent.class);
    }

    if (attackingHandler()) {
      return;
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
        || state == AnimationState.STOMP_LEFT
        || state == AnimationState.DEATH_LEFT;
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
        entity.getEvents().trigger(IDLE_RIGHT_START_EVENT);
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
      case DEATH_LEFT:
        entity.getEvents().trigger("deathLeftStart");
        break;
      case DEATH_RIGHT:
        entity.getEvents().trigger("deathRightStart");
        break;
    }
  }

  private void onRockThrow(Entity target) {
    if (isDead || isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.ROCK_LEFT : AnimationState.ROCK_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onLaserAttack(Entity target) {
    if (isDead || isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.LASER_LEFT : AnimationState.LASER_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onRockStart() {
    onRockThrow(null);
  }

  private void onMeleeAttack(Entity target) {
    if (isDead || isAttacking) {
      return;
    }
    currentAnimState =
        isTargetToLeft(target) ? AnimationState.STOMP_LEFT : AnimationState.STOMP_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  private void onRangedAttack(Entity target, ProjectileType projectile) {
    if (projectile == ProjectileType.LASER) {
      onLaserAttack(target);
    } else {
      onRockThrow(target);
    }
  }

  void animateIdleL() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_LEFT;
    if (animator != null) {
      animator.startAnimation(IDLE_L);
    }
  }

  void animateIdleR() {
    isAttacking = false;
    currentAnimState = AnimationState.IDLE_RIGHT;
    if (animator != null) {
      animator.startAnimation(IDLE_R);
    }
  }

  void animateWalkL() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_LEFT;
    if (animator != null) {
      animator.startAnimation(WALK_L);
    }
  }

  void animateWalkR() {
    isAttacking = false;
    currentAnimState = AnimationState.WALK_RIGHT;
    if (animator != null) {
      animator.startAnimation(WALK_R);
    }
  }

  void animateRockThrowL() {
    isAttacking = true;
    currentAnimState = AnimationState.ROCK_LEFT;
    if (animator != null) {
      animator.startAnimation(ROCK_L);
    }
  }

  void animateRockThrowR() {
    isAttacking = true;
    currentAnimState = AnimationState.ROCK_RIGHT;
    if (animator != null) {
      animator.startAnimation(ROCK_R);
    }
  }

  void animateLaserL() {
    isAttacking = true;
    currentAnimState = AnimationState.LASER_LEFT;
    if (weaponAnimator != null) {
      if (weaponAnimator.hasAnimation(LASER_L)) {
        weaponAnimator.startAnimation(LASER_L);
      } else if (weaponAnimator.hasAnimation("laser_l")) {
        weaponAnimator.startAnimation("laser_l");
      }
    }
    if (animator != null) {
      if (animator.hasAnimation(TAUNT_L)) {
        animator.startAnimation(TAUNT_L);
      } else if (animator.hasAnimation(IDLE_L)) {
        animator.startAnimation(IDLE_L);
      }
    }
  }

  void animateLaserR() {
    isAttacking = true;
    currentAnimState = AnimationState.LASER_RIGHT;
    if (weaponAnimator != null) {
      if (weaponAnimator.hasAnimation(LASER_R)) {
        weaponAnimator.startAnimation(LASER_R);
      } else if (weaponAnimator.hasAnimation("laser_r")) {
        weaponAnimator.startAnimation("laser_r");
      }
    }
    if (animator != null) {
      if (animator.hasAnimation(TAUNT_R)) {
        animator.startAnimation(TAUNT_R);
      } else if (animator.hasAnimation(IDLE_R)) {
        animator.startAnimation(IDLE_R);
      }
    }
  }

  void animateStompL() {
    isAttacking = true;
    currentAnimState = AnimationState.STOMP_LEFT;
    if (animator != null) {
      animator.startAnimation(STOMP_L);
    }
  }

  void animateStompR() {
    isAttacking = true;
    currentAnimState = AnimationState.STOMP_RIGHT;
    if (animator != null) {
      animator.startAnimation(STOMP_R);
    }
  }

  private void onDeath() {
    isDead = true;
    isAttacking = false;
    if (weaponAnimator != null && weaponAnimator.getCurrentAnimation() != null) {
      weaponAnimator.stopAnimation();
    }
    currentAnimState =
        isFacingLeft(currentAnimState) ? AnimationState.DEATH_LEFT : AnimationState.DEATH_RIGHT;
    triggerStateEvent(currentAnimState);
  }

  void animateDeathL() {
    isDead = true;
    isAttacking = false;
    currentAnimState = AnimationState.DEATH_LEFT;
    if (animator != null) {
      animator.startAnimation(DEATH_L);
    }
  }

  void animateDeathR() {
    isDead = true;
    isAttacking = false;
    currentAnimState = AnimationState.DEATH_RIGHT;
    if (animator != null) {
      animator.startAnimation(DEATH_R);
    }
  }

  public boolean isDead() {
    return isDead;
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

  /** Returns true when the animation that matters for the current attack has finished. */
  private boolean hasAttackFinished() {
    if (isLaserState(currentAnimState) && hasActiveWeaponAnimation()) {
      return weaponAnimator.isFinished();
    }
    return animator == null || animator.isFinished();
  }

  /** Returns true for the two laser states. */
  private boolean isLaserState(AnimationState state) {
    return state == AnimationState.LASER_LEFT || state == AnimationState.LASER_RIGHT;
  }

  /** Returns true when the weapon animator has an animation loaded. */
  private boolean hasActiveWeaponAnimation() {
    return weaponAnimator != null && weaponAnimator.getCurrentAnimation() != null;
  }

  /** Returns true when the entity has a physics component with a body. */
  private boolean hasPhysicsBody() {
    return physicsComponent != null && physicsComponent.getBody() != null;
  }

  /** Clears the attack flag, stops the weapon animation and returns to idle if there is no body. */
  private void finishAttack() {
    isAttacking = false;
    if (hasActiveWeaponAnimation()) {
      weaponAnimator.stopAnimation();
    }
    if (!hasPhysicsBody()) {
      currentAnimState =
          isFacingLeft(currentAnimState) ? AnimationState.IDLE_LEFT : AnimationState.IDLE_RIGHT;
      triggerStateEvent(currentAnimState);
    }
  }
}
