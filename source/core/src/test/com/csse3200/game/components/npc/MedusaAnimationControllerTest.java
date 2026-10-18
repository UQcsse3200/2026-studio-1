package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.enemies.animation.MedusaAnimationController;
import com.csse3200.game.components.projectile.ProjectileType;
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
class MedusaAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private MedusaAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new MedusaAnimationController();

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
    verify(animationRenderComponent).startAnimation("gorgon_idle_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isFacingLeft());
    assertEquals(MedusaAnimationController.AnimationState.IDLE_RIGHT, controller.getCurrentState());
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
    verify(animationRenderComponent).startAnimation("gorgon_walk_l");
    assertTrue(controller.isFacingLeft());
    assertFalse(controller.isFacingRight());
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
    verify(animationRenderComponent).startAnimation("gorgon_walk_r");
    assertTrue(controller.isFacingRight());
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
    verify(animationRenderComponent).startAnimation("gorgon_walk_l");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_idle_l");
    assertTrue(controller.isFacingLeft());
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
    verify(animationRenderComponent).startAnimation("gorgon_walk_r");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    // idleRightStart triggered once in create(), once after stopping
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("gorgon_idle_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldNotTriggerEventRepeatedlyOnSameState() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();
    controller.update();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent, times(1)).startAnimation("gorgon_walk_r");
  }

  @Test
  void shouldTransitionToAttackRightOnMeleeAttackWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 attackRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackRightStart", attackRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(attackRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_attack_r");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldTransitionToAttackLeftOnMeleeAttackWhenTargetOnLeft() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 attackLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackLeftStart", attackLCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(attackLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_attack_l");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldTransitionToPetrifyOnRangedAttackEvents() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 petrifyLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("petrifyLeftStart", petrifyLCallback);

    entity.create();

    // Ranged attack with projectile
    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    verify(petrifyLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_petrify_l");
    assertTrue(controller.isAttacking());
    assertEquals(
        MedusaAnimationController.AnimationState.PETRIFY_LEFT, controller.getCurrentState());
    assertTrue(controller.isFacingLeft());

    // While attacking, subsequent attack events must not restart the animation
    entity.getEvents().trigger("rangedAttackWindup", target);
    entity.getEvents().trigger("rangedAttackFired", target);

    verify(petrifyLCallback, times(1)).handle();
    verify(animationRenderComponent, times(1)).startAnimation("gorgon_petrify_l");

    // Once finished, new attack can trigger animation
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();
    assertFalse(controller.isAttacking());

    entity.getEvents().trigger("rangedAttackWindup", target);
    verify(petrifyLCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("gorgon_petrify_l");
  }

  @Test
  void shouldTransitionToPetrifyRightWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 petrifyRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("petrifyRightStart", petrifyRCallback);

    entity.create();

    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    verify(petrifyRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_petrify_r");
    assertTrue(controller.isAttacking());
    assertEquals(
        MedusaAnimationController.AnimationState.PETRIFY_RIGHT, controller.getCurrentState());
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldHandleAttackWithNullTargetGracefully() {
    EventListener0 attackRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("attackRightStart", attackRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", null);

    verify(attackRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_attack_r");
    assertTrue(controller.isAttacking());
  }

  @Test
  void shouldNotInterruptAttackWithWalkUntilFinished() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    // Trigger attack
    entity.getEvents().trigger("meleeAttack", target);
    assertTrue(controller.isAttacking());

    // While attacking and animation not finished, moving should not trigger walk
    when(animationRenderComponent.isFinished()).thenReturn(false);
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    verify(walkRCallback, never()).handle();
    assertTrue(controller.isAttacking());

    // Once animation finishes, update should transition to walking
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();

    assertFalse(controller.isAttacking());
    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_walk_r");
  }

  @Test
  void shouldPlayDeathAnimationsOnDeathEvent() {
    entity.create();

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("gorgon_death_r");
    assertTrue(controller.isDead());
    assertFalse(controller.isAttacking());

    // Movement after death should not trigger animations
    lenient().when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(animationRenderComponent, never()).startAnimation("gorgon_walk_l");

    // Attack after death should not trigger animations
    Entity target = new Entity();
    target.setPosition(-5f, 0f);
    entity.getEvents().trigger("meleeAttack", target);
    verify(animationRenderComponent, never()).startAnimation("gorgon_attack_l");
  }

  @Test
  void shouldPlayDeathLeftWhenFacingLeftOnDeath() {
    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    assertTrue(controller.isFacingLeft());

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("gorgon_death_l");
    assertTrue(controller.isDead());
  }

  @Test
  void shouldRespondToDirectAttackAndDeathEvents() {
    entity.create();

    entity.getEvents().trigger("attackLeftStart");
    verify(animationRenderComponent).startAnimation("gorgon_attack_l");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("attackRightStart");
    verify(animationRenderComponent).startAnimation("gorgon_attack_r");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("deathLeftStart");
    verify(animationRenderComponent).startAnimation("gorgon_death_l");
    assertTrue(controller.isDead());

    entity.getEvents().trigger("deathRightStart");
    verify(animationRenderComponent).startAnimation("gorgon_death_r");
    assertTrue(controller.isDead());

    entity.getEvents().trigger("petrifyLeftStart");
    verify(animationRenderComponent).startAnimation("gorgon_petrify_l");
    assertTrue(controller.isAttacking());
    assertEquals(
        MedusaAnimationController.AnimationState.PETRIFY_LEFT, controller.getCurrentState());

    entity.getEvents().trigger("petrifyRightStart");
    verify(animationRenderComponent).startAnimation("gorgon_petrify_r");
    assertTrue(controller.isAttacking());
    assertEquals(
        MedusaAnimationController.AnimationState.PETRIFY_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldPlayPetrifyOnDirectPetrifyAndGazeEvents() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 petrifyRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("petrifyRightStart", petrifyRCallback);

    entity.create();

    entity.getEvents().trigger("petrify", target);
    verify(petrifyRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("gorgon_petrify_r");

    // Complete first attack
    when(animationRenderComponent.isFinished()).thenReturn(true);
    controller.update();

    entity.getEvents().trigger("gaze", target);
    verify(petrifyRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("gorgon_petrify_r");

    // Complete second attack
    controller.update();

    entity.getEvents().trigger("petrifyAttack", target);
    verify(petrifyRCallback, times(3)).handle();
    verify(animationRenderComponent, times(3)).startAnimation("gorgon_petrify_r");
  }

  @Test
  void shouldDelegateRangedTaskPollingWhenRangedAttackComponentPresent() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    com.csse3200.game.components.enemies.attacks.RangedAttackComponent rangedComp =
        mock(com.csse3200.game.components.enemies.attacks.RangedAttackComponent.class);
    entity.addComponent(rangedComp);

    EventListener0 petrifyLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("petrifyLeftStart", petrifyLCallback);

    entity.create();

    // Ranged task poll ("rangedAttack") should NOT trigger animation directly when
    // RangedAttackComponent is present
    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    verify(petrifyLCallback, never()).handle();
    verify(animationRenderComponent, never()).startAnimation("gorgon_petrify_l");

    // RangedAttackComponent announces windup when attack is accepted: animation starts
    entity.getEvents().trigger("rangedAttackWindup", target);
    verify(petrifyLCallback, times(1)).handle();
    verify(animationRenderComponent, times(1)).startAnimation("gorgon_petrify_l");

    // Repeated task polling during windup does not restart the animation
    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    verify(petrifyLCallback, times(1)).handle();

    // RangedAttackComponent announces fired when projectile releases: does not replay
    entity.getEvents().trigger("rangedAttackFired", target);
    verify(petrifyLCallback, times(1)).handle();
  }

  @Test
  void shouldNotPlayPetrifyAfterDeath() {
    entity.create();
    entity.getEvents().trigger("death");
    assertTrue(controller.isDead());

    Entity target = new Entity();
    target.setPosition(-5f, 0f);
    entity.getEvents().trigger("petrify", target);
    verify(animationRenderComponent, never()).startAnimation("gorgon_petrify_l");

    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
    verify(animationRenderComponent, never()).startAnimation("gorgon_petrify_l");
  }
}
