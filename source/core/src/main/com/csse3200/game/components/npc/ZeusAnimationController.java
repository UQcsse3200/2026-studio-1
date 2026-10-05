package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a Zeus entity's state and plays the corresponding
 * animations (idle, walk, physical strike, slam/lightning cast, death) from the zeus atlas. It uses
 * the slam animation for the ranged lightning cast and the strike animation for melee attacks.
 */
public class ZeusAnimationController extends Component {
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
    STRIKE_LEFT,
    STRIKE_RIGHT,
    SLAM_LEFT,
    SLAM_RIGHT,
    DEATH_LEFT,
    DEATH_RIGHT
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

    // Physical / sword melee strike listeners
    entity.getEvents().addListener("strikeLeftStart", this::animateStrikeL);
    entity.getEvents().addListener("strikeRightStart", this::animateStrikeR);
    entity.getEvents().addListener("meleeLeftStart", this::animateStrikeL);
    entity.getEvents().addListener("meleeRightStart", this::animateStrikeR);
    entity.getEvents().addListener("meleeAttack", (Entity target) -> onMeleeAttack(target));
    entity.getEvents().addListener("meleeAttackWindup", (Entity target) -> onMeleeAttack(target));
    entity.getEvents().addListener("strike", (Entity target) -> onMeleeAttack(target));

    // Slam / lightning cast (ranged attack) listeners
    entity.getEvents().addListener("slamLeftStart", this::animateSlamL);
    entity.getEvents().addListener("slamRightStart", this::animateSlamR);
    entity.getEvents().addListener("lightningLeftStart", this::animateSlamL);
    entity.getEvents().addListener("lightningRightStart", this::animateSlamR);
    entity.getEvents().addListener("rangedLeftStart", this::animateSlamL);
    entity.getEvents().addListener("rangedRightStart", this::animateSlamR);
    entity.getEvents().addListener("rangedAttackWindup", (Entity target) -> onRangedAttack(target));
    entity.getEvents().addListener("rangedAttackFired", (Entity target) -> onRangedAttack(target));
    entity.getEvents().addListener("rangedAttackStart", () -> onRangedAttack(null));
    entity
        .getEvents()
        .addListener(
            "rangedAttack", (Entity target, ProjectileType type) -> onRangedAttack(target));
    entity.getEvents().addListener("lightning", (Entity target) -> onRangedAttack(target));
    entity.getEvents().addListener("slam", (Entity target) -> onRangedAttack(target));

    // Death listeners
    entity.getEvents().addListener("deathLeftStart", this::animateDeathL);
    entity.getEvents().addListener("deathRightStart", this::animateDeathR);
    entity.getEvents().addListener("death", this::onDeath);

    // Trigger a default starting state
    entity.getEvents().trigger("idleRightStart");
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
          && (currentAnimState == AnimationState.STRIKE_LEFT
              || currentAnimState == AnimationState.STRIKE_RIGHT
              || currentAnimState == AnimationState.SLAM_LEFT
              || currentAnimState == AnimationState.SLAM_RIGHT)) {
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
        entity.getEvents().trigger("idleLeftStart");
        break;
      case IDLE_RIGHT:
        entity.getEvents().trigger("idleRightStart");
        break;
      case WALK_LEFT:
        entity.getEvents().trigger("walkLeftStart");
        break;
      case WALK_RIGHT:
        entity.getEvents().trigger("walkRightStart");
        break;
      case STRIKE_LEFT:
        entity.getEvents().trigger("strikeLeftStart");
        break;
      case STRIKE_RIGHT:
        entity.getEvents().trigger("strikeRightStart");
        break;
      case SLAM_LEFT:
        entity.getEvents().trigger("slamLeftStart");
        break;
      case SLAM_RIGHT:
        entity.getEvents().trigger("slamRightStart");
        break;
      case DEATH_LEFT:
        entity.getEvents().trigger("deathLeftStart");
        break;
      case DEATH_RIGHT:
        entity.getEvents().trigger("deathRightStart");
        break;
    }
  }

  private void onMeleeAttack(Entity target) {
    if (isDead) {
      return;
    }
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.STRIKE_LEFT
              : AnimationState.STRIKE_RIGHT;
    } else {
      currentAnimState = isFacingLeft() ? AnimationState.STRIKE_LEFT : AnimationState.STRIKE_RIGHT;
    }
    isAttacking = true;
    triggerStateEvent(currentAnimState);
  }

  private void onRangedAttack(Entity target) {
    if (isDead) {
      return;
    }
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.SLAM_LEFT
              : AnimationState.SLAM_RIGHT;
    } else {
      currentAnimState = isFacingLeft() ? AnimationState.SLAM_LEFT : AnimationState.SLAM_RIGHT;
    }
    isAttacking = true;
    triggerStateEvent(currentAnimState);
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
      animator.startAnimation("zeus_idle_l");
    }
  }

  void animateIdleR() {
    currentAnimState = AnimationState.IDLE_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("zeus_idle_r");
    }
  }

  void animateWalkL() {
    currentAnimState = AnimationState.WALK_LEFT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("zeus_walk_l");
    }
  }

  void animateWalkR() {
    currentAnimState = AnimationState.WALK_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("zeus_walk_r");
    }
  }

  void animateStrikeL() {
    currentAnimState = AnimationState.STRIKE_LEFT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("zeus_p_strike_l");
    }
  }

  void animateStrikeR() {
    currentAnimState = AnimationState.STRIKE_RIGHT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("zeus_p_strike_r");
    }
  }

  void animateSlamL() {
    currentAnimState = AnimationState.SLAM_LEFT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("zeus_slam_l");
    }
  }

  void animateSlamR() {
    currentAnimState = AnimationState.SLAM_RIGHT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("zeus_slam_r");
    }
  }

  void animateDeathL() {
    currentAnimState = AnimationState.DEATH_LEFT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("zeus_death_l");
    }
  }

  void animateDeathR() {
    currentAnimState = AnimationState.DEATH_RIGHT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("zeus_death_r");
    }
  }

  public boolean isFacingLeft() {
    return currentAnimState == AnimationState.IDLE_LEFT
        || currentAnimState == AnimationState.WALK_LEFT
        || currentAnimState == AnimationState.STRIKE_LEFT
        || currentAnimState == AnimationState.SLAM_LEFT
        || currentAnimState == AnimationState.DEATH_LEFT;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.IDLE_RIGHT
        || currentAnimState == AnimationState.WALK_RIGHT
        || currentAnimState == AnimationState.STRIKE_RIGHT
        || currentAnimState == AnimationState.SLAM_RIGHT
        || currentAnimState == AnimationState.DEATH_RIGHT;
  }

  public AnimationState getCurrentState() {
    return currentAnimState;
  }

  public boolean isAttacking() {
    return isAttacking;
  }

  public boolean isDead() {
    return isDead;
  }
}
