package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Mirrors {@link LightningFreezeComponentTest}'s structure - {@link PetrifyEffectComponent} is
 * mechanically identical to {@link LightningFreezeComponent} (an on-hit, timed, zero-speed stun
 * triggered via the target's "applySpeedEffect" event), just with no accompanying sound effect.
 */
@ExtendWith(GameExtension.class)
class PetrifyEffectComponentTest {
  private PlayerActions playerActions;
  private Entity target;
  private Entity gaze;

  @BeforeEach
  void setUp() {
    playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    target = new Entity().addComponent(playerActions).addComponent(new SpeedEffectComponent());
    target.create();

    gaze = new Entity().addComponent(new PetrifyEffectComponent(90));
    gaze.create();
  }

  @Test
  void shouldRejectNegativePetrifyTicks() {
    assertThrows(IllegalArgumentException.class, () -> new PetrifyEffectComponent(-1));
  }

  @Test
  void shouldPetrifyTargetOnProjectileHit() {
    gaze.getEvents().trigger("projectileHit", target);
    verify(playerActions).addSpeedModifier(any(), eq(0f));
  }

  @Test
  void shouldUnpetrifyAfterConfiguredTicks() {
    gaze.getEvents().trigger("projectileHit", target);
    SpeedEffectComponent speed = target.getComponent(SpeedEffectComponent.class);

    for (int i = 0; i < 89; i++) speed.update();
    verify(playerActions, never()).removeSpeedModifier(any());

    speed.update();
    verify(playerActions).removeSpeedModifier(any());
  }

  @Test
  void shouldIgnoreTargetWithoutSpeedEffectComponent() {
    Entity plain = new Entity();
    plain.create();
    assertDoesNotThrow(() -> gaze.getEvents().trigger("projectileHit", plain));
  }

  @Test
  void shouldNotThrowWhenTargetIsNull() {
    assertDoesNotThrow(() -> gaze.getEvents().trigger("projectileHit", (Entity) null));
  }
}
