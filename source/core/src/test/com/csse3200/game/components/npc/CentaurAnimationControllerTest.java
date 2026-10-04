package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class CentaurAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private CentaurAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new CentaurAnimationController();

    // Stub physics component to return a mock body
    lenient().when(physicsComponent.getBody()).thenReturn(body);

    entity
        .addComponent(physicsComponent)
        .addComponent(animationRenderComponent)
        .addComponent(controller);
  }

  @Test
  void shouldInitializeToIdleRight() {
    EventListener0 idleRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("idleRightStart", idleRCallback);

    entity.create();

    verify(idleRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_idle_r");
  }

  @Test
  void shouldTransitionToRunLeftOnNegativeVelocity() {
    EventListener0 runLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runLeftStart", runLCallback);

    entity.create();

    // Set mock body linear velocity to negative x (moving left)
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));

    controller.update();

    verify(runLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_l");
  }

  @Test
  void shouldTransitionToRunRightOnPositiveVelocity() {
    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create();

    // Set mock body linear velocity to positive x (moving right)
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();

    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");
  }

  @Test
  void shouldTransitionToIdleLeftAfterRunLeft() {
    EventListener0 runLCallback = mock(EventListener0.class);
    EventListener0 idleLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runLeftStart", runLCallback);
    entity.getEvents().addListener("idleLeftStart", idleLCallback);

    entity.create();

    // 1. Run left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(runLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_l");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_idle_l");
  }

  @Test
  void shouldTransitionToIdleRightAfterRunRight() {
    EventListener0 runRCallback = mock(EventListener0.class);
    EventListener0 idleRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);
    entity.getEvents().addListener("idleRightStart", idleRCallback);

    entity.create();

    // 1. Run right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    // idleRightStart triggered once in create(), once after stopping
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("centaur_idle_r");
  }

  @Test
  void shouldNotTriggerEventRepeatedlyOnSameState() {
    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create();

    // Moving right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();
    controller.update(); // Update again with same velocity

    verify(runRCallback, times(1)).handle(); // Should only trigger once
    verify(animationRenderComponent, times(1)).startAnimation("centaur_run_r");
  }

  @Test
  void shouldTransitionToSwingRightOnAttackWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldTransitionToSwingLeftOnAttackWhenTargetOnLeft() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 swingLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingLeftStart", swingLCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(swingLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_swing_l");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldTransitionToSwingOnRangedAttackFired() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("rangedAttackFired", target);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldHandleAttackWithNullTargetGracefully() {
    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", null);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldNotInterruptSwingWithRunOrIdleUntilFinished() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create();

    // Trigger attack
    entity.getEvents().trigger("meleeAttack", target);
    assertTrue(controller.isSwinging());

    // While swinging and animation not finished, moving should not trigger run animation
    when(animationRenderComponent.isFinished()).thenReturn(false);
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    verify(runRCallback, never()).handle();
    assertTrue(controller.isSwinging());

    // Once animation finishes, update should transition to running
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();

    assertFalse(controller.isSwinging());
    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");
  }

  @Test
  void shouldInterruptSwingWhenChargeStarts() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create();

    // Trigger attack
    entity.getEvents().trigger("meleeAttack", target);
    assertTrue(controller.isSwinging());

    // Trigger chargeStart event
    entity.getEvents().trigger("chargeStart");

    assertFalse(controller.isSwinging());
    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");
  }

  @Test
  void shouldPlayDeathAnimations() {
    entity.create();

    entity.getEvents().trigger("deathLeftStart");
    verify(animationRenderComponent).startAnimation("centaur_death_l");

    entity.getEvents().trigger("deathRightStart");
    verify(animationRenderComponent).startAnimation("centaur_death_r");
  }

  @Test
  void shouldTrackFacingDirectionCorrectly() {
    entity.create();
    assertTrue(controller.isFacingRight());

    // Move left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    assertFalse(controller.isFacingRight());

    // Stop (should remain facing left in idle)
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    assertFalse(controller.isFacingRight());

    // Move right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    assertTrue(controller.isFacingRight());
  }
}
