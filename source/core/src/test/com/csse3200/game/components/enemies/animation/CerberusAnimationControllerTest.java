package com.csse3200.game.components.enemies.animation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
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
class CerberusAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private CerberusAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new CerberusAnimationController();

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
    verify(animationRenderComponent).startAnimation("cerberus_idle_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isFacingLeft());
    assertEquals(
        CerberusAnimationController.AnimationState.IDLE_RIGHT, controller.getCurrentState());
    assertFalse(controller.isAttacking());
    assertFalse(controller.isDead());
  }

  @Test
  void shouldTransitionToWalkLeftOnNegativeVelocity() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_walk_l");
    assertTrue(controller.isFacingLeft());
    assertFalse(controller.isFacingRight());
    assertEquals(
        CerberusAnimationController.AnimationState.WALK_LEFT, controller.getCurrentState());
  }

  @Test
  void shouldTransitionToWalkRightOnPositiveVelocity() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    // First walk left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    // Then walk right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_walk_r");
    assertTrue(controller.isFacingRight());
    assertEquals(
        CerberusAnimationController.AnimationState.WALK_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldTransitionToIdleWhenVelocityIsNearZero() {
    EventListener0 idleLCallback = mock(EventListener0.class);
    EventListener0 idleRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("idleLeftStart", idleLCallback);
    entity.getEvents().addListener("idleRightStart", idleRCallback);

    entity.create();

    // Walk left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    assertTrue(controller.isFacingLeft());

    // Stop moving -> IDLE_LEFT
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_idle_l");
    assertTrue(controller.isFacingLeft());
    assertEquals(
        CerberusAnimationController.AnimationState.IDLE_LEFT, controller.getCurrentState());

    // Walk right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    assertTrue(controller.isFacingRight());

    // Stop moving -> IDLE_RIGHT
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    // initial create() + stopping after walking right = 2 times
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_idle_r");
    assertTrue(controller.isFacingRight());
    assertEquals(
        CerberusAnimationController.AnimationState.IDLE_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldNotTriggerEventRepeatedlyOnSameState() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    controller.update();

    verify(walkLCallback, times(1)).handle();
  }

  @Test
  void shouldTriggerAttackLeftWhenTargetOnLeft() {
    EventListener0 attackLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackLeftStart", attackLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(attackLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_attack_l");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingLeft());
    assertEquals(
        CerberusAnimationController.AnimationState.ATTACK_LEFT, controller.getCurrentState());
  }

  @Test
  void shouldTriggerAttackRightWhenTargetOnRight() {
    EventListener0 attackRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackRightStart", attackRCallback);

    entity.create();
    // Face left initially
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(attackRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_attack_r");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingRight());
    assertEquals(
        CerberusAnimationController.AnimationState.ATTACK_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldTriggerAttackOnMeleeAttackWindup() {
    EventListener0 attackLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackLeftStart", attackLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttackWindup", target);

    verify(attackLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_attack_l");
    assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerAttackOnBiteEvent() {
    EventListener0 attackLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackLeftStart", attackLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("bite", target);

    verify(attackLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_attack_l");
    assertTrue(controller.isAttacking());
  }

  @Test
  void shouldHandleNullTargetOnMeleeAttackGracefully() {
    entity.create();
    assertDoesNotThrow(() -> entity.getEvents().trigger("meleeAttack", (Entity) null));
    assertTrue(controller.isAttacking());
  }

  @Test
  void shouldNotInterruptAttackUntilFinished() {
    entity.create();
    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttackWindup", target);
    verify(animationRenderComponent, times(1)).startAnimation("cerberus_attack_l");
    assertTrue(controller.isAttacking());

    // Second trigger while attack is playing does nothing
    entity.getEvents().trigger("meleeAttackWindup", target);
    verify(animationRenderComponent, times(1)).startAnimation("cerberus_attack_l");

    // Finished animation allows next attack
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();
    assertFalse(controller.isAttacking());

    entity.getEvents().trigger("meleeAttackWindup", target);
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_attack_l");
  }

  @Test
  void shouldDelegateMeleeTaskPollingWhenMeleeAttackComponentPresent() {
    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    MeleeAttackComponent meleeComp = mock(MeleeAttackComponent.class);
    entity.addComponent(meleeComp);

    EventListener0 attackLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackLeftStart", attackLCallback);

    entity.create();

    // Polling event from task does not start attack animation
    entity.getEvents().trigger("meleeAttack", target);
    verify(attackLCallback, never()).handle();
    verify(animationRenderComponent, never()).startAnimation("cerberus_attack_l");

    // Windup event from component starts attack animation
    entity.getEvents().trigger("meleeAttackWindup", target);
    verify(attackLCallback, times(1)).handle();
    verify(animationRenderComponent, times(1)).startAnimation("cerberus_attack_l");
  }

  @Test
  void shouldPlayDeathAnimationFacingRight() {
    EventListener0 deathRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("deathRightStart", deathRCallback);

    entity.create();
    entity.getEvents().trigger("death");

    verify(deathRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_death_r");
    assertTrue(controller.isDead());
    assertFalse(controller.isAttacking());
    assertEquals(
        CerberusAnimationController.AnimationState.DEATH_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldPlayDeathAnimationFacingLeft() {
    EventListener0 deathLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("deathLeftStart", deathLCallback);

    entity.create();
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    entity.getEvents().trigger("death");

    verify(deathLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_death_l");
    assertTrue(controller.isDead());
    assertEquals(
        CerberusAnimationController.AnimationState.DEATH_LEFT, controller.getCurrentState());
  }

  @Test
  void shouldNotAttackOrWalkWhenDead() {
    entity.create();
    entity.getEvents().trigger("death");
    assertTrue(controller.isDead());

    lenient().when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    verify(animationRenderComponent, never()).startAnimation("cerberus_walk_r");

    entity.getEvents().trigger("meleeAttackWindup", (Entity) null);
    verify(animationRenderComponent, never()).startAnimation("cerberus_attack_r");
  }

  @Test
  void shouldRespondToDirectAnimationEvents() {
    entity.create();

    entity.getEvents().trigger("idleLeftStart");
    verify(animationRenderComponent).startAnimation("cerberus_idle_l");
    assertTrue(controller.isFacingLeft());

    entity.getEvents().trigger("idleRightStart");
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_idle_r");
    assertTrue(controller.isFacingRight());

    entity.getEvents().trigger("walkLeftStart");
    verify(animationRenderComponent).startAnimation("cerberus_walk_l");

    entity.getEvents().trigger("walkRightStart");
    verify(animationRenderComponent).startAnimation("cerberus_walk_r");

    entity.getEvents().trigger("attackLeftStart");
    verify(animationRenderComponent).startAnimation("cerberus_attack_l");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("attackRightStart");
    verify(animationRenderComponent).startAnimation("cerberus_attack_r");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("deathLeftStart");
    verify(animationRenderComponent).startAnimation("cerberus_death_l");
    assertTrue(controller.isDead());

    entity.getEvents().trigger("deathRightStart");
    verify(animationRenderComponent).startAnimation("cerberus_death_r");
    assertTrue(controller.isDead());
  }
}
