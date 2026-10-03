package com.csse3200.game.areas.terrain.map;

/**
 * One image of a parallax backdrop, drawn behind the tile layers.
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
 */
public record BackdropLayer(
    String texture, float scroll, float driftX, float driftY, boolean spansMap) {

  /** Creates a layer that fills the view. */
  public BackdropLayer(String texture, float scroll, float driftX, float driftY) {
    this(texture, scroll, driftX, driftY, false);
  }
}
