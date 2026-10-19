package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.csse3200.game.areas.terrain.map.BackdropLayer;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ParallaxBackdropRenderComponentTest {
  private static final float VIEW_WIDTH = 20f;
  private static final float VIEW_HEIGHT = 10f;
  private static final float SQUARE = 1f;
  private static final float TOLERANCE = 1e-4f;

  @Test
  void aSquareImageRepeatsAcrossAWiderView() {
    BackdropLayer layer = new BackdropLayer("far.png", 0f, 0f, 0f);

    float[] window = window(layer, 7f, 0f);

    assertArrayEquals(new float[] {0f, 1f, 2f, 0f}, window, TOLERANCE);
  }

  @Test
  void aLayerSlidesByItsShareOfTheCameraMovement() {
    BackdropLayer layer = new BackdropLayer("pillars.png", 0.5f, 0f, 0f);

    // The camera is 4 units along, so the art moves 2 of the image's 10 units.
    float[] window = window(layer, 4f, 0f);

    assertArrayEquals(new float[] {0.2f, 1f, 2.2f, 0f}, window, TOLERANCE);
  }

  @Test
  void driftMovesTheArtRightAndUpOverTime() {
    BackdropLayer layer = new BackdropLayer("embers.png", 0f, 1f, 2f);

    // After 3 seconds: 3 units right of 10 wide, 6 units up of 10 tall.
    float[] window = window(layer, 0f, 3f);

    assertArrayEquals(new float[] {0.7f, 1.6f, 2.7f, 0.6f}, window, TOLERANCE);
  }

  @Test
  void offsetsWrapSoALongRunningLevelStaysPrecise() {
    BackdropLayer layer = new BackdropLayer("fog.png", 1f, 0f, 0f);

    float[] window = window(layer, 1234f, 0f);

    assertArrayEquals(new float[] {0.4f, 1f, 2.4f, 0f}, window, 1e-3f);
  }

  @Test
  void aSpanningLayerShowsItsBottomAtTheBottomOfTheMapAndItsTopAtTheTop() {
    float mapHeight = 90f;
    float layerHeight = 30f;

    assertEquals(
        0f,
        ParallaxBackdropRenderComponent.spanningLayerBottom(
            0f, VIEW_HEIGHT, layerHeight, mapHeight),
        TOLERANCE);
    // With the view at the very top (bottom edge at 80), the layer's top edge meets the map's.
    assertEquals(
        mapHeight - layerHeight,
        ParallaxBackdropRenderComponent.spanningLayerBottom(
            80f, VIEW_HEIGHT, layerHeight, mapHeight),
        TOLERANCE);
  }

  @Test
  void aSpanningLayerStaysPutWhenTheMapIsNoTallerThanTheView() {
    assertEquals(
        0f,
        ParallaxBackdropRenderComponent.spanningLayerBottom(3f, VIEW_HEIGHT, 30f, VIEW_HEIGHT),
        TOLERANCE);
  }

  @Test
  void createMakesOnlyViewFillingLayersRepeat() {
    Map<String, Texture> textures = registerTextures("far.png", "tall.png", "flash.png");
    LevelMapData map =
        mapWithBackdrops(
            Map.of(
                LevelMapData.WHOLE_MAP_BACKDROP,
                List.of(plain("far.png"), spanning("tall.png"), flickering("flash.png"))),
            Map.of());

    attach(new ParallaxBackdropRenderComponent(map));

    verify(textures.get("far.png")).setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
    verify(textures.get("tall.png"), never()).setWrap(any(), any());
    verify(textures.get("flash.png"), never()).setWrap(any(), any());
  }

  @Test
  void drawsTheBackdropOfTheSubLevelTheCameraIsIn() {
    Map<String, Texture> textures =
        registerTextures("far.png", "tall.png", "hidden.png", "nether.png");
    BackdropLayer hidden =
        new BackdropLayer("hidden.png", 0f, 0f, 0f, false, new BackdropLayer.Rows(25, 28));
    LevelMapData map =
        mapWithBackdrops(
            Map.of(
                "dungeon",
                List.of(plain("far.png"), spanning("tall.png"), hidden),
                "nether",
                List.of(plain("nether.png"))),
            Map.of());
    ParallaxBackdropRenderComponent backdrop = attach(new ParallaxBackdropRenderComponent(map));
    SpriteBatch batch = batchViewing(0f, 0f);

    backdrop.draw(batch);

    // The 2:1 image scaled to the 10-tall view is 20 wide, exactly filling it once.
    verify(batch).draw(textures.get("far.png"), 0f, 0f, 20f, 10f, 0f, 1f, 1f, 0f);
    // Spanning the 40-wide map, the 2:1 image is 20 tall and starts at the map's bottom.
    verify(batch).draw(textures.get("tall.png"), 0f, 0f, 40f, 20f);
    verify(batch, never())
        .draw(eq(textures.get("hidden.png")), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    verifyRegionDraws(batch, textures.get("hidden.png"), 0);
    verifyRegionDraws(batch, textures.get("nether.png"), 0);
    // Once for each of the two visible layers, then reset afterwards.
    verify(batch, times(3)).setColor(1f, 1f, 1f, 1f);
    verify(batch).flush();
    assertEquals(-2, backdrop.getLayer());
    assertEquals(0f, backdrop.getZIndex());
  }

  @Test
  void overlaysPlayAFlickerThenWaitForTheNextOne() {
    Map<String, Texture> textures = registerTextures("flash.png");
    GameTime time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    LevelMapData map =
        mapWithBackdrops(
            Map.of(), Map.of(LevelMapData.WHOLE_MAP_BACKDROP, List.of(flickering("flash.png"))));
    ParallaxBackdropRenderComponent overlay =
        attach(new ParallaxBackdropRenderComponent(map, true));
    // Above both sub-levels, so the whole-map overlay applies.
    SpriteBatch batch = batchViewing(0f, 40f);

    overlay.draw(batch);

    // Frame 0 of 2, half the view tall, centred across the top of the view.
    verify(batch).draw(textures.get("flash.png"), 7.5f, 45f, 5f, 5f, 0f, 1f, 0.5f, 0f);

    when(time.getDeltaTime()).thenReturn(1f);
    overlay.update();
    overlay.draw(batch);

    // The play has finished, so nothing new is drawn until the next one starts.
    verifyRegionDraws(batch, textures.get("flash.png"), 1);
    assertEquals(1, overlay.getLayer());
    assertEquals(Float.MAX_VALUE, overlay.getZIndex());
  }

  private static void verifyRegionDraws(SpriteBatch batch, Texture texture, int count) {
    verify(batch, times(count))
        .draw(
            eq(texture),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat());
  }

  private static BackdropLayer plain(String texture) {
    return new BackdropLayer(texture, 0f, 0f, 0f);
  }

  private static BackdropLayer spanning(String texture) {
    return new BackdropLayer(texture, 0f, 0f, 0f, true);
  }

  private static BackdropLayer flickering(String texture) {
    return new BackdropLayer(
        texture, 0f, 0f, 0f, false, null, 1f, new BackdropLayer.Flicker(2, 10f, 0f, 0f, 0.5f));
  }

  /** A 40x30 map of 1-unit tiles: a dungeon in rows 0-14 and the Nether in rows 15-29. */
  private static LevelMapData mapWithBackdrops(
      Map<String, List<BackdropLayer>> backdrops, Map<String, List<BackdropLayer>> overlays) {
    return LevelMapData.builder("test")
        .tileSize(1f)
        .size(40, 30)
        .subLevels(
            List.of(
                new SubLevel("dungeon", null, new SubLevel.Bounds(0, 0, 40, 15), null, null, null),
                new SubLevel("nether", null, new SubLevel.Bounds(0, 15, 40, 15), null, null, null)))
        .backdrops(backdrops)
        .overlays(overlays)
        .build();
  }

  private static Map<String, Texture> registerTextures(String... paths) {
    ResourceService resources = mock(ResourceService.class);
    Map<String, Texture> textures = new HashMap<>();
    for (String path : paths) {
      Texture texture = mock(Texture.class);
      when(texture.getWidth()).thenReturn(64);
      when(texture.getHeight()).thenReturn(32);
      when(resources.getAsset(eq(path), any())).thenReturn(texture);
      textures.put(path, texture);
    }
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    return textures;
  }

  private static ParallaxBackdropRenderComponent attach(ParallaxBackdropRenderComponent component) {
    new Entity().addComponent(component).create();
    return component;
  }

  /** A batch whose camera shows the 20x10 rectangle with its bottom-left corner at (x, y). */
  private static SpriteBatch batchViewing(float x, float y) {
    SpriteBatch batch = mock(SpriteBatch.class);
    when(batch.getProjectionMatrix())
        .thenReturn(new Matrix4().setToOrtho2D(x, y, VIEW_WIDTH, VIEW_HEIGHT));
    return batch;
  }

  private static float[] window(BackdropLayer layer, float viewLeft, float elapsed) {
    return ParallaxBackdropRenderComponent.textureWindow(
        layer, viewLeft, VIEW_WIDTH, VIEW_HEIGHT, SQUARE, elapsed);
  }
}
