package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.csse3200.game.areas.terrain.map.BackdropLayer;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a map's parallax backdrops behind its terrain, or its overlays in front of everything in
 * the world: for whichever sub-level the camera is in, that sub-level's stack of images, each
 * moving at its own rate as the camera does.
 */
public class ParallaxBackdropRenderComponent extends RenderComponent {
  private final LevelMapData mapData;
  private final boolean inFront;
  private float elapsed;

  /** For each flickering layer: when it next plays, and where in the view it is placed. */
  private final Map<BackdropLayer, float[]> flickers = new HashMap<>();

  /** Draws the map's backdrops, behind its terrain. */
  public ParallaxBackdropRenderComponent(LevelMapData mapData) {
    this(mapData, false);
  }

  /**
   * @param mapData the map whose layers are drawn
   * @param inFront true to draw the map's overlays in front of the world, false to draw its
   *     backdrops behind the terrain
   */
  public ParallaxBackdropRenderComponent(LevelMapData mapData, boolean inFront) {
    this.mapData = mapData;
    this.inFront = inFront;
  }

  @Override
  public void create() {
    super.create();
    for (String texturePath : repeatingTextures()) {
      texture(texturePath).setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }
  }

  @Override
  public void update() {
    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The batch is already set to the camera's orthographic projection, which scales by 2 / size
    // and translates by the camera centre, so the visible rectangle can be read back from it.
    float[] projection = batch.getProjectionMatrix().val;
    float viewWidth = 2f / projection[Matrix4.M00];
    float viewHeight = 2f / projection[Matrix4.M11];
    float centreX = -projection[Matrix4.M03] / projection[Matrix4.M00];
    float centreY = -projection[Matrix4.M13] / projection[Matrix4.M11];
    float viewLeft = centreX - viewWidth / 2f;
    float viewBottom = centreY - viewHeight / 2f;

    SubLevel subLevel = mapData.getSubLevelAt((int) Math.floor(centreY / mapData.getTileSize()));
    float mapWidth = mapData.getWidth() * mapData.getTileSize();
    float mapHeight = mapData.getHeight() * mapData.getTileSize();

    String subLevelId = subLevel == null ? null : subLevel.id();
    List<BackdropLayer> layers =
        inFront ? mapData.getOverlay(subLevelId) : mapData.getBackdrop(subLevelId);

    for (BackdropLayer layer : layers) {
      float visibility = layer.visibilityAt(centreY / mapData.getTileSize());
      if (visibility <= 0f) {
        continue;
      }
      batch.setColor(1f, 1f, 1f, visibility);
      Texture texture = texture(layer.texture());
      if (layer.flicker() != null) {
        drawFlicker(batch, layer, texture, viewLeft, viewBottom, viewWidth, viewHeight);
        continue;
      }
      if (layer.spansMap()) {
        float layerHeight = mapWidth * texture.getHeight() / texture.getWidth();
        float layerBottom = spanningLayerBottom(viewBottom, viewHeight, layerHeight, mapHeight);
        batch.draw(texture, 0f, layerBottom, mapWidth, layerHeight);
        continue;
      }
      float[] window =
          textureWindow(
              layer,
              viewLeft,
              viewWidth,
              viewHeight,
              (float) texture.getWidth() / texture.getHeight(),
              elapsed);
      batch.draw(
          texture,
          viewLeft,
          viewBottom,
          viewWidth,
          viewHeight,
          window[0],
          window[1],
          window[2],
          window[3]);
    }
    batch.setColor(1f, 1f, 1f, 1f);
    // The terrain draws through its own batch, so anything still queued here would land on top.
    batch.flush();
  }

