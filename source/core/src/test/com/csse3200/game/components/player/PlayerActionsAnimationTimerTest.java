package com.csse3200.game.components.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.PlayerRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlayerActionsAnimationTimerTest {
  private PlayerAnimationController animator;
  private PlayerRenderComponent renderer;
  private PlayerActions actions;
  private Entity player;

  @BeforeEach
  void setUp() {
    animator = new PlayerAnimationController();
    renderer = mock(PlayerRenderComponent.class);
    actions = new PlayerActions();
    player = new Entity().addComponent(renderer).addComponent(actions).addComponent(animator);
    player.create();
  }

  @Test
  void animationTimerShouldStartRun() {
    when(renderer.isFinished()).thenReturn(true);
    when(renderer.getCurrentAnimation()).thenReturn("Jump");
    actions.setWalkDirection(10, 0);
    actions.animationtimer("Right");
    verify(renderer).startAnimation("Run");
  }

  @Test
  void animationTimerShouldStartIdle() {
    when(renderer.isFinished()).thenReturn(true);
    when(renderer.getCurrentAnimation()).thenReturn("Jump");
    actions.setWalkDirection(0, 0);
    actions.animationtimer("Right");
    verify(renderer).startAnimation("Idle");
  }

  @Test
  void animationTimerShouldTurnHurtPlayingFalse() {
    animator.hurtPlaying = true;
    assertTrue(animator.hurtPlaying);
    when(renderer.isFinished()).thenReturn(true);
    when(renderer.getCurrentAnimation()).thenReturn("hurt");
    actions.animationtimer("Right");
    assertFalse(animator.hurtPlaying);
  }

  @Test
  void animationTimerShouldContinuePlayingCurrentAnimation() {
    animator.animateClimb();
    verify(renderer).startAnimation("climb");
  }
}
