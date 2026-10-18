package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.PlayerRenderComponent;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PlayerAnimationControllerTest {
    PlayerAnimationController animator = new PlayerAnimationController();
    PlayerRenderComponent renderer = mock(PlayerRenderComponent.class);
    Entity player = new Entity().addComponent(animator).addComponent(renderer);
    @Test
    void animateIdleTest(){
        animator.create();
        animator.animateIdle("Right");
        verify(renderer).startAnimation("Idle");
    }
    @Test
    void animateRunTest(){
        animator.create();
        animator.animateRun("Right");
        verify(renderer).startAnimation("Run");
    }
    @Test
    void animateAttackTest(){
        animator.create();
        animator.animateAttack("Right");
        verify(renderer).startAnimation("Attacks");
    }
    @Test
    void animateSlideTest(){
        animator.create();
        animator.animateSlide("Right");
        verify(renderer).startAnimation("Slide");
    }
    @Test
    void animateCrouchTest(){
        animator.create();
        animator.animateCrouch("Right");
        verify(renderer).startAnimation("crouchidle");
    }
    @Test
    void animateJumpTest(){
        animator.create();
        animator.animateJump("Right");
        verify(renderer).startAnimation("Jump");
    }
    @Test
    void animateRollTest(){
        animator.create();
        animator.animateRoll("Right");
        verify(renderer).startAnimation("Roll");
    }
    @Test
    void animateDeathTest(){
        animator.create();
        animator.animateDeath("Right");
        verify(renderer).startAnimation("death");
    }
    @Test
    void animateHealTest(){
        animator.create();
        animator.animateHeal("Right");
        verify(renderer).startAnimation("health");
    }
    @Test
    void animateHurtTest(){
        animator.create();
        animator.animateHurt("Right");
        verify(renderer).startAnimation("hurt");
    }
    @Test
    void animateClimbTest(){
        animator.create();
        animator.animateClimb();
        verify(renderer).startAnimation("climb");
    }
    @Test
    void facingRightTrueTest(){
        assertTrue(animator.facingRight("Right"));
    }

    @Test
    void facingRightFalseTest(){
        assertFalse(animator.facingRight("Left"));
    }

    @Test
    void canAnimateTrueTest(){
        animator.create();
        animator.animateIdle("Right");
        assertTrue(animator.canAnimate());
    }

    @Test
    void canAnimateFalseTest(){
        animator.create();
        animator.animateHurt("Right");
        assertFalse(animator.canAnimate());
    }
}
