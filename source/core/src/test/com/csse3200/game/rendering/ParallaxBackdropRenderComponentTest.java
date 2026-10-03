package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.areas.terrain.map.BackdropLayer;
import org.junit.jupiter.api.Test;

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

  private static float[] window(BackdropLayer layer, float viewLeft, float elapsed) {
    return ParallaxBackdropRenderComponent.textureWindow(
        layer, viewLeft, VIEW_WIDTH, VIEW_HEIGHT, SQUARE, elapsed);
  }
}
