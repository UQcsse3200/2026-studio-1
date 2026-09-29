package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;

/**
 * Controller for enemy weapon animations on skeleton NPCs. Synchronizes the weapon's facing
 * direction and attack animations with the skeleton's state.
 */
public class SkeletonWeaponAnimationController extends Component {
  private EnemyWeaponAnimationComponent animator;
  private boolean facingRight = true;
  private boolean isAttacking = false;

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(EnemyWeaponAnimationComponent.class);

    // Listen to directional movement events triggered by SkeletonAnimationController
    entity.getEvents().addListener("walkLeftStart", this::onFaceLeft);
    entity.getEvents().addListener("idleLeftStart", this::onFaceLeft);
    entity.getEvents().addListener("walkRightStart", this::onFaceRight);
    entity.getEvents().addListener("idleRightStart", this::onFaceRight);

    // Listen to attack events
    entity.getEvents().addListener("meleeAttack", (Entity target) -> onMeleeAttack());
    entity.getEvents().addListener("rangedAttackStart", this::onRangedAttack);
    entity.getEvents().addListener("rangedAttack", (Entity target) -> onRangedAttack());

    // Listen to explicit direct start events if triggered elsewhere
    entity.getEvents().addListener("SwordLeftStart", this::animateSwordL);
    entity.getEvents().addListener("SwordRightStart", this::animateSwordR);
    entity.getEvents().addListener("BowLeftStart", this::animateBowL);
    entity.getEvents().addListener("BowRightStart", this::animateBowR);

    // Default resting stance
    updateRestingStance();
  }

  @Override
  public void update() {
    if (animator == null) {
      return;
    }

    if (isAttacking && animator.isFinished()) {
      isAttacking = false;
      updateRestingStance();
    }
  }

  private void onFaceLeft() {
    facingRight = false;
    if (!isAttacking) {
      updateRestingStance();
    }
  }

  private void onFaceRight() {
    facingRight = true;
    if (!isAttacking) {
      updateRestingStance();
    }
  }

  private void onMeleeAttack() {
    if (facingRight) {
      animateSwordR();
    } else {
      animateSwordL();
    }
  }

  private void onRangedAttack() {
    if (facingRight) {
      animateBowR();
    } else {
      animateBowL();
    }
  }

  public void animateSwordL() {
    if (animator != null && animator.hasAnimation("sword_l")) {
      isAttacking = true;
      animator.startAnimation("sword_l");
    }
  }

  public void animateSwordR() {
    if (animator != null && animator.hasAnimation("sword_r")) {
      isAttacking = true;
      animator.startAnimation("sword_r");
    }
  }

  public void animateBowL() {
    if (animator != null && animator.hasAnimation("bow_l")) {
      isAttacking = true;
      animator.startAnimation("bow_l");
    }
  }

  public void animateBowR() {
    if (animator != null && animator.hasAnimation("bow_r")) {
      isAttacking = true;
      animator.startAnimation("bow_r");
    }
  }

  private void updateRestingStance() {
    if (animator == null) {
      return;
    }

    String restingAnim = facingRight ? "default_r" : "default_l";
    if (animator.hasAnimation(restingAnim)) {
      animator.startAnimation(restingAnim);
    }
  }

  public boolean isFacingRight() {
    return facingRight;
  }

  public boolean isAttacking() {
    return isAttacking;
  }
}
