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
  void shouldInitializeToWalkRight() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();

    verify(walkRCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_r");
    assertTrue(controller.isFacingRight());
    assertFalse(controller.isFacingLeft());
    assertEquals(
        CerberusAnimationController.AnimationState.WALK_RIGHT, controller.getCurrentState());
  }

  @Test
  void shouldTransitionToWalkLeftOnNegativeVelocity() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();

    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_l");
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

    // Triggered once in create(), once on direction switch
    verify(walkRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldMaintainDirectionWhenVelocityIsNearZero() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();

    // Walk left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();
    verify(walkLCallback, times(1)).handle();
    assertTrue(controller.isFacingLeft());

    // Stop moving
    when(body.getLinearVelocity()).thenReturn(new Vector2(0f, 0f));
    controller.update();

    // Still facing left, no new trigger
    verify(walkLCallback, times(1)).handle();
    assertTrue(controller.isFacingLeft());
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
  void shouldFaceTargetOnMeleeAttackWhenTargetOnLeft() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_l");
    assertTrue(controller.isFacingLeft());
  }

  @Test
  void shouldFaceTargetOnMeleeAttackWhenTargetOnRight() {
    EventListener0 walkRCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkRightStart", walkRCallback);

    entity.create();
    // Start facing left
    when(body.getLinearVelocity()).thenReturn(new Vector2(-1f, 0f));
    controller.update();

    entity.setPosition(2f, 0f);
    Entity target = new Entity();
    target.setPosition(5f, 0f);

    entity.getEvents().trigger("meleeAttack", target);

    // Initial create() + meleeAttack turning right = 2 times
    verify(walkRCallback, times(2)).handle();
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_r");
    assertTrue(controller.isFacingRight());
  }

  @Test
  void shouldFaceTargetOnMeleeAttackWindup() {
    EventListener0 walkLCallback = mock(EventListener0.class);
    entity.getEvents().addListener("walkLeftStart", walkLCallback);

    entity.create();
    entity.setPosition(5f, 0f);

    Entity target = new Entity();
    target.setPosition(2f, 0f);

    entity.getEvents().trigger("meleeAttackWindup", target);

    verify(walkLCallback, times(1)).handle();
    verify(animationRenderComponent).startAnimation("cerberus_l");
  }

  @Test
  void shouldHandleNullTargetOnMeleeAttackGracefully() {
    entity.create();
    assertDoesNotThrow(() -> entity.getEvents().trigger("meleeAttack", (Entity) null));
  }

  @Test
  void shouldRespondToIdleEventsByMappingToWalking() {
    entity.create();

    entity.getEvents().trigger("idleLeftStart");
    verify(animationRenderComponent).startAnimation("cerberus_l");
    assertTrue(controller.isFacingLeft());

    entity.getEvents().trigger("idleRightStart");
    verify(animationRenderComponent, times(2)).startAnimation("cerberus_r");
    assertTrue(controller.isFacingRight());
  }
}
