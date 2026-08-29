package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.BobbingTextureRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemFactoryTest {
  private ConsumableGenerator generator;

  @BeforeEach
  void setUp() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    generator = new ConsumableGenerator();
  }

  @Test
  void shouldCreateRenderableEntityForEveryConsumable() {
    for (ConsumableType type : ConsumableType.values()) {
      ConsumableItem item = generator.generateConsumable(type, 1);
      Entity dropped = ItemFactory.createConsumable(item);

      assertNotNull(
          dropped.getComponent(BobbingTextureRenderComponent.class),
          type + " should be drawn with a bobbing sprite");
    }
  }

  @Test
  void shouldSizeDroppedItemsSmallerThanThePlayer() {
    ConsumableItem item = generator.generateConsumable(ConsumableType.HEALTH_POTION, 1);

    Entity dropped = ItemFactory.createConsumable(item);

    assertEquals(0.6f, dropped.getScale().y, 0.001f);
    assertEquals(0.6f, dropped.getScale().x, 0.001f);
  }

  @Test
  void shouldRejectNullItem() {
    assertThrows(IllegalArgumentException.class, () -> ItemFactory.createConsumable(null));
  }
}
