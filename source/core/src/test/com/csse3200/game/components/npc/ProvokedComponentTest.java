package com.csse3200.game.components.npc;

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
class ProvokedComponentTest {
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0f);
    ServiceLocator.registerTimeSource(time);
  }

  @Test
  void shouldBeProvokedWithAttackPendingAfterHealthDrops() {
    Entity npc = makeNpc(new ProvokedComponent(8f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);

    npc.getComponent(CombatStatsComponent.class).addHealth(-10);

    assertTrue(provoked.isProvoked());
    assertTrue(provoked.isAttackPending());
  }

  @Test
  void shouldNotBeProvokedByHealing() {
    Entity npc = makeNpc(new ProvokedComponent(8f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);

    npc.getComponent(CombatStatsComponent.class).addHealth(10);

    assertFalse(provoked.isProvoked());
    assertFalse(provoked.isAttackPending());
  }

  @Test
  void shouldNotAddSecondAttackInSameFight() {
    Entity npc = makeNpc(new ProvokedComponent(8f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    CombatStatsComponent stats = npc.getComponent(CombatStatsComponent.class);

    stats.addHealth(-10);
    provoked.useAttack();
    stats.addHealth(-10);

    assertTrue(provoked.isProvoked());
    assertFalse(provoked.isAttackPending());
  }

  @Test
  void shouldCalmDownAndCancelWaitingAttack() {
    Entity npc = makeNpc(new ProvokedComponent(8f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);

    when(time.getDeltaTime()).thenReturn(8.1f);
    npc.update();

    assertFalse(provoked.isProvoked());
    assertFalse(provoked.isAttackPending());
  }

  @Test
  void shouldReadyNewAttackAfterRepeatCooldown() {
    Entity npc = makeNpc(new ProvokedComponent(8f, 1.5f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
    provoked.useAttack();

    when(time.getDeltaTime()).thenReturn(1.0f);
    npc.update();
    assertFalse(provoked.isAttackPending(), "the next attack should not be ready mid-break");

    when(time.getDeltaTime()).thenReturn(0.6f);
    npc.update();
    assertTrue(provoked.isAttackPending(), "the next attack should be ready after the break");
  }

  @Test
  void shouldNotBeProvokedWithoutCombatStats() {
    ProvokedComponent provoked = new ProvokedComponent(8f);
    Entity npc = new Entity().addComponent(provoked);
    npc.create();

    when(time.getDeltaTime()).thenReturn(1f);
    npc.update();

    assertFalse(provoked.isProvoked());
    assertFalse(provoked.isAttackPending());
  }

  @Test
  void shouldStopHoldingFightWhenTimerRunsOut() {
    Entity npc = makeNpc(new ProvokedComponent(8f, 1.5f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
    provoked.useAttack();
    assertTrue(provoked.isHoldingFight(), "a repeating NPC should hold the fight after attacking");

    when(time.getDeltaTime()).thenReturn(8.1f);
    npc.update();

    assertFalse(provoked.isHoldingFight());
    assertFalse(provoked.isProvoked());
  }

  @Test
  void shouldNotCountRepeatBreakWhileAttacking() {
    Entity npc = makeNpc(new ProvokedComponent(8f, 1.5f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
    provoked.useAttack();
    provoked.setAttacking(true);

    when(time.getDeltaTime()).thenReturn(2f);
    npc.update();
    assertFalse(provoked.isAttackPending(), "the break should not count down mid-attack");

    provoked.setAttacking(false);
    npc.update();
    assertTrue(provoked.isAttackPending(), "the break should count down once the attack ends");
  }

  @Test
  void shouldCancelRepeatBreakOnCalmDown() {
    Entity npc = makeNpc(new ProvokedComponent(8f, 1.5f));
    ProvokedComponent provoked = npc.getComponent(ProvokedComponent.class);
    npc.getComponent(CombatStatsComponent.class).addHealth(-10);
    provoked.useAttack();

    when(time.getDeltaTime()).thenReturn(8.1f);
    npc.update();
    when(time.getDeltaTime()).thenReturn(1f);
    npc.update();

    assertFalse(provoked.isAttackPending(), "a calmed-down NPC should not ready another attack");
  }

  private Entity makeNpc(ProvokedComponent provoked) {
    Entity npc = new Entity().addComponent(new CombatStatsComponent(50, 0)).addComponent(provoked);
    npc.create();
    return npc;
  }
}
