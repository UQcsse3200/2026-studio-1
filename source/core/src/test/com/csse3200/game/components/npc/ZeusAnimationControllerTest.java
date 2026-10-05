package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
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
class ZeusAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private ZeusAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new ZeusAnimationController();

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
    verify(animationRenderComponent).startAnimation("zeus_idle_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isFacingLeft());
    assertEquals(ZeusAnimationController.AnimationState.IDLE_RIGHT, controller.getCurrentState());
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
    verify(animationRenderComponent).startAnimation("zeus_walk_l");
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
    verify(animationRenderComponent).startAnimation("zeus_walk_r");
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
    verify(animationRenderComponent).startAnimation("zeus_walk_l");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_idle_l");
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
    verify(animationRenderComponent).startAnimation("zeus_walk_r");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("zeus_idle_r");
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
    verify(animationRenderComponent, times(1)).startAnimation("zeus_walk_r");
  }

  @Test
  void shouldTransitionToStrikeRightOnMeleeAttackWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 strikeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("strikeRightStart", strikeRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(strikeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_p_strike_r");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldTransitionToStrikeLeftOnMeleeAttackWhenTargetOnLeft() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 strikeLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("strikeLeftStart", strikeLCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", target);

    verify(strikeLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_p_strike_l");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldTransitionToSlamRightOnRangedAttackWhenTargetOnRight() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    EventListener0 slamRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("slamRightStart", slamRCallback);

    entity.create();

    entity.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);

    verify(slamRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_slam_r");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldTransitionToSlamLeftOnRangedAttackWhenTargetOnLeft() {
    entity.setPosition(0f, 0f);
    Entity target = new Entity();
    target.setPosition(-5f, 0f);

    EventListener0 slamLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("slamLeftStart", slamLCallback);

    entity.create();

    entity.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);

    verify(slamLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_slam_l");
    assertTrue(controller.isAttacking());
    assertTrue(controller.isFacingLeft());

    // Windup and fired
    entity.getEvents().trigger("rangedAttackWindup", target);
    entity.getEvents().trigger("rangedAttackFired", target);

    verify(slamLCallback, times(3)).handle();
    verify(animationRenderComponent, times(3)).startAnimation("zeus_slam_l");
  }

  @Test
  void shouldHandleAttackWithNullTargetGracefully() {
    EventListener0 strikeRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("strikeRightStart", strikeRCallback);

    entity.create();

    entity.getEvents().trigger("meleeAttack", null);

    verify(strikeRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("zeus_p_strike_r");
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

    // Trigger melee attack
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
    verify(animationRenderComponent).startAnimation("zeus_walk_r");
  }

  @Test
  void shouldPlayDeathAnimationsOnDeathEvent() {
    entity.create();

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("zeus_death_r");
    assertTrue(controller.isDead());
    assertFalse(controller.isAttacking());

    // Movement after death should not trigger animations
    lenient().when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(animationRenderComponent, never()).startAnimation("zeus_walk_l");

    // Attack after death should not trigger animations
    Entity target = new Entity();
    target.setPosition(-5f, 0f);
    entity.getEvents().trigger("meleeAttack", target);
    verify(animationRenderComponent, never()).startAnimation("zeus_p_strike_l");
  }

  @Test
  void shouldPlayDeathLeftWhenFacingLeftOnDeath() {
    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    assertTrue(controller.isFacingLeft());

    entity.getEvents().trigger("death");

    verify(animationRenderComponent).startAnimation("zeus_death_l");
    assertTrue(controller.isDead());
  }

  @Test
  void shouldRespondToDirectStrikeSlamAndDeathEvents() {
    entity.create();

    entity.getEvents().trigger("strikeLeftStart");
    verify(animationRenderComponent).startAnimation("zeus_p_strike_l");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("strikeRightStart");
    verify(animationRenderComponent).startAnimation("zeus_p_strike_r");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("slamLeftStart");
    verify(animationRenderComponent).startAnimation("zeus_slam_l");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("slamRightStart");
    verify(animationRenderComponent).startAnimation("zeus_slam_r");
    assertTrue(controller.isAttacking());

    entity.getEvents().trigger("deathLeftStart");
    verify(animationRenderComponent).startAnimation("zeus_death_l");
    assertTrue(controller.isDead());

    entity.getEvents().trigger("deathRightStart");
    verify(animationRenderComponent).startAnimation("zeus_death_r");
    assertTrue(controller.isDead());
  }
}
