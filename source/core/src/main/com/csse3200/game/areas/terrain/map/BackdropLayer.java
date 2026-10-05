package com.csse3200.game.areas.terrain.map;

/**
 * One image of a parallax backdrop or overlay, drawn behind or in front of the tile layers.
 *
 * <p>A backdrop is a stack of these, listed back to front. A layer is drawn one of two ways:
 *
 * <ul>
 *   <li>Filling the view, pinned to the screen, sliding its artwork sideways as the camera pans so
 *       distant layers appear to move less than near ones. This suits a level explored sideways.
 *   <li>Spanning the map: as wide as the map and as tall as its own proportions make it, rising
 *       more slowly than the camera so its bottom meets the map's bottom and its top the map's top.
 *       The shorter the image, the further away it seems. This suits a level that is climbed.
 * </ul>
 *
 * @param texture the image; one that fills the view must tile seamlessly in every direction it
 *     moves
 * @param scroll for a layer filling the view, how far the artwork moves relative to the camera: 0
 *     is fixed to the screen, 1 moves with the tiles
 * @param driftX for a layer filling the view, constant sideways movement in world units per second
 * @param driftY for a layer filling the view, constant upward movement in world units per second
 * @param spansMap true to span the map rather than fill the view
 * @param rows the band of the map the layer is seen in, or null to be seen everywhere
 * @param alpha how opaque the layer is drawn, from 0 (unseen) to 1 (solid)
 * @param flicker for a layer that is a strip of frames shown now and then, such as distant
 *     lightning, how it plays; null for an ordinary layer
 */
public record BackdropLayer(
    String texture,
    float scroll,
    float driftX,
    float driftY,
    boolean spansMap,
    Rows rows,
    float alpha,
    Flicker flicker) {

  /** Creates a layer that fills the view and is seen everywhere. */
  public BackdropLayer(String texture, float scroll, float driftX, float driftY) {
    this(texture, scroll, driftX, driftY, false, null);
  }

  /** Creates a layer that is seen everywhere. */
  public BackdropLayer(String texture, float scroll, float driftX, float driftY, boolean spansMap) {
    this(texture, scroll, driftX, driftY, spansMap, null);
  }

  /** Creates a solid, ordinary layer. */
  public BackdropLayer(
      String texture, float scroll, float driftX, float driftY, boolean spansMap, Rows rows) {
    this(texture, scroll, driftX, driftY, spansMap, rows, 1f, null);
  }

  /**
   * How strongly the layer shows when the camera is centred on a tile row: fully inside its band,
   * fading out over a few rows either side so weather does not snap on and off.
   *
   * @param tileY the tile row at the centre of the view
   * @return opacity from 0 (unseen) to 1 (fully seen)
   */
  public float visibilityAt(float tileY) {
    return alpha * (rows == null ? 1f : rows.visibilityAt(tileY));
  }

  /**
   * How a layer that is a horizontal strip of frames plays: once through, somewhere new in the
   * upper part of the view, after a random wait.
   *
   * @param frames the number of frames in the strip
   * @param fps frames shown per second
   * @param minGap the shortest wait between plays, in seconds
   * @param maxGap the longest wait between plays, in seconds
   * @param height the height a frame is drawn at, as a fraction of the view's height
   */
  public record Flicker(int frames, float fps, float minGap, float maxGap, float height) {
    /**
     * @param sincePlayStarted seconds since the strip last began to play
     * @return the frame to show, or -1 when the strip is not playing
     */
    public int frameAt(float sincePlayStarted) {
      int frame = (int) (sincePlayStarted * fps);
      return sincePlayStarted < 0f || frame >= frames ? -1 : frame;
    }
  }

  /**
   * A band of tile rows, counted from the bottom of the map.
   *
   * @param from the lowest row of the band
   * @param to the highest row of the band
   */
  public record Rows(int from, int to) {
    private static final float FADE_ROWS = 8f;

    float visibilityAt(float tileY) {
      float outside = Math.max(from - tileY, tileY - to);
      return Math.clamp(1f - outside / FADE_ROWS, 0f, 1f);
    }
  }
}
