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
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SpeedEffectComponentTest {

  private Entity entity;
  private PlayerActions playerActions;
  private SpeedEffectComponent effect;

  @BeforeEach
  void setUp() {
    entity = new Entity();
    playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    entity.addComponent(playerActions);

    effect = new SpeedEffectComponent();
    entity.addComponent(effect);
    entity.create();
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
}
