package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import java.util.List;

/**
 * Everything a system outside the map package needs to ask about the level it is running in.
 *
 * <p>This is the supported way to read a level. Prefer it to {@link LevelMapData}: that class is
 * the map package's own structure, and reading it from outside couples your feature to how layers
 * happen to be arranged today. Every method here keeps working when the map format changes.
 *
 * <pre>{@code
 * // is there something under the player's feet?
 * boolean grounded = level.isSupporting(tileX, tileY - 1);
 *
 * // put one of my things wherever the map author marked
 * for (Marker marker : level.markers("npc")) {
 *   spawnNpc(marker.id(), level.tileToWorld(marker.position()));
 * }
 * }</pre>
 *
 * <p>All coordinates are tile coordinates with the origin at the bottom left and y increasing
 * upwards, matching world coordinates. Out of bounds reads return null or false rather than
 * throwing, so a system scanning around a tile does not have to bounds check first.
 */
public interface LevelView {

  /**
   * @return the level's display name
   */
  String name();

  /**
   * @return width in tiles
   */
  int width();

  /**
   * @return height in tiles
   */
  int height();

  /**
   * @return the world size of one tile
   */
  float tileSize();

  /**
   * @param x tile x
   * @param y tile y
   * @return true if the tile is inside the level
   */
  boolean inBounds(int x, int y);

  /**
   * The full tile at a position, including any properties its legend entry carries.
   *
   * @param x tile x
   * @param y tile y
   * @return the tile, or null if the cell is empty or out of bounds
   */
  TileDefinition tileAt(int x, int y);

  /**
   * @param x tile x
   * @param y tile y
   * @return the tile's type, or null if the cell is empty or out of bounds
   */
  TileType tileTypeAt(int x, int y);

  /**
   * @param x tile x
   * @param y tile y
   * @return true if the tile blocks movement from every direction
   */
  boolean isSolid(int x, int y);

  /**
   * @param x tile x
   * @param y tile y
   * @return true if something standing on this tile would not fall, so floors, walls and one-way
   *     platforms
   */
  boolean isSupporting(int x, int y);

  /**
   * @param x tile x
   * @param y tile y
   * @return true if the tile can be moved through, so empty cells, decoration and ladders
   */
  boolean isWalkable(int x, int y);

  /**
   * @param x tile x
   * @param y tile y
   * @return true if standing on this tile damages the player
   */
  boolean isHazard(int x, int y);

  /**
   * @return the tile the player starts on, or null if the level defines none
   */
  GridPoint2 playerSpawn();

  /**
   * Every tile with open space above solid footing, from the bottom of the level up. What loot,
   * props and wandering entities use to find somewhere sensible to stand.
   *
   * @return the usable tiles, empty if the level has no open ground
   */
  List<GridPoint2> groundTiles();

  /**
   * The markers a map author placed under one kind.
   *
   * @param kind the marker kind, matched ignoring case (e.g. "npc", "light", "checkpoint")
   * @return the markers, empty if the level has none of that kind
   */
  List<Marker> markers(String kind);

  /**
   * @return the doorways leading out of this level
   */
  List<RoomTransition> transitions();
}
