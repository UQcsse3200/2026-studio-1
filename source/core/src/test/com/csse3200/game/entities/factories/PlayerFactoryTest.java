package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.input.InputFactory;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@ExtendWith(GameExtension.class)
class PlayerFactoryTest {

  @BeforeEach
  void setUp() {
    InputFactory inputFactory = Mockito.mock(InputFactory.class);
    Mockito.when(inputFactory.createForPlayer()).thenReturn(Mockito.mock(InputComponent.class));
    InputService inputService = Mockito.mock(InputService.class);
    Mockito.when(inputService.getInputFactory()).thenReturn(inputFactory);

    ServiceLocator.registerInputService(inputService);
    // The factory scales the player sprite, so the resource service has to hand back a texture.
    Texture texture = Mockito.mock(Texture.class);
    Mockito.when(texture.getWidth()).thenReturn(16);
    Mockito.when(texture.getHeight()).thenReturn(16);
    ResourceService resourceService = Mockito.mock(ResourceService.class);
    Mockito.when(
            resourceService.getAsset(
                ArgumentMatchers.anyString(), ArgumentMatchers.eq(Texture.class)))
        .thenReturn(texture);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(Mockito.mock(RenderService.class));
    ServiceLocator.registerTimeSource(Mockito.mock(GameTime.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  /**
   * A potion only works if the player carries the component that applies it. Without these the buff
   * and regeneration potions silently do nothing in a running game, even though every unit test of
   * the effects passes.
   */
  @Test
  void shouldGiveThePlayerEveryComponentPotionsNeed() {
    Entity player = PlayerFactory.createPlayer();

    assertNotNull(
        player.getComponent(ConsumableUseComponent.class),
        "player should be able to use consumables");
    assertNotNull(
        player.getComponent(PlayerBuffComponent.class),
        "player should be able to receive buff potions");
    assertNotNull(
        player.getComponent(PlayerRegenComponent.class),
        "player should be able to receive regeneration potions");
  }
}
