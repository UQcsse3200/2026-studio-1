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
class HarpyAnimationControllerTest {
  @Mock PhysicsComponent physicsComponent;
  @Mock AnimationRenderComponent animationRenderComponent;
  @Mock Body body;

  private Entity entity;
  private HarpyAnimationController controller;

  @BeforeEach
  void beforeEach() {
    entity = new Entity();
    controller = new HarpyAnimationController();

    lenient().when(physicsComponent.getBody()).thenReturn(body);

    entity
        .addComponent(physicsComponent)
        .addComponent(animationRenderComponent)
        .addComponent(controller);
  }

  @Test
  void shouldInitializeToMoveRight() {
    EventListener0 moveRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveRightStart", moveRCallback);

    entity.create();

    verify(moveRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("harpy_y_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isFacingLeft());
    assertEquals(HarpyAnimationController.AnimationState.MOVE_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldTransitionToMoveLeftOnNegativeVelocity() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    verify(moveLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("harpy_y_l");
    assertTrue(controller.isFacingLeft());
    assertFalse(controller.isFacingRight());
  }

  @Test
  void shouldTransitionToMoveRightOnPositiveVelocity() {
    EventListener0 moveRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveRightStart", moveRCallback);

    entity.create();

    // First move left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    // Then move right
    when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 0f));
    controller.update();

    // Once in create(), once on direction change
    verify(moveRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("harpy_y_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldMaintainDirectionWhenVelocityIsNearZero() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();

    // Move left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(moveLCallback, times(1)).handle();
    assertTrue(controller.isFacingLeft());

    // Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    // Still facing left, no extra trigger
    verify(moveLCallback, times(1)).handle();
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldNotTriggerEventRepeatedlyOnSameState() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    controller.update();

    verify(moveLCallback, times(1)).handle();
  }

  @Test
  void shouldFaceTargetOnMeleeAttackWhenTargetOnLeft() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(moveLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("harpy_y_l");
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldFaceTargetOnMeleeAttackWhenTargetOnRight() {
    EventListener0 moveRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveRightStart", moveRCallback);

    entity.create();
    // Start facing left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(moveRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("harpy_y_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldFaceTargetOnRangedAttack() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);

    verify(moveLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("harpy_y_l");
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldFaceTargetOnRangedAttackWindupAndFired() {
    EventListener0 moveLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("moveLeftStart", moveLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("rangedAttackWindup", target);
    entity.getEvents().trigger("rangedAttackFired", target);

    verify(moveLCallback, times(2)).handle();
  }

  @Test
  void shouldHandleNullTargetGracefully() {
    entity.create();
    assertDoesNotThrow(() -> entity.getEvents().trigger("meleeAttack", (Entity) null));
    assertDoesNotThrow(
        () -> entity.getEvents().trigger("rangedAttack", null, ProjectileType.ARROW));
  }

  @Test
  void shouldRespondToFlyAndIdleEvents() {
    entity.create();

    entity.getEvents().trigger("flyLeftStart");
    verify(animationRenderComponent).startAnimation("harpy_y_l");
    assertTrue(controller.isFacingLeft());

    entity.getEvents().trigger("idleRightStart");
    verify(animationRenderComponent, times(2)).startAnimation("harpy_y_r");
    assertTrue(controller.isFacingRight());
  }
}
