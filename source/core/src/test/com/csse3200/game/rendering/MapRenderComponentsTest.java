package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Covers the small render components that draw map tiles: backgrounds, animations and glows. */
@ExtendWith(GameExtension.class)
class MapRenderComponentsTest {
  private Texture texture;
  private GameTime time;

  @BeforeEach
  void setUp() {
    texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(64);
    when(texture.getHeight()).thenReturn(32);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), any())).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
  }

  @Test
  void mapBackgroundSitsBehindEverything() {
    MapBackgroundRenderComponent background = new MapBackgroundRenderComponent("bg.png");

    assertEquals(-1, background.getLayer());
    assertEquals(0f, background.getZIndex());
  }

  @Test
  void sheetAnimationLoopsThroughFrames() {
    assertEquals(0, SheetAnimationRenderComponent.frameAt(0f, 4, 8f));
    assertEquals(2, SheetAnimationRenderComponent.frameAt(0.3f, 4, 8f));
    assertEquals(1, SheetAnimationRenderComponent.frameAt(0.625f, 4, 8f), "wraps after 4");
  }

  @Test
  void sheetAnimationDrawsTheCurrentFrameOverTheEntity() {
    SheetAnimationRenderComponent animation =
        new SheetAnimationRenderComponent("flame.png", 4, 8f, 0f);
    Entity entity = new Entity().addComponent(animation);
    entity.setPosition(2f, 3f);
    entity.setScale(1f, 0.5f);
    entity.create();
    SpriteBatch batch = mock(SpriteBatch.class);

    when(time.getDeltaTime()).thenReturn(0.25f);
    animation.update();
    animation.draw(batch);

    // 0.25s at 8fps is frame 2 of 4, the strip's third quarter.
    verify(batch).draw(texture, 2f, 3f, 1f, 0.5f, 0.5f, 1f, 0.75f, 0f);
    assertEquals(0, animation.getLayer());
    assertEquals(0.5f, animation.getZIndex());
  }

  @Test
  void sheetAnimationTreatsZeroFramesAsOne() {
    SheetAnimationRenderComponent animation =
        new SheetAnimationRenderComponent("still.png", 0, 8f, 1f);
    Entity entity = new Entity().addComponent(animation);
    entity.create();
    SpriteBatch batch = mock(SpriteBatch.class);

    animation.draw(batch);

    verify(batch).draw(texture, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f);
  }

  @Test
  void glowDrawsAdditivelyAroundTheEntityAndRestoresTheBatch() {
    GlowRenderComponent glow = new GlowRenderComponent("glow.png", 2f, 0f);
    Entity entity = new Entity().addComponent(glow);
    entity.setPosition(5f, 5f);
    entity.create();
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getBlendSrcFunc()).thenReturn(GL20.GL_SRC_ALPHA);
    when(batch.getBlendDstFunc()).thenReturn(GL20.GL_ONE_MINUS_SRC_ALPHA);

    when(time.getDeltaTime()).thenReturn(0.5f);
    glow.update();
    glow.draw(batch);

    verify(batch).setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    verify(batch).setColor(1f, 1f, 1f, GlowRenderComponent.alphaAt(0.5f, 0f));
    verify(batch).draw(texture, 4f, 4f, 2f, 2f);
    verify(batch).setColor(1f, 1f, 1f, 1f);
    verify(batch).setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    assertEquals(0, glow.getLayer());
    assertEquals(1f, glow.getZIndex());
  }

  @Test
  void glowLoadsItsTextureOnCreate() {
    ResourceService resources = ServiceLocator.getResourceService();
    new Entity().addComponent(new GlowRenderComponent("light.png", 1f, 0f)).create();

    verify(resources).getAsset("light.png", Texture.class);
  }
}
