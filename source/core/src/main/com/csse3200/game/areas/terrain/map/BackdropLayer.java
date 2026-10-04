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
 */
public record BackdropLayer(
    String texture, float scroll, float driftX, float driftY, boolean spansMap, Rows rows) {

  /** Creates a layer that fills the view and is seen everywhere. */
  public BackdropLayer(String texture, float scroll, float driftX, float driftY) {
    this(texture, scroll, driftX, driftY, false, null);
  }

  /** Creates a layer that is seen everywhere. */
  public BackdropLayer(String texture, float scroll, float driftX, float driftY, boolean spansMap) {
    this(texture, scroll, driftX, driftY, spansMap, null);
  }

  /**
   * How strongly the layer shows when the camera is centred on a tile row: fully inside its band,
   * fading out over a few rows either side so weather does not snap on and off.
   *
   * @param tileY the tile row at the centre of the view
   * @return opacity from 0 (unseen) to 1 (fully seen)
   */
  public float visibilityAt(float tileY) {
    return rows == null ? 1f : rows.visibilityAt(tileY);
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
