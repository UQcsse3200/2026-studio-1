package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class SpeedEffectComponentTest {

  private Entity entity;
  private PlayerActions playerActions;

  @BeforeEach
  void setUp() {
    entity = new Entity();
    playerActions = mock(PlayerActions.class);
    entity.addComponent(playerActions);
  }

  @Test
  void shouldThrowOnNegativeTime() {
    assertThrows(IllegalArgumentException.class, () -> new SpeedEffectComponent(-1, 0.5f));
  }

  @Test
  void shouldThrowOnNegativeMultiplier() {
    assertThrows(IllegalArgumentException.class, () -> new SpeedEffectComponent(10, -0.1f));
  }

  @Test
  void shouldAllowZeroMultiplierForPause() {
    assertDoesNotThrow(() -> new SpeedEffectComponent(10, 0f));
  }

  @Test
  void shouldAddModifierOnCreate() {
    SpeedEffectComponent effect = new SpeedEffectComponent(10, 0.5f);
    entity.addComponent(effect);
    entity.create();

    verify(playerActions).addSpeedModifier(effect, 0.5f);
  }

  @Test
  void shouldDisableImmediatelyWhenTimeIsZero() {
    SpeedEffectComponent effect = new SpeedEffectComponent(0, 0.5f);
    entity.addComponent(effect);
    entity.create();

    verify(playerActions).addSpeedModifier(effect, 0.5f);
    // Instant effect never reverts, and disables itself so update() won't run again
    verify(playerActions, never()).removeSpeedModifier(any());
  }

  @Test
  void shouldNotRevertBeforeTimerExpires() {
    SpeedEffectComponent effect = new SpeedEffectComponent(3, 0.5f);
    entity.addComponent(effect);
    entity.create();

    effect.update(); // tick 1 -> 2 remaining
    effect.update(); // tick 2 -> 1 remaining

    verify(playerActions, never()).removeSpeedModifier(any());
  }

  @Test
  void shouldRevertExactlyWhenTimerExpires() {
    SpeedEffectComponent effect = new SpeedEffectComponent(2, 0.5f);
    entity.addComponent(effect);
    entity.create();

    effect.update(); // tick 1 -> 1 remaining
    verify(playerActions, never()).removeSpeedModifier(any());

    effect.update(); // tick 2 -> 0 remaining, should revert now
    verify(playerActions).removeSpeedModifier(effect);
  }

  @Test
  void shouldRemoveModifierOnDispose() {
    SpeedEffectComponent effect = new SpeedEffectComponent(50, 0.5f);
    entity.addComponent(effect);
    entity.create();

    effect.dispose();

    verify(playerActions).removeSpeedModifier(effect);
  }

  @Test
  void shouldSupportPauseSlowAndSpeedUpValues() {
    SpeedEffectComponent pause = new SpeedEffectComponent(5, 0f);
    SpeedEffectComponent slow = new SpeedEffectComponent(5, 0.5f);
    SpeedEffectComponent speedUp = new SpeedEffectComponent(5, 2f);

    entity.addComponent(pause);
    pause.create();
    verify(playerActions).addSpeedModifier(pause, 0f);

    Entity entity2 = new Entity();
    PlayerActions pa2 = mock(PlayerActions.class);
    entity2.addComponent(pa2);
    entity2.addComponent(slow);
    slow.create();
    verify(pa2).addSpeedModifier(slow, 0.5f);

    Entity entity3 = new Entity();
    PlayerActions pa3 = mock(PlayerActions.class);
    entity3.addComponent(pa3);
    entity3.addComponent(speedUp);
    speedUp.create();
    verify(pa3).addSpeedModifier(speedUp, 2f);
  }
}
