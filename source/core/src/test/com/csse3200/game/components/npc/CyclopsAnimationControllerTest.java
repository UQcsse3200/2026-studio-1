package com.csse3200.game.components.npc;

import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class CyclopsAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private CyclopsAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new CyclopsAnimationController();

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
    verify(animationRenderComponent).startAnimation("cyclops_idle_r");
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
    verify(animationRenderComponent).startAnimation("cyclops_walk_l");
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
    verify(animationRenderComponent).startAnimation("cyclops_walk_r");
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
    verify(animationRenderComponent).startAnimation("cyclops_walk_l");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    verify(idleLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cyclops_idle_l");
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
    verify(animationRenderComponent).startAnimation("cyclops_walk_r");

    // 2. Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();
    // idleRightStart triggered once in create(), once after stopping
    verify(idleRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("cyclops_idle_r");
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
    verify(animationRenderComponent, times(1)).startAnimation("cyclops_walk_r");
  }

  @Test
  void shouldTriggerRockThrowLeftOnEvent() {
    EventListener0 rockThrowLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowLeftStart", rockThrowLCallback);

    entity.create();

    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("rockThrow", target);

    verify(rockThrowLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cyclops_rock_l");
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
    org.junit.jupiter.api.Assertions.assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldTriggerRockThrowRightOnEvent() {
    EventListener0 rockThrowRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowRightStart", rockThrowRCallback);

    entity.create();

    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("rockThrow", target);

    verify(rockThrowRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cyclops_rock_r");
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
    org.junit.jupiter.api.Assertions.assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldTriggerRockThrowOnRangedAttackWindupAndFired() {
    EventListener0 rockThrowRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowRightStart", rockThrowRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("rangedAttackWindup", target);

    verify(rockThrowRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cyclops_rock_r");
  }

  @Test
  void shouldTriggerLaserLeftWithWeaponAnimator() {
    EnemyWeaponAnimationComponent weaponAnimator = mock(EnemyWeaponAnimationComponent.class);
    when(weaponAnimator.hasAnimation("cyclops_laser_l")).thenReturn(true);
    when(animationRenderComponent.hasAnimation("cyclops_taunt_l")).thenReturn(true);

    entity.addComponent(weaponAnimator);
    EventListener0 laserLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("laserLeftStart", laserLCallback);

    entity.create();
    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);

    verify(laserLCallback, times(1)).handle();
    verify(weaponAnimator).startAnimation("cyclops_laser_l");
    verify(animationRenderComponent).startAnimation("cyclops_taunt_l");
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerLaserRightWithWeaponAnimator() {
    EnemyWeaponAnimationComponent weaponAnimator = mock(EnemyWeaponAnimationComponent.class);
    when(weaponAnimator.hasAnimation("cyclops_laser_r")).thenReturn(true);
    when(animationRenderComponent.hasAnimation("cyclops_taunt_r")).thenReturn(true);

    entity.addComponent(weaponAnimator);
    EventListener0 laserRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("laserRightStart", laserRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);

    verify(laserRCallback, times(1)).handle();
    verify(weaponAnimator).startAnimation("cyclops_laser_r");
    verify(animationRenderComponent).startAnimation("cyclops_taunt_r");
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerLaserOnLightningRangedAttack() {
    EnemyWeaponAnimationComponent weaponAnimator = mock(EnemyWeaponAnimationComponent.class);
    when(weaponAnimator.hasAnimation("cyclops_laser_r")).thenReturn(true);
    when(animationRenderComponent.hasAnimation("cyclops_taunt_r")).thenReturn(true);

    entity.addComponent(weaponAnimator);
    EventListener0 laserRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("laserRightStart", laserRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity
        .getEvents()
        .trigger(
            "rangedAttack",
            target,
            com.csse3200.game.components.projectile.ProjectileType.LIGHTNING);

    verify(laserRCallback, times(1)).handle();
    verify(weaponAnimator).startAnimation("cyclops_laser_r");
  }

  @Test
  void shouldTriggerStompOnMeleeAttack() {
    EventListener0 stompRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("stompRightStart", stompRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(stompRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cyclops_stomp_r");
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldNotInterruptAttackWhilePlaying() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("rockThrow", target);
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());

    when(animationRenderComponent.isFinished()).thenReturn(false);
    lenient().when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();

    verify(walkRCallback, times(0)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldResumeWalkOnceAttackFinishes() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("rockThrow", target);

    when(animationRenderComponent.isFinished()).thenReturn(true);
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));

    controller.update();

    verify(walkRCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertFalse(controller.isAttacking());
  }

  @Test
  void shouldStopWeaponAnimatorWhenLaserFinishes() {
    EnemyWeaponAnimationComponent weaponAnimator = mock(EnemyWeaponAnimationComponent.class);
    when(weaponAnimator.hasAnimation("cyclops_laser_r")).thenReturn(true);
    when(weaponAnimator.getCurrentAnimation()).thenReturn("cyclops_laser_r");

    entity.addComponent(weaponAnimator);
    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("laserAttack", target, ProjectileType.ARROW);

    when(weaponAnimator.isFinished()).thenReturn(true);
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));

    controller.update();

    verify(weaponAnimator).stopAnimation();
    org.junit.jupiter.api.Assertions.assertFalse(controller.isAttacking());
  }

  @Test
  void shouldTriggerLaserOnLaserAttackStart() {
    EventListener0 laserRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("laserRightStart", laserRCallback);

    entity.create();
    entity.getEvents().trigger("laserAttackStart");

    verify(laserRCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerLaserOnLaserAttackWindup() {
    EventListener0 laserLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("laserLeftStart", laserLCallback);

    entity.create();
    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("laserAttackWindup", target);

    verify(laserLCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerRockOnRockAttackStart() {
    EventListener0 rockRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowRightStart", rockRCallback);

    entity.create();
    entity.getEvents().trigger("rockAttackStart");

    verify(rockRCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerRockOnRockAttack() {
    EventListener0 rockLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowLeftStart", rockLCallback);

    entity.create();
    entity.setPosition(5f, 0f);
    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("rockAttack", target, ProjectileType.ARROW);

    verify(rockLCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }

  @Test
  void shouldTriggerRockOnRockAttackWindup() {
    EventListener0 rockRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("rockThrowRightStart", rockRCallback);

    entity.create();
    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("rockAttackWindup", target);

    verify(rockRCallback, times(1)).handle();
    org.junit.jupiter.api.Assertions.assertTrue(controller.isAttacking());
  }
}
