package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.PlayerRenderComponent;

/**
 * This class listens to events relevant to a player entity's state and plays the animation when one
 * of the events is triggered.
 */
public class PlayerAnimationController extends Component {
  PlayerRenderComponent animator;

  @Override
  public void create() {
    super.create();
    animator = this.entity.getComponent(PlayerRenderComponent.class);

    entity.getEvents().addListener("idle", this::animateIdle);
    entity.getEvents().addListener("run", this::animateRun);
    entity.getEvents().addListener("attacking", this::animateAttack);
    entity.getEvents().addListener("sliding", this::animateSlide);
    entity.getEvents().addListener("crouchidle", this::animateCrouch);
    entity.getEvents().addListener("jumping", this::animateJump);
    entity.getEvents().addListener("rolling", this::animateRoll);
    entity.getEvents().addListener("dead", this::animateDeath);
    entity.getEvents().addListener("heal", this::animateHeal);
    entity.getEvents().addListener("climb", this::animateClimb);
    entity.getEvents().addListener("hurt", this::animateHurt);
  }

  boolean facingRight(String direction) {
    return "Right".equals(direction);
  }
  boolean hurtPlaying = false;
  boolean deadPlaying = false;

  boolean canAnimate() {
    return !hurtPlaying && !deadPlaying;
  }

  void animateIdle(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Idle");
    } else {
      animator.startAnimation("LeftIdle");
    }
  }

  void animateRun(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Run");
    } else {
      animator.startAnimation("LeftRun");
    }
  }

  void animateAttack(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Attacks");
    } else {
      animator.startAnimation("LeftAttacks");
    }
  }

  void animateSlide(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Slide");
    } else {
      animator.startAnimation("LeftSlide");
    }
  }

  void animateCrouch(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("crouchidle");
    } else {
      animator.startAnimation("Leftcrouchidle");
    }
  }

  void animateJump(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Jump");
    } else {
      animator.startAnimation("LeftJump");
    }
  }

  void animateRoll(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("Roll");
    } else {
      animator.startAnimation("LeftRoll");
    }
  }

  void animateDeath(String direction) {
    deadPlaying = true;
    if (facingRight(direction)) {
      animator.startAnimation("death");
    } else {
      animator.startAnimation("leftdeath");
    }
  }

  void animateHeal(String direction) {
    if (!canAnimate()) {
      return;
    }
    if (facingRight(direction)) {
      animator.startAnimation("health");
    } else {
      animator.startAnimation("lefthealth");
    }
  }

  void animateHurt(String direction) {
    hurtPlaying = true;
    if (facingRight(direction)) {
      animator.startAnimation("hurt");
    } else {
      animator.startAnimation("lefthurt");
    }
  }

  void animateClimb() {
    animator.startAnimation("climb");
  }
}
