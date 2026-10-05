package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;

/**
 * One named section of a map that the game treats as a level of its own.
 *
 * <p>Level 1 is a single map holding a dungeon below and the Nether above. They share a tile grid
 * but are separate places: the camera stays inside one of them, and crossing between them shows a
 * title card. A sub-level says where that section is and what to call it.
 *
 * <pre>{@code
 * "subLevels": [
 *   {
 *     "id": "dungeon",
 *     "title": "DUNGEON",
 *     "bounds": { "x": 0, "y": 0, "width": 56, "height": 32 },
 *     "door": { "x": 26, "y": 22 },
 *     "destination": "nether",
 *     "destinationSpawn": { "x": 26, "y": 33 }
 *   }
 * ]
 * }</pre>
 *
 * <p>All coordinates are tile coordinates, origin bottom left, y up.
 *
 * @param id the name this sub-level is referred to by, used as a destination
 * @param title the card shown when the player crosses into it, or null for no card
 * @param bounds the tile rectangle it occupies
 * @param door the tile holding the lift or stair between sections, or null if it has none
 * @param destination the id of the sub-level the door leads to, or null
 * @param destinationSpawn the tile the player arrives on over there, or null
 */
public record SubLevel(
    String id,
    String title,
    Bounds bounds,
    GridPoint2 door,
    String destination,
    GridPoint2 destinationSpawn) {

  /**
   * A tile rectangle, with its origin at the bottom left corner.
   *
   * @param x left edge in tiles
   * @param y bottom edge in tiles
   * @param width width in tiles
   * @param height height in tiles
   */
  public record Bounds(int x, int y, int width, int height) {
    /**
     * @return the bottom edge in tiles
     */
    public int bottom() {
      return y;
    }

    /**
     * @return the tile row above this rectangle
     */
    public int top() {
      return y + height;
    }

    /**
     * @param tileX tile x
     * @param tileY tile y
     * @return true if the tile is inside this rectangle
     */
    public boolean contains(int tileX, int tileY) {
      return tileX >= x && tileX < x + width && tileY >= y && tileY < top();
    }
  }

  /**
   * @param tileY tile y
   * @return true if the row is within this sub-level, ignoring x
   */
  public boolean containsRow(int tileY) {
    return tileY >= bounds.bottom() && tileY < bounds.top();
  }
}
