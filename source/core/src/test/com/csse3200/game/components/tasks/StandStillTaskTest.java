package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link StandStillTask}: an NPC that stops and faces the player while the player is within
 * dialogue range. The components the task reads (proximity, movement, animator) are mocks, so each
 * test decides what the task sees.
 */
@ExtendWith(GameExtension.class)
class StandStillTaskTest {
  private static final int PRIORITY = 5;

  private Entity npc;
  private Entity player;
  private DialogueProximityComponent proximity;
  private PhysicsMovementComponent movement;
  private AnimationRenderComponent animator;
  private StandStillTask task;

  @BeforeEach
  void setUp() {
    proximity = mock(DialogueProximityComponent.class);
    movement = mock(PhysicsMovementComponent.class);
    animator = mock(AnimationRenderComponent.class);
    npc = new Entity().addComponent(proximity).addComponent(movement).addComponent(animator);
    npc.setPosition(5f, 0f);

    player = new Entity();
    player.setPosition(0f, 0f);

    task = new StandStillTask(player, PRIORITY);
    task.create(() -> npc);
  }

  // ---------- priority ----------

  @Test
  void shouldHaveItsPriorityWhileThePlayerIsInDialogueRange() {
    when(proximity.isPlayerInRange()).thenReturn(true);

    assertEquals(PRIORITY, task.getPriority());
  }

  @Test
  void shouldHaveNegativePriorityWhileThePlayerIsOutOfDialogueRange() {
    when(proximity.isPlayerInRange()).thenReturn(false);

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldHaveNegativePriorityWithoutAProximityComponent() {
    Entity plain = new Entity();
    StandStillTask plainTask = new StandStillTask(player, PRIORITY);
    plainTask.create(() -> plain);

    assertEquals(-1, plainTask.getPriority());
  }

  @Test
  void shouldHaveNegativePriorityWithoutAPlayer() {
    when(proximity.isPlayerInRange()).thenReturn(true);
    StandStillTask noPlayer = new StandStillTask(null, PRIORITY);
    noPlayer.create(() -> npc);

    assertEquals(-1, noPlayer.getPriority());
  }

  // ---------- starting ----------

  @Test
  void shouldStopMovingWhenItStarts() {
    task.start();

    verify(movement).setMoving(false);
  }

  @Test
  void shouldStartWithoutAMovementComponent() {
    Entity plain = new Entity();
    StandStillTask plainTask = new StandStillTask(player, PRIORITY);
    plainTask.create(() -> plain);

    assertDoesNotThrow(plainTask::start);
  }

  // ---------- facing the player ----------

  @Test
  void shouldFaceLeftWhenThePlayerIsOnTheLeft() {
    EventListener0 faceLeft = listenFor("idleLeftStart");
    EventListener0 faceRight = listenFor("idleRightStart");
    when(animator.getCurrentAnimation()).thenReturn("idler");

    task.update();

    verify(faceLeft).handle();
    verify(faceRight, never()).handle();
  }

  @Test
  void shouldFaceRightWhenThePlayerIsOnTheRight() {
    player.setPosition(10f, 0f);
    EventListener0 faceLeft = listenFor("idleLeftStart");
    EventListener0 faceRight = listenFor("idleRightStart");
    when(animator.getCurrentAnimation()).thenReturn("idlel");

    task.update();

    verify(faceRight).handle();
    verify(faceLeft, never()).handle();
  }

  @Test
  void shouldNotRestartTheIdleAnimationItIsAlreadyPlaying() {
    EventListener0 faceLeft = listenFor("idleLeftStart");
    when(animator.getCurrentAnimation()).thenReturn("idlel");

    task.update();

    verify(faceLeft, never()).handle();
  }

  @Test
  void shouldDoNothingWhenThereIsNoAnimator() {
    Entity plain = new Entity();
    StandStillTask plainTask = new StandStillTask(player, PRIORITY);
    plainTask.create(() -> plain);

    assertDoesNotThrow(plainTask::update);
  }

  private EventListener0 listenFor(String event) {
    EventListener0 listener = mock(EventListener0.class);
    npc.getEvents().addListener(event, listener);
    return listener;
  }
}
