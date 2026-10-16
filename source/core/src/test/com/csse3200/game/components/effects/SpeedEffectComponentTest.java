package com.csse3200.game.components.effects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SpeedEffectComponentTest {

  private Entity entity;
  private PlayerActions playerActions;
  private SpeedEffectComponent effect;
  private int frozenStarts;
  private int frozenEnds;

  @BeforeEach
  void setUp() {
    entity = new Entity();
    playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    entity.addComponent(playerActions);

    effect = new SpeedEffectComponent();
    entity.addComponent(effect);
    entity.create();

    frozenStarts = 0;
    frozenEnds = 0;
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_START_EVENT, () -> frozenStarts++);
    entity.getEvents().addListener(SpeedEffectComponent.FROZEN_END_EVENT, () -> frozenEnds++);
  }

  /** Runs the component's per-frame update the given number of times. */
  private void updateTimes(int ticks) {
    for (int i = 0; i < ticks; i++) {
      effect.update();
    }
  }

  @Test
  void shouldThrowOnNegativeTime() {
    assertThrows(IllegalArgumentException.class, () -> effect.applyEffect(-1, 0.5f));
  }

  @Test
  void shouldThrowOnNegativeMultiplier() {
    assertThrows(IllegalArgumentException.class, () -> effect.applyEffect(10, -0.1f));
  }

  @Test
  void shouldAllowZeroMultiplierForPause() {
    assertDoesNotThrow(() -> effect.applyEffect(10, 0f));
  }

  @Test
  void shouldAddModifierWhenEffectApplied() {
    effect.applyEffect(10, 0.5f);

    verify(playerActions).addSpeedModifier(any(), eq(0.5f));
  }

  @Test
  void shouldNeverRevertWhenTimeIsZero() {
    effect.applyEffect(0, 0.5f);

    verify(playerActions).addSpeedModifier(any(), eq(0.5f));

    // Time 0 is permanent: it is never tracked, so update() can't revert it
    effect.update();
    effect.update();
    verify(playerActions, never()).removeSpeedModifier(any());
  }

  @Test
  void shouldNotRevertBeforeTimerExpires() {
    effect.applyEffect(3, 0.5f);

    effect.update(); // 3 -> 2
    effect.update(); // 2 -> 1

    verify(playerActions, never()).removeSpeedModifier(any());
  }

  @Test
  void shouldRevertExactlyWhenTimerExpires() {
    ArgumentCaptor<Object> key = ArgumentCaptor.forClass(Object.class);
    effect.applyEffect(2, 0.5f);
    verify(playerActions).addSpeedModifier(key.capture(), eq(0.5f));

    effect.update(); // 2 -> 1
    verify(playerActions, never()).removeSpeedModifier(any());

    effect.update(); // 1 -> 0, reverts now
    verify(playerActions).removeSpeedModifier(key.getValue());
  }

  @Test
  void shouldNotRevertTwice() {
    effect.applyEffect(1, 0.5f);

    effect.update(); // expires and reverts
    effect.update(); // already removed from the list
    effect.update();

    verify(playerActions, times(1)).removeSpeedModifier(any());
  }

  @Test
  void shouldRemoveActiveModifiersOnDispose() {
    ArgumentCaptor<Object> key = ArgumentCaptor.forClass(Object.class);
    effect.applyEffect(50, 0.5f);
    verify(playerActions).addSpeedModifier(key.capture(), eq(0.5f));

    effect.dispose();

    verify(playerActions).removeSpeedModifier(key.getValue());
  }

  @Test
  void shouldSupportPauseSlowAndSpeedUpValues() {
    effect.applyEffect(5, 0f);
    effect.applyEffect(5, 0.5f);
    effect.applyEffect(5, 2f);

    verify(playerActions).addSpeedModifier(any(), eq(0f));
    verify(playerActions).addSpeedModifier(any(), eq(0.5f));
    verify(playerActions).addSpeedModifier(any(), eq(2f));
  }

  @Test
  void shouldStackEffectsWithIndependentKeysAndTimers() {
    ArgumentCaptor<Object> keys = ArgumentCaptor.forClass(Object.class);
    effect.applyEffect(1, 0.5f); // short
    effect.applyEffect(3, 0f); // long
    verify(playerActions, times(2)).addSpeedModifier(keys.capture(), anyFloat());

    Object shortKey = keys.getAllValues().get(0);
    Object longKey = keys.getAllValues().get(1);
    assertNotSame(shortKey, longKey);

    effect.update(); // short expires, long still going

    verify(playerActions).removeSpeedModifier(shortKey);
    verify(playerActions, never()).removeSpeedModifier(longKey);
  }

  @Test
  void shouldDoNothingWithoutPlayerActions() {
    Entity bare = new Entity();
    SpeedEffectComponent orphan = new SpeedEffectComponent();
    bare.addComponent(orphan);
    bare.create();

    assertDoesNotThrow(() -> orphan.applyEffect(5, 0.5f));
    assertDoesNotThrow(orphan::update);
    assertDoesNotThrow(orphan::dispose);
  }

  @Test
  void shouldApplyEffectWhenEventTriggered() {
    entity.getEvents().trigger("applySpeedEffect", 10, 0.5f);
    verify(playerActions).addSpeedModifier(any(), eq(0.5f));
  }

  // ---------- frozen reporting: getFrozenTicksRemaining(), frozenStart, frozenEnd ----------

  @Test
  void shouldReportNothingFrozenBeforeAnyEffect() {
    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(0, frozenStarts);
    assertEquals(0, frozenEnds);
  }

  @Test
  void shouldReportFrozenTimeAndFireStartOnceForASingleFreeze() {
    effect.applyEffect(180, 0f);

    assertEquals(180, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenStarts);
    assertEquals(0, frozenEnds);
  }

  @Test
  void shouldCountDownWithoutEndingEarly() {
    effect.applyEffect(3, 0f);

    updateTimes(2);

    assertEquals(1, effect.getFrozenTicksRemaining());
    assertEquals(0, frozenEnds);
  }

  @Test
  void shouldEndOnceAndRevertSpeedWhenTheFreezeRunsOut() {
    ArgumentCaptor<Object> key = ArgumentCaptor.forClass(Object.class);
    effect.applyEffect(3, 0f);
    verify(playerActions).addSpeedModifier(key.capture(), eq(0f));

    updateTimes(3);

    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenEnds);
    verify(playerActions).removeSpeedModifier(key.getValue());
  }

  @ParameterizedTest
  @ValueSource(floats = {0.5f, 2f})
  void shouldNotReportASlowOrSpeedUpAsFrozen(float multiplier) {
    effect.applyEffect(60, multiplier);

    assertEquals(0, effect.getFrozenTicksRemaining());
    updateTimes(60);
    assertEquals(0, frozenStarts);
    assertEquals(0, frozenEnds);
  }

  @Test
  void shouldCombineOverlappingFreezesIntoOneStartTheLongestTimeAndOneEnd() {
    effect.applyEffect(100, 0f);
    effect.applyEffect(40, 0f);

    assertEquals(100, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenStarts);

    updateTimes(99);
    assertEquals(0, frozenEnds);

    updateTimes(1);
    assertEquals(1, frozenEnds);
  }

  @Test
  void shouldReportTheLongerFreezeWhenTheSecondIsLonger() {
    effect.applyEffect(40, 0f);
    effect.applyEffect(100, 0f);

    assertEquals(100, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenStarts);
  }

  @Test
  void shouldEndTheFreezeEvenWhileASlowRemains() {
    effect.applyEffect(60, 0f);
    effect.applyEffect(120, 0.5f);

    updateTimes(60);

    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenEnds);
    verify(playerActions, times(1)).removeSpeedModifier(any()); // the slow is still active
  }

  @Test
  void shouldStartAgainForAFreezeAppliedAfterAnotherEnded() {
    effect.applyEffect(2, 0f);
    updateTimes(2);

    effect.applyEffect(2, 0f);

    assertEquals(2, frozenStarts);
    assertEquals(1, frozenEnds);
  }

  @Test
  void shouldNotShowAPermanentZeroAsFrozenButStillApplyIt() {
    effect.applyEffect(0, 0f);

    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(0, frozenStarts);
    assertEquals(0, frozenEnds);
    verify(playerActions).addSpeedModifier(any(), eq(0f));
  }

  @Test
  void shouldNotReportANanMultiplierAsFrozen() {
    effect.applyEffect(60, Float.NaN);

    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(0, frozenStarts);
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "60, -0.1"})
  void shouldRejectInvalidInputWithoutReportingOrApplyingAnything(int time, float multiplier) {
    assertThrows(IllegalArgumentException.class, () -> effect.applyEffect(time, multiplier));

    assertEquals(0, effect.getFrozenTicksRemaining());
    assertEquals(0, frozenStarts);
    verify(playerActions, never()).addSpeedModifier(any(), anyFloat());
  }

  @Test
  void shouldReportNothingWithoutPlayerActions() {
    Entity bare = new Entity();
    SpeedEffectComponent orphan = new SpeedEffectComponent();
    bare.addComponent(orphan);
    bare.create();
    int[] starts = {0};
    bare.getEvents().addListener(SpeedEffectComponent.FROZEN_START_EVENT, () -> starts[0]++);

    orphan.applyEffect(60, 0f);

    assertEquals(0, orphan.getFrozenTicksRemaining());
    assertEquals(0, starts[0]);
  }

  @Test
  void shouldEndOnceAndRemoveTheModifierWhenDisposedMidFreeze() {
    ArgumentCaptor<Object> key = ArgumentCaptor.forClass(Object.class);
    effect.applyEffect(100, 0f);
    verify(playerActions).addSpeedModifier(key.capture(), eq(0f));

    effect.dispose();
    effect.dispose(); // a second dispose must not end the freeze again

    assertEquals(1, frozenEnds);
    assertEquals(0, effect.getFrozenTicksRemaining());
    verify(playerActions).removeSpeedModifier(key.getValue());
  }

  @Test
  void shouldNotEndAnythingWhenDisposedWithoutAFreeze() {
    effect.dispose();

    assertEquals(0, frozenEnds);
  }

  @Test
  void shouldReportAFreezeStartedThroughTheEventName() {
    entity.getEvents().trigger("applySpeedEffect", 60, 0f);

    assertEquals(60, effect.getFrozenTicksRemaining());
    assertEquals(1, frozenStarts);
  }

  @Test
  void shouldHandleTheLargestWholeNumberDuration() {
    effect.applyEffect(Integer.MAX_VALUE, 0f);

    effect.update();

    assertEquals(Integer.MAX_VALUE - 1, effect.getFrozenTicksRemaining());
  }

  @Test
  void shouldStartAndEndOnTheFirstUpdateForAOneTickFreeze() {
    effect.applyEffect(1, 0f);
    assertEquals(1, frozenStarts);

    effect.update();

    assertEquals(1, frozenEnds);
    assertEquals(0, effect.getFrozenTicksRemaining());
  }
}
