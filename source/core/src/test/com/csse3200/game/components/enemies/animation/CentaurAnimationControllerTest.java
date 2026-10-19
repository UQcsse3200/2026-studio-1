package com.csse3200.game.components.enemies.animation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.enemies.attacks.ChargeComponent;
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

  /** A charge toward a target on the left plays the left run animation. */
  @Test
  void shouldRunLeftWhenChargeTargetIsOnTheLeft() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(-5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 runLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runLeftStart", runLCallback);

    entity.create();
    entity.setPosition(0f, 0f);

    entity.getEvents().trigger("chargeStart");

    verify(runLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_l");
  }

  /** A charge toward a target on the right plays the right run animation. */
  @Test
  void shouldRunRightWhenChargeTargetIsOnTheRight() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create();
    entity.setPosition(0f, 0f);

    entity.getEvents().trigger("chargeStart");

    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");
  }

  /** With no target, charge direction follows the velocity. */
  @Test
  void shouldRunInDirectionOfVelocityWhenChargeHasNoTarget() {
    EventListener0 runLCallback = mock(EventListener0.class);
    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runLeftStart", runLCallback);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create(); // no ChargeComponent on the entity, so there is no target position

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(runLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_l");

    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(runRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("centaur_run_r");
  }

  /** With no target and no velocity, the charge keeps the current facing. */
  @Test
  void shouldRunInCurrentFacingWhenStationaryAndNoTarget() {
    EventListener0 runLCallback = mock(EventListener0.class);
    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runLeftStart", runLCallback);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create(); // starts facing right

    // facing right and stationary
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(runRCallback, times(1)).handle();
    verify(runLCallback, never()).handle();

    // run left so the controller faces left, then stand still and charge.
    // runLeftStart fires once for the run and once more for the charge.
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(runLCallback, times(2)).handle();
  }

  /** With no physics body the charge still starts and does not throw. */
  @Test
  void shouldStartChargeWithoutPhysicsBody() {
    lenient().when(physicsComponent.getBody()).thenReturn(null);

    EventListener0 runRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("runRightStart", runRCallback);

    entity.create(); // facing right

    assertDoesNotThrow(() -> entity.getEvents().trigger("chargeStart"));

    verify(runRCallback, times(1)).handle();
  }

  @Test
  void shouldPlayDeathAnimationsOnDeathEvent() {
    entity.create();

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("centaur_death_r");
    assertTrue(controller.isDead());
    assertFalse(controller.isSwinging());

    // Movement after death should not trigger animations
    lenient().when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(animationRenderComponent, never()).startAnimation("centaur_run_l");

    // Attack after death should not trigger animations
    Entity target = new Entity();
    target.setPosition(-5f, 0f);
    entity.getEvents().trigger("meleeAttack", target);
    verify(animationRenderComponent, never()).startAnimation("centaur_swing_l");
  }

  @Test
  void shouldPlayDeathLeftWhenFacingLeftOnDeath() {
    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    assertFalse(controller.isFacingRight());

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("centaur_death_l");
    assertTrue(controller.isDead());
  }

  @Test
  void shouldRespondToDirectDeathEvents() {
    entity.create();

    entity.getEvents().trigger("deathLeftStart");
    verify(animationRenderComponent).startAnimation("centaur_death_l");
    assertTrue(controller.isDead());

    entity.getEvents().trigger("deathRightStart");
    verify(animationRenderComponent).startAnimation("centaur_death_r");
  }
}
