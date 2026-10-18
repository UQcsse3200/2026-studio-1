package com.csse3200.game.components.enemies.animation;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.components.enemies.attacks.RangedAttackComponent;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to Medusa's state and plays the animation when one of the
 * events is triggered. It also updates the animation state based on velocity and attacks (melee
 * bite and ranged gaze), driven by the real {@code gorgon} atlas, which only has idle, walk, attack
 * and dead animations (no separate rock/laser/stomp states like the Cyclops). This class listens to
 * events relevant to a Medusa entity's state and plays the corresponding animations (idle, walk,
 * attack, death) from the gorgon atlas. It updates facing direction and animation state based on
 * velocity, melee bite attacks, and ranged gaze attacks.
 */
public class MedusaAnimationController extends Component {
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
    PETRIFY_LEFT,
    PETRIFY_RIGHT,
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

    // Melee attack listeners (bite)
    entity.getEvents().addListener("attackLeftStart", this::animateAttackL);
    entity.getEvents().addListener("attackRightStart", this::animateAttackR);
    entity.getEvents().addListener("meleeAttack", this::onMeleeAttack);
    entity.getEvents().addListener("meleeAttackWindup", this::onAttack);
    entity.getEvents().addListener("bite", this::onAttack);

    // Petrify / ranged gaze attack listeners
    entity.getEvents().addListener("petrifyLeftStart", this::animatePetrifyL);
    entity.getEvents().addListener("petrifyRightStart", this::animatePetrifyR);
    entity.getEvents().addListener("petrify", this::onPetrify);
    entity.getEvents().addListener("petrifyAttack", this::onPetrify);
    entity.getEvents().addListener("gaze", this::onPetrify);
    entity.getEvents().addListener("rangedAttackWindup", this::onPetrify);
    entity.getEvents().addListener("rangedAttackFired", this::onRangedFired);
    entity.getEvents().addListener("rangedAttackStart", () -> onRangedAttack(null));
    entity
        .getEvents()
        .addListener(
            "rangedAttack", (Entity target, ProjectileType type) -> onRangedAttack(target));

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
          && (currentAnimState == AnimationState.ATTACK_LEFT
              || currentAnimState == AnimationState.ATTACK_RIGHT
              || currentAnimState == AnimationState.PETRIFY_LEFT
              || currentAnimState == AnimationState.PETRIFY_RIGHT)) {
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
      case ATTACK_LEFT:
        entity.getEvents().trigger("attackLeftStart");
        break;
      case ATTACK_RIGHT:
        entity.getEvents().trigger("attackRightStart");
        break;
      case PETRIFY_LEFT:
        entity.getEvents().trigger("petrifyLeftStart");
        break;
      case PETRIFY_RIGHT:
        entity.getEvents().trigger("petrifyRightStart");
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
    if (entity != null && entity.getComponent(MeleeAttackComponent.class) != null) {
      return;
    }
    onAttack(target);
  }

  private void onRangedAttack(Entity target) {
    if (entity != null && entity.getComponent(RangedAttackComponent.class) != null) {
      return;
    }
    onPetrify(target);
  }

  private void onRangedFired(Entity target) {
    if (entity != null && entity.getComponent(RangedAttackComponent.class) != null) {
      return;
    }
    onPetrify(target);
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

  private void onPetrify(Entity target) {
    if (isDead || isAttacking) {
      return;
    }
    if (target != null
        && target.getPosition() != null
        && entity != null
        && entity.getPosition() != null) {
      currentAnimState =
          (target.getPosition().x < entity.getPosition().x)
              ? AnimationState.PETRIFY_LEFT
              : AnimationState.PETRIFY_RIGHT;
    } else {
      currentAnimState =
          isFacingLeft() ? AnimationState.PETRIFY_LEFT : AnimationState.PETRIFY_RIGHT;
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
      animator.startAnimation("gorgon_idle_l");
    }
  }

  void animateIdleR() {
    currentAnimState = AnimationState.IDLE_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("gorgon_idle_r");
    }
  }

  void animateWalkL() {
    currentAnimState = AnimationState.WALK_LEFT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("gorgon_walk_l");
    }
  }

  void animateWalkR() {
    currentAnimState = AnimationState.WALK_RIGHT;
    isAttacking = false;
    if (animator != null) {
      animator.startAnimation("gorgon_walk_r");
    }
  }

  void animateAttackL() {
    currentAnimState = AnimationState.ATTACK_LEFT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("gorgon_attack_l");
    }
  }

  void animateAttackR() {
    currentAnimState = AnimationState.ATTACK_RIGHT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("gorgon_attack_r");
    }
  }

  void animatePetrifyL() {
    currentAnimState = AnimationState.PETRIFY_LEFT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("gorgon_petrify_l");
    }
  }

  void animatePetrifyR() {
    currentAnimState = AnimationState.PETRIFY_RIGHT;
    isAttacking = true;
    if (animator != null) {
      animator.startAnimation("gorgon_petrify_r");
    }
  }

  void animateDeathL() {
    currentAnimState = AnimationState.DEATH_LEFT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      if (animator.hasAnimation("gorgon_dead_l")) {
        animator.startAnimation("gorgon_dead_l");
      } else {
        animator.startAnimation("gorgon_death_l");
      }
    }
  }

  void animateDeathR() {
    currentAnimState = AnimationState.DEATH_RIGHT;
    isDead = true;
    isAttacking = false;
    if (animator != null) {
      if (animator.hasAnimation("gorgon_dead_r")) {
        animator.startAnimation("gorgon_dead_r");
      } else {
        animator.startAnimation("gorgon_death_r");
      }
    }
  }

  public boolean isFacingLeft() {
    return currentAnimState == AnimationState.IDLE_LEFT
        || currentAnimState == AnimationState.WALK_LEFT
        || currentAnimState == AnimationState.ATTACK_LEFT
        || currentAnimState == AnimationState.PETRIFY_LEFT
        || currentAnimState == AnimationState.DEATH_LEFT;
  }

  public boolean isFacingRight() {
    return currentAnimState == AnimationState.IDLE_RIGHT
        || currentAnimState == AnimationState.WALK_RIGHT
        || currentAnimState == AnimationState.ATTACK_RIGHT
        || currentAnimState == AnimationState.PETRIFY_RIGHT
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
