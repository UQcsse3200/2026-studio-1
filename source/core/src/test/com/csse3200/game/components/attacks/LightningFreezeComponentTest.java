package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LightningFreezeComponentTest {
  private PlayerActions playerActions;
  private Entity target;
  private Entity bolt;
  private Sound zapSound;

  @BeforeEach
  void setUp() {
    zapSound = mock(Sound.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Sound.class))).thenReturn(zapSound);
    ServiceLocator.registerResourceService(resources);

    playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    target = new Entity().addComponent(playerActions).addComponent(new SpeedEffectComponent());
    target.create();

    bolt = new Entity().addComponent(new LightningFreezeComponent(120));
    bolt.create();
  }

  @Test
  void shouldFreezeTargetOnProjectileHit() {
    bolt.getEvents().trigger("projectileHit", target);
    verify(playerActions).addSpeedModifier(any(), eq(0f));
  }

  @Test
  void shouldUnfreezeAfterConfiguredTicks() {
    bolt.getEvents().trigger("projectileHit", target);
    SpeedEffectComponent speed = target.getComponent(SpeedEffectComponent.class);

    for (int i = 0; i < 119; i++) speed.update();
    verify(playerActions, never()).removeSpeedModifier(any());

    speed.update();
    verify(playerActions).removeSpeedModifier(any());
  }

  @Test
  void shouldIgnoreTargetWithoutSpeedEffectComponent() {
    Entity plain = new Entity();
    plain.create();
    assertDoesNotThrow(() -> bolt.getEvents().trigger("projectileHit", plain));
  }

  @Test
  void shouldPlayZapSoundOnHit() {
    bolt.getEvents().trigger("projectileHit", target);
    verify(zapSound).play(anyFloat());
  }
}
