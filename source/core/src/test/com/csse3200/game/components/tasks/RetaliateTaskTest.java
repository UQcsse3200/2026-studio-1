package com.csse3200.game.components.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.entities.Entity;
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

  private void hitNpc() {
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
  }
}
