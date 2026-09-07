package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/**
 * This class listens to events relevant to a player entity's state and plays the animation when one
 * of the events is triggered.
 */
public class PlayerAnimationController extends Component {
    AnimationRenderComponent animator;

    @Override
    public void create() {
        super.create();
        animator = this.entity.getComponent(AnimationRenderComponent.class);
        entity.getEvents().addListener("idle", this::animateIdle);
        entity.getEvents().addListener("run", this::animateRun);
        entity.getEvents().addListener("attack", this::animateAttack);
        entity.getEvents().addListener("slide", this::animateSlide);
        entity.getEvents().addListener("crouch", this::animateCrouch);
        entity.getEvents().addListener("jump", this::animateJump);
        entity.getEvents().addListener("roll", this::animateRoll);
    }

    void animateIdle() {
        animator.startAnimation("Idle");
    }

    void animateRun() {
        animator.startAnimation("Run");
    }

    void animateAttack() {
        animator.startAnimation("Attacks");
    }

    void animateSlide(boolean sliding) {
        if (sliding) {
            animator.startAnimation("Slide");
        }
    }

    void animateCrouch() {
        animator.startAnimation("crouchidle");
    }

    void animateJump(Vector2 direction) {
        animator.startAnimation("Jump");
    }

    void animateRoll() {
        animator.startAnimation("Roll");
    }
}