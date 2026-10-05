package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerRenderComponentTest {
  @Test
  void shouldAddRemoveAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    TextureAtlas atlas2 = mock(TextureAtlas.class);
    TextureAtlas atlas3 = mock(TextureAtlas.class);
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);

    assertTrue(animator.addAnimation("test_name", 0.1f));
    assertTrue(animator.removeAnimation("test_name"));
    assertFalse(animator.removeAnimation("test_name"));
  }

  @Test
  void shouldFailRemoveInvalidAnimation() {
    TextureAtlas atlas1 = mock(TextureAtlas.class);
    TextureAtlas atlas2 = mock(TextureAtlas.class);
    TextureAtlas atlas3 = mock(TextureAtlas.class);
    when(atlas1.findRegions("test_name")).thenReturn(null);
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas1, atlas2, atlas3);

    assertFalse(animator.addAnimation("test_name", 0.1f));
    assertFalse(animator.removeAnimation("test_name"));
  }

  @Test
  void shouldFailDuplicateAddAnimation() {
    TextureAtlas atlas1 = createMockAtlas("test_name", 1);
    TextureAtlas atlas2 = mock(TextureAtlas.class);
    TextureAtlas atlas3 = mock(TextureAtlas.class);
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas1, atlas2, atlas3);

    assertTrue(animator.addAnimation("test_name", 0.1f));
    assertFalse(animator.addAnimation("test_name", 0.2f));
  }

  @Test
  void shouldHaveAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    TextureAtlas atlas2 = createMockAtlas("test2_name", 1);
    TextureAtlas atlas3 = createMockAtlas("test3_name", 1);
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);

    animator.addAnimation("test_name", 0.1f);
    animator.addAnimation("test2_name", 0.1f);
    animator.addAnimation("test3_name", 0.1f);
    assertTrue(animator.hasAnimation("test_name"));
    assertTrue(animator.hasAnimation("test2_name"));
    assertTrue(animator.hasAnimation("test3_name"));
    animator.removeAnimation("test_name");
    assertFalse(animator.hasAnimation("test_name"));
  }

  @Test
  void shouldPlayAnimation() {
    int numFrames = 5;
    String animName = "test_name";
    float frameTime = 1f;

    // Mock texture atlas
    TextureAtlas atlas = createMockAtlas(animName, numFrames);
    TextureAtlas atlas2 = createMockAtlas(animName, numFrames);
    TextureAtlas atlas3 = createMockAtlas(animName, numFrames);
    Array<TextureAtlas.AtlasRegion> regions = atlas.findRegions(animName);
    for (TextureAtlas.AtlasRegion region : regions) {
      when(region.getRegionWidth()).thenReturn(42);
      when(region.getRegionHeight()).thenReturn(42);
    }
    SpriteBatch batch = mock(SpriteBatch.class);

    // Mock game time
    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(frameTime);

    // Start animation
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);
    Entity entity = new Entity();
    animator.setEntity(entity);
    animator.addAnimation(animName, frameTime);
    animator.startAnimation(animName);

    for (int i = 0; i < 5; i++) {
      // Each draw advances 1 frame, check that it matches for each
      animator.draw(batch);
      verify(batch).draw(regions.get(i), entity.getPosition().x, entity.getPosition().y, 1f, 1f);
    }
  }

  @Test
  void shouldFinish() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    TextureAtlas atlas2 = createMockAtlas("test_name", 1);
    TextureAtlas atlas3 = createMockAtlas("test_name", 1);
    SpriteBatch batch = mock(SpriteBatch.class);

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(1f);

    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);
    Entity entity = new Entity();
    animator.setEntity(entity);
    animator.addAnimation("test_name", 1f);
    assertFalse(animator.isFinished());

    animator.startAnimation("test_name");
    assertFalse(animator.isFinished());

    animator.draw(batch);
    assertTrue(animator.isFinished());
  }

  @Test
  void shouldStopAnimation() {
    TextureAtlas atlas = createMockAtlas("test_name", 1);
    TextureAtlas atlas2 = createMockAtlas("test_name", 1);
    TextureAtlas atlas3 = createMockAtlas("test_name", 1);
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);
    animator.addAnimation("test_name", 1f);
    assertFalse(animator.stopAnimation());

    animator.startAnimation("test_name");
    assertTrue(animator.stopAnimation());
    assertNull(animator.getCurrentAnimation());
  }

  @Test
  void shouldNotDisposeSharedAtlas() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    TextureAtlas atlas2 = mock(TextureAtlas.class);
    TextureAtlas atlas3 = mock(TextureAtlas.class);
    ServiceLocator.registerRenderService(new RenderService());
    PlayerRenderComponent animator = new PlayerRenderComponent(atlas, atlas2, atlas3);

    animator.dispose();

    verify(atlas, never()).dispose();
  }

  static TextureAtlas createMockAtlas(String animationName, int numRegions) {
    TextureAtlas atlas = mock(TextureAtlas.class);
    Array<TextureAtlas.AtlasRegion> regions = new Array<>(numRegions);
    for (int i = 0; i < numRegions; i++) {
      regions.add(mock(TextureAtlas.AtlasRegion.class));
    }
    when(atlas.findRegions(animationName)).thenReturn(regions);
    return atlas;
  }
}