  /**
   * Draws a flickering layer's current frame, if it is mid-play, and schedules its next play
   * somewhere new in the upper part of the view once it has finished.
   */
  private void drawFlicker(
      SpriteBatch batch,
      BackdropLayer layer,
      Texture texture,
      float viewLeft,
      float viewBottom,
      float viewWidth,
      float viewHeight) {
    BackdropLayer.Flicker flicker = layer.flicker();
    // {time the play starts, x in the view, top in the view}, the last two as fractions.
    float[] state =
        flickers.computeIfAbsent(layer, key -> new float[] {nextFlicker(flicker), 0.5f, 1f});

    int frame = flicker.frameAt(elapsed - state[0]);
    if (frame < 0) {
      if (elapsed > state[0]) {
        state[0] = nextFlicker(flicker);
        state[1] = MathUtils.random(0.1f, 0.9f);
        state[2] = MathUtils.random(0.85f, 1f);
      }
      return;
    }

    float frameWidth = 1f / flicker.frames();
    float height = viewHeight * flicker.height();
    float width = height * texture.getWidth() * frameWidth / texture.getHeight();
    batch.draw(
        texture,
        viewLeft + viewWidth * state[1] - width / 2f,
        viewBottom + viewHeight * state[2] - height,
        width,
        height,
        frame * frameWidth,
        1f,
        (frame + 1) * frameWidth,
        0f);
  }

  private float nextFlicker(BackdropLayer.Flicker flicker) {
    return elapsed + MathUtils.random(flicker.minGap(), flicker.maxGap());
  }

  /**
   * Works out where a layer spanning the map sits, so that it shows its bottom edge when the camera
   * is at the bottom of the map and its top edge when the camera is at the top.
   *
   * @param viewBottom the bottom edge of the view in world units
   * @param viewHeight the height of the view in world units
   * @param layerHeight the height the layer is drawn at in world units
   * @param mapHeight the height of the map in world units
   * @return the world y of the layer's bottom edge
   */
  static float spanningLayerBottom(
      float viewBottom, float viewHeight, float layerHeight, float mapHeight) {
    float cameraTravel = mapHeight - viewHeight;
    if (cameraTravel <= 0f) {
      return 0f;
    }
    float rate = (layerHeight - viewHeight) / cameraTravel;
    return viewBottom * (1f - rate);
  }

  /**
   * Works out which part of a view-filling layer's image is on screen. The image is scaled to the
   * height of the view and repeats sideways, so a view wider than the image shows it more than
   * once.
   *
   * @param layer the layer being drawn
   * @param viewLeft the left edge of the view in world units
   * @param viewWidth the width of the view in world units
   * @param viewHeight the height of the view in world units
   * @param textureAspect the image's width divided by its height
   * @param elapsed seconds the backdrop has been running, which drives drift
   * @return texture coordinates {u, v, u2, v2} for the bottom-left and top-right of the view
   */
  static float[] textureWindow(
      BackdropLayer layer,
      float viewLeft,
      float viewWidth,
      float viewHeight,
      float textureAspect,
      float elapsed) {
    float imageWidth = viewHeight * textureAspect;
    float u = wrap((viewLeft * layer.scroll() - layer.driftX() * elapsed) / imageWidth);
    float v = wrap(layer.driftY() * elapsed / viewHeight);
    return new float[] {u, v + 1f, u + viewWidth / imageWidth, v};
  }

  /** Keeps a texture offset in [0, 1) so it stays precise however long the level has run. */
  private static float wrap(float offset) {
    return offset - (float) Math.floor(offset);
  }

  private Iterable<String> repeatingTextures() {
    return (inFront ? mapData.getOverlays() : mapData.getBackdrops())
        .values().stream()
            .flatMap(backdrop -> backdrop.stream())
            .filter(layer -> !layer.spansMap() && layer.flicker() == null)
            .map(BackdropLayer::texture)
            .distinct()
            .toList();
  }

  private static Texture texture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  /** Backdrops sit below the terrain; overlays share the characters' layer and are drawn last. */
  @Override
  public int getLayer() {
    return inFront ? 1 : -2;
  }

  @Override
  public float getZIndex() {
    return inFront ? Float.MAX_VALUE : 0f;
  }
}
