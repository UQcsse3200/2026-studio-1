package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyWeaponAnimationComponentTest {

  @Test
  void shouldAddAndRemoveAnimation() {
    TextureAtlas atlas = createMockAtlas("sword_r", 3);
    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);

    assertTrue(animator.addAnimation("sword_r", 0.1f));
    assertTrue(animator.hasAnimation("sword_r"));
    assertTrue(animator.removeAnimation("sword_r"));
    assertFalse(animator.hasAnimation("sword_r"));
  }

  @Test
  void shouldFallbackToSingleRegionWhenRegionsEmpty() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    when(atlas.findRegions("default_r")).thenReturn(new Array<>());
    AtlasRegion singleRegion = mock(AtlasRegion.class);
    when(atlas.findRegion("default_r")).thenReturn(singleRegion);

    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    assertTrue(animator.addAnimation("default_r", 0.1f));
    assertTrue(animator.hasAnimation("default_r"));
  }

  @Test
  void shouldFailOnUnknownAnimation() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    when(atlas.findRegions("unknown")).thenReturn(null);
    when(atlas.findRegion("unknown")).thenReturn(null);

    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    assertFalse(animator.addAnimation("unknown", 0.1f));
  }

  @Test
  void shouldStartAndStopAnimation() {
    TextureAtlas atlas = createMockAtlas("bow_r", 2);
    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    animator.addAnimation("bow_r", 0.1f);

    animator.startAnimation("bow_r");
    assertEquals("bow_r", animator.getCurrentAnimation());

    assertTrue(animator.stopAnimation());
    assertNull(animator.getCurrentAnimation());
    assertFalse(animator.stopAnimation());
  }

  @Test
  void shouldCalculateHigherZIndexThanEntity() {
    TextureAtlas atlas = mock(TextureAtlas.class);
    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    Entity entity = new Entity();
    entity.setPosition(new Vector2(5f, 10f));
    animator.setEntity(entity);

    // Entity body z-index is -10.0f; weapon should be -10.0f + 1 = -9.0f
    assertEquals(-10f + 1f, animator.getZIndex(), 0.0001f);
  }

  @Test
  void shouldDrawWithScaleAndOffset() {
    TextureAtlas atlas = createMockAtlas("sword_r", 1);
    Array<AtlasRegion> regions = atlas.findRegions("sword_r");
    SpriteBatch batch = mock(SpriteBatch.class);

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(0.016f);

    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    Entity entity = new Entity();
    entity.setPosition(new Vector2(10f, 5f));
    entity.setScale(new Vector2(1.5f, 1.5f));
    animator.setEntity(entity);
    animator.addAnimation("sword_r", 0.1f);
    animator.startAnimation("sword_r");

    animator.draw(batch);

    // Expected width/height: 1.5 * 1.5 = 2.25
    // Expected x: 10 - (1.5 * 0.25) = 9.625, y: 5 - (1.5 * 0.25) - 0.15 = 4.475
    verify(batch).draw(regions.get(0), 9.625f, 4.475f, 2.25f, 2.25f);
  }

  @Test
  void shouldSupportCustomOffsets() {
    TextureAtlas atlas = createMockAtlas("sword_r", 1);
    Array<AtlasRegion> regions = atlas.findRegions("sword_r");
    SpriteBatch batch = mock(SpriteBatch.class);

    GameTime gameTime = mock(GameTime.class);
    ServiceLocator.registerTimeSource(gameTime);
    when(gameTime.getDeltaTime()).thenReturn(0.016f);

    EnemyWeaponAnimationComponent animator = new EnemyWeaponAnimationComponent(atlas);
    animator.setOffset(0.2f, -0.3f);
    assertEquals(0.2f, animator.getOffsetX());
    assertEquals(-0.3f, animator.getOffsetY());

    Entity entity = new Entity();
    entity.setPosition(new Vector2(10f, 5f));
    entity.setScale(new Vector2(1.5f, 1.5f));
    animator.setEntity(entity);
    animator.addAnimation("sword_r", 0.1f);
    animator.startAnimation("sword_r");

    animator.draw(batch);

    // Expected x: 10 - (1.5 * 0.25) + 0.2 = 9.825, y: 5 - (1.5 * 0.25) - 0.3 = 4.325
    verify(batch).draw(regions.get(0), 9.825f, 4.325f, 2.25f, 2.25f);
  }

  static TextureAtlas createMockAtlas(String animationName, int numRegions) {
    TextureAtlas atlas = mock(TextureAtlas.class);
    Array<AtlasRegion> regions = new Array<>(numRegions);
    for (int i = 0; i < numRegions; i++) {
      regions.add(mock(AtlasRegion.class));
    }
    when(atlas.findRegions(animationName)).thenReturn(regions);
    return atlas;
  }
}
