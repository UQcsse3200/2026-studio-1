package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;
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
    }

    void animateIdle(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Idle");
        } else {
            animator.startAnimation("LeftIdle");
        }
    }

    void animateRun(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Run");
        } else {
            animator.startAnimation("LeftRun");
        }
    }

    void animateAttack(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Attacks");
        } else {
            animator.startAnimation("LeftAttacks");
        }
    }

    void animateSlide(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Slide");
        } else {
            animator.startAnimation("LeftSlide");
        }
    }

    void animateCrouch(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("crouchidle");
        } else {
            animator.startAnimation("Leftcrouchidle");
        }
    }

    void animateJump(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Jump");
        } else {
            animator.startAnimation("LeftJump");
        }
    }

    void animateRoll(String direction) {
        if ("Right".equals(direction)) {
            animator.startAnimation("Roll");
        } else {
            animator.startAnimation("LeftRoll");
        }
    }
}