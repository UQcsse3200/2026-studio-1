package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PoisonEffectComponentTest {
  private GameTime time;
  private CombatStatsComponent targetStats;
  private Entity poisonEntity;
  private PoisonEffectComponent poison;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0f);
    ServiceLocator.registerTimeSource(time);

    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 0));
    target.create();
    targetStats = target.getComponent(CombatStatsComponent.class);

    poison = new PoisonEffectComponent(target, 5, 5, 2f);
    poisonEntity = new Entity().addComponent(poison);
    poisonEntity.create();
  }

  @Test
  void shouldDealDamageEachTickForTotalTicks() {
    when(time.getDeltaTime()).thenReturn(2f);

    for (int tick = 1; tick <= 5; tick++) {
      poisonEntity.update();
      assertEquals(100 - 5 * tick, targetStats.getHealth(), "tick " + tick + " should deal 5");
    }

    poisonEntity.update();
    poisonEntity.update();
    assertEquals(75, targetStats.getHealth(), "no damage should be dealt after the last tick");
    assertFalse(poison.isRunning());
  }

  @Test
  void shouldNotDamageBeforeFirstTick() {
    when(time.getDeltaTime()).thenReturn(1.9f);

    poisonEntity.update();

    assertEquals(100, targetStats.getHealth());
  }

  @Test
  void shouldRestartToFullTicks() {
    when(time.getDeltaTime()).thenReturn(2f);
    for (int i = 0; i < 3; i++) {
      poisonEntity.update();
    }
    assertEquals(85, targetStats.getHealth());

    poison.restart();
    for (int i = 0; i < 7; i++) {
      poisonEntity.update();
    }

    assertEquals(60, targetStats.getHealth(), "a restart should apply all 5 ticks again");
  }

  @Test
  void shouldNotDamageWhilePaused() {
    when(time.getDeltaTime()).thenReturn(0f);

    for (int i = 0; i < 100; i++) {
      poisonEntity.update();
    }

    assertEquals(100, targetStats.getHealth());
    assertTrue(poison.isRunning());
  }
}
