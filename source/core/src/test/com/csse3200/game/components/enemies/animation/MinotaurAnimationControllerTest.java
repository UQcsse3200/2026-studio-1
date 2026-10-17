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
class MinotaurAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private MinotaurAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new MinotaurAnimationController();

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
    verify(animationRenderComponent).startAnimation("minotaur_idle_r");
  }

  @Test
  void shouldTransitionToWalkLeftOnNegativeVelocity() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();

    // Set mock body linear velocity to negative x (moving left)
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));

    controller.update();

    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_l");
  }

  @Test
  void shouldTransitionToWalkRightOnPositiveVelocity() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    // Set mock body linear velocity to positive x (moving right)
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_r");
  }

  @Test
  void shouldTransitionToIdleLeftAfterWalkLeft() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    EventListener0 idleLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);
    entity.getEvents().addListener("idleLeftStart", idleLCallback);

    entity.create();

    // 1. Walk left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_l");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_idle_l");
  }

  @Test
  void shouldTransitionToIdleRightAfterWalkRight() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    EventListener0 idleRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);
    entity.getEvents().addListener("idleRightStart", idleRCallback);

    entity.create();

    // 1. Walk right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_r");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    // idleRightStart triggered once in create(), once after stopping
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("minotaur_idle_r");
  }

  @Test
  void shouldNotTriggerEventRepeatedlyOnSameState() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    // Moving right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();
    controller.update(); // Update again with same velocity

    verify(walkRCallback, times(1)).handle(); // Should only trigger once
    verify(animationRenderComponent, times(1)).startAnimation("minotaur_walk_r");
  }

  @Test
  void shouldTransitionToChargeRightWhenStationaryInChargeWindup() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(false);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");
  }

  @Test
  void shouldTransitionToChargeLeftWhenStationaryInChargeWindup() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(false);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(-5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    verify(chargeLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_l");
  }

  @Test
  void shouldTransitionToWalkRightWhenChargingInRushPhase() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(true);
    entity.addComponent(chargeComponent);

    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(3f, 0f));
    controller.update();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_r");
  }

  @Test
  void shouldTransitionFromChargeToIdleWhenChargeEnds() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(false);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeRCallback = mock(EventListener0.class);
    EventListener0 idleRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);
    entity.getEvents().addListener("idleRightStart", idleRCallback);

    entity.create();

    // 1. Charge windup right (stationary)
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");

    // 2. Charge ends and entity remains stationary
    when(chargeComponent.isCharging()).thenReturn(false);
    controller.update();
    // idleRightStart was called once on create(), now a second time after stopping
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("minotaur_idle_r");
  }

  @Test
  void shouldTriggerChargeOnChargeStartEvent() {
    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();

    // Trigger chargeStart event directly
    entity.getEvents().trigger("chargeStart");

    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");
  }

  @Test
  void shouldTransitionToSwingRightOnMeleeAttackWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldTransitionToSwingLeftOnMeleeAttackWhenTargetOnLeft() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 swingLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingLeftStart", swingLCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(swingLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_swing_l");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldTransitionToSwingOnMeleeAttackWindup() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttackWindup", target);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldHandleMeleeAttackWithNullTargetGracefully() {
    EventListener0 swingRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("swingRightStart", swingRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", null);

    verify(swingRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_swing_r");
    assertTrue(controller.isSwinging());
  }

  @Test
  void shouldNotInterruptSwingWithWalkOrIdleUntilFinished() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    // Trigger swing attack
    entity.getEvents().trigger("meleeAttack", target);
    assertTrue(controller.isSwinging());

    // While swinging and animation not finished, moving should not trigger walk animation
    when(animationRenderComponent.isFinished()).thenReturn(false);
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    verify(walkRCallback, never()).handle();
    assertTrue(controller.isSwinging());

    // Once animation finishes, update should transition to walking
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();

    assertFalse(controller.isSwinging());
    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_r");
  }

  @Test
  void shouldInterruptSwingWhenChargeStarts() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();

    // Trigger melee swing
    entity.getEvents().trigger("meleeAttack", target);
    assertTrue(controller.isSwinging());

    // Trigger chargeStart event
    entity.getEvents().trigger("chargeStart");

    assertFalse(controller.isSwinging());
    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");
  }

  /** A charge toward a target on the left plays the left charge animation. */
  @Test
  void shouldChargeLeftWhenTargetIsOnTheLeft() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(-5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);

    entity.create();
    entity.setPosition(0f, 0f);

    entity.getEvents().trigger("chargeStart");

    verify(chargeLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_l");
  }

  /** A charge toward a target on the right plays the right charge animation. */
  @Test
  void shouldChargeRightWhenTargetIsOnTheRight() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();
    entity.setPosition(0f, 0f);

    entity.getEvents().trigger("chargeStart");

    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");
  }

  /** With no target, charge direction follows the velocity. */
  @Test
  void shouldChargeInDirectionOfVelocityWhenNoTarget() {
    EventListener0 chargeLCallback = mock(EventListener0.class);
    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create(); // no ChargeComponent on the entity, so there is no target position

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(chargeLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_l");

    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(chargeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_charge_r");
  }

  /** With no target and no velocity, the charge keeps the current facing. */
  @Test
  void shouldChargeInCurrentFacingWhenStationaryAndNoTarget() {
    EventListener0 chargeLCallback = mock(EventListener0.class);
    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create(); // starts facing right

    // facing right and stationary
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(chargeRCallback, times(1)).handle();
    verify(chargeLCallback, never()).handle();

    // walk left so the controller faces left, then stand still and charge
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    entity.getEvents().trigger("chargeStart");
    verify(chargeLCallback, times(1)).handle();
  }

  /** With no physics body the charge still starts and does not throw. */
  @Test
  void shouldStartChargeWithoutPhysicsBody() {
    lenient().when(physicsComponent.getBody()).thenReturn(null);

    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create(); // facing right

    assertDoesNotThrow(() -> entity.getEvents().trigger("chargeStart"));

    verify(chargeRCallback, times(1)).handle();
  }

  /** During the windup, the state faces the target. */
  @Test
  void shouldFaceTargetDuringChargeWindup() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(false);
    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(-5f, 0f));
    entity.addComponent(chargeComponent);

    EventListener0 chargeLCallback = mock(EventListener0.class);
    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));

    controller.update(); // target on the left
    verify(chargeLCallback, times(1)).handle();

    when(chargeComponent.getTargetPosition()).thenReturn(new Vector2(5f, 0f));
    controller.update(); // target moved to the right
    verify(chargeRCallback, times(1)).handle();
  }

  /** During the windup with no target, the state keeps the current facing. */
  @Test
  void shouldKeepFacingDuringChargeWindupWhenNoTarget() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(false);
    when(chargeComponent.isRushing()).thenReturn(false);
    // getTargetPosition() is not stubbed, so it returns null
    entity.addComponent(chargeComponent);

    EventListener0 chargeLCallback = mock(EventListener0.class);
    EventListener0 chargeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("chargeLeftStart", chargeLCallback);
    entity.getEvents().addListener("chargeRightStart", chargeRCallback);

    entity.create();

    // walk left so the controller faces left, then begin the windup
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    when(chargeComponent.isCharging()).thenReturn(true);
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(chargeLCallback, times(1)).handle();

    // walk right so the controller faces right, then begin the windup again
    when(chargeComponent.isCharging()).thenReturn(false);
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    when(chargeComponent.isCharging()).thenReturn(true);
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(chargeRCallback, times(1)).handle();
  }

  /** While rushing, the velocity decides the state, not the windup rule. */
  @Test
  void shouldUseVelocityWhileRushing() {
    ChargeComponent chargeComponent = mock(ChargeComponent.class);
    when(chargeComponent.isCharging()).thenReturn(true);
    when(chargeComponent.isRushing()).thenReturn(true);
    entity.addComponent(chargeComponent);

    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("minotaur_walk_r");
  }
}
