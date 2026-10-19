package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RetaliateTaskTest {
  private Entity npc;
  private Entity player;
  private ProvokedComponent provoked;
  private RetaliateTask task;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0f);
    ServiceLocator.registerTimeSource(time);

    npc =
        new Entity()
            .addComponent(new CombatStatsComponent(50, 0))
            .addComponent(new ProvokedComponent(8f));
    npc.create();
    npc.setPosition(0f, 0f);
    provoked = npc.getComponent(ProvokedComponent.class);

    player = new Entity();

    task = new RetaliateTask(player, 10, 4f, "headbutt");
    task.create(() -> npc);
  }

  @Test
  void shouldHaveNegativePriorityWhenNotProvoked() {
    player.setPosition(2f, 0f);

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldHavePriorityWhenAttackReadyAndInRange() {
    player.setPosition(2f, 0f);
    hitNpc();

    assertEquals(10, task.getPriority());
  }

  @Test
  void shouldHaveNegativePriorityWhenOutOfRange() {
    player.setPosition(10f, 0f);
    hitNpc();

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldKeepPriorityWhileAttacking() {
    player.setPosition(10f, 0f);
    hitNpc();
    provoked.useAttack();
    provoked.setAttacking(true);

    assertEquals(10, task.getPriority());
  }

  @Test
  void shouldTriggerTheAttackEventWithThePlayerWhenAnAttackIsReadyAndInRange() {
    EventListener1<Entity> attack = listenForAttack();
    player.setPosition(2f, 0f);
    hitNpc();

    task.update();

    verify(attack).handle(player);
    assertFalse(provoked.isAttackPending(), "the attack should be used up");
  }

  @Test
  void shouldWaitForThePlayerToComeCloserWhenOutOfRange() {
    EventListener1<Entity> attack = listenForAttack();
    player.setPosition(10f, 0f);
    hitNpc();

    task.update();

    verify(attack, never()).handle(any());
    assertTrue(provoked.isAttackPending(), "the attack should still be waiting");
  }

  @Test
  void shouldNotAttackWhenNotProvoked() {
    EventListener1<Entity> attack = listenForAttack();
    player.setPosition(2f, 0f);

    task.update();

    verify(attack, never()).handle(any());
  }

  @Test
  void shouldNotStartAnotherAttackWhileOneIsRunning() {
    EventListener1<Entity> attack = listenForAttack();
    player.setPosition(2f, 0f);
    hitNpc();
    provoked.setAttacking(true);

    task.update();

    verify(attack, never()).handle(any());
    assertTrue(provoked.isAttackPending(), "the waiting attack must not be used up");
  }

  @Test
  void shouldAttackOnlyOncePerHit() {
    EventListener1<Entity> attack = listenForAttack();
    player.setPosition(2f, 0f);
    hitNpc();

    task.update();
    task.update();

    verify(attack, times(1)).handle(player);
  }

  @Test
  void shouldLeaveFightModeBetweenAttacksWhenItDoesNotRepeat() {
    player.setPosition(10f, 0f);
    hitNpc();
    provoked.useAttack();

    assertEquals(-1, task.getPriority());
  }

  @Test
  void shouldStayInFightModeBetweenAttacksWhenItRepeats() {
    Entity repeater =
        new Entity()
            .addComponent(new CombatStatsComponent(50, 0))
            .addComponent(new ProvokedComponent(8f, 1.5f));
    repeater.create();
    repeater.setPosition(0f, 0f);
    RetaliateTask repeating = new RetaliateTask(player, 10, 4f, "headbutt");
    repeating.create(() -> repeater);
    player.setPosition(10f, 0f);
    repeater.getComponent(CombatStatsComponent.class).addHealth(-10);
    repeater.getComponent(ProvokedComponent.class).useAttack();

    assertEquals(10, repeating.getPriority());
  }

  @Test
  void shouldHaveNegativePriorityWithoutAPlayer() {
    RetaliateTask noPlayer = new RetaliateTask(null, 10, 4f, "headbutt");
    noPlayer.create(() -> npc);
    hitNpc();

    assertEquals(-1, noPlayer.getPriority());
  }

  @SuppressWarnings("unchecked")
  private EventListener1<Entity> listenForAttack() {
    EventListener1<Entity> listener = mock(EventListener1.class);
    npc.getEvents().addListener("headbutt", listener);
    return listener;
  }

  private void hitNpc() {
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
  }
}
