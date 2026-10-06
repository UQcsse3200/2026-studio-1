package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ArrowFactoryLightningTest {
  @BeforeEach
  void setUp() {
    Texture texture = mock(Texture.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(time);
  }

  private Entity bolt() {
    return ArrowFactory.createLightning(new Vector2(10f, 3f), 8f, 12f, 0, 120, PhysicsLayer.PLAYER);
  }

  @Test
  void shouldSpawnAboveAndCentredOnTarget() {
    Entity bolt = bolt();
    assertEquals(10f - bolt.getScale().x / 2f, bolt.getPosition().x, 0.001f);
    assertEquals(11f, bolt.getPosition().y, 0.001f);
  }

  @Test
  void shouldUseTheLightningSpriteProportions() {
    Vector2 scale = bolt().getScale();
    assertEquals(0.3125f, scale.x, 0.0001f);
    assertEquals(3.125f, scale.y, 0.0001f);
  }
}
