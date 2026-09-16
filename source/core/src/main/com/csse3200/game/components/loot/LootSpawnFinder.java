package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapSpawns;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Finds the tiles on a map where a piece of loot can rest on the ground.
 *
 * <p>Most maps do not declare loot spawn points, so loot needs somewhere sensible to go without a
 * map author placing it by hand. A tile qualifies when all three of these hold:
 *
 * <ol>
 *   <li><b>Open:</b> the tile itself is empty space, so loot is never placed inside a wall.
 *   <li><b>Standing on ground:</b> the tile below is a floor, wall or platform, so loot does not
 *       float and is not left on a hazard.
 *   <li><b>Room above:</b> the tile above is not a floor or wall. Without this, a gap sealed inside
 *       a thick band of ground passed the first two checks and loot was drawn inside the ground
 *       (bug #194).
 * </ol>
 *
 * <p>On top of that, a tile must be <b>reachable</b>: starting from the player's spawn, the finder
 * visits every tile the player can move through, one neighbour at a time, and only tiles it visited
 * are used. That rules out open pockets walled off from the rest of the map, which can still pass
 * the three checks above. A map with no player spawn has no starting point, so this check is
 * skipped for it.
 *
 * <p>Reachability only follows open tiles, not jumps, so a ledge that needs a jump the player
 * cannot make can still be chosen. Maps that need exact control can declare their own loot spawn
 * points, which are always used first.
 */
public class LootSpawnFinder {

  /**
   * Returns every tile that loot could be placed on, scanning from the bottom of the map up.
   *
   * @param map map to search
   * @return the usable tiles, empty when the map has no open, reachable ground
   * @throws IllegalArgumentException if {@code map} is null
   */
  public static List<SpawnPoint> findGroundSpots(LevelMapData map) {
    if (map == null) {
      throw new IllegalArgumentException("LevelMapData must not be null.");
    }

    boolean[][] reachable = findReachableTiles(map);
    List<SpawnPoint> spots = new ArrayList<>();

    // Row 0 has nothing beneath it, so the scan starts one row up.
    for (int y = 1; y < map.getHeight(); y++) {
      for (int x = 0; x < map.getWidth(); x++) {
        TileType tile = map.getTileType(x, y);
        TileType below = map.getTileType(x, y - 1);
        TileType above = tileAt(map, x, y + 1);

        boolean restsOnGround = isOpen(tile) && isGround(below);
        boolean hasRoomAbove = !isSolid(above);
        boolean playerCanReach = reachable == null || reachable[x][y];

        if (restsOnGround && hasRoomAbove && playerCanReach) {
          spots.add(new SpawnPoint("loot", x, y));
        }
      }
    }

    return spots;
  }

  /**
   * Marks every tile the player can get to by moving through open tiles from their spawn.
   *
   * <p>This is a breadth-first search: the spawn tile goes into a queue, and each tile taken out of
   * the queue adds its unvisited neighbours (left, right, up and down) that the player can move
   * through. When the queue is empty, every connected tile has been visited.
   *
   * @param map map to search
   * @return {@code reachable[x][y]} for each tile, or {@code null} when the map has no usable
   *     player spawn, meaning every tile should be treated as reachable
   */
  private static boolean[][] findReachableTiles(LevelMapData map) {
    MapSpawns spawns = map.getSpawns();
    GridPoint2 start = spawns == null ? null : spawns.getPlayer();
    if (start == null || !isInside(map, start.x, start.y)) {
      return null;
    }

    boolean[][] reached = new boolean[map.getWidth()][map.getHeight()];
    Deque<GridPoint2> toVisit = new ArrayDeque<>();
    reached[start.x][start.y] = true;
    toVisit.add(start);

    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!toVisit.isEmpty()) {
      GridPoint2 tile = toVisit.poll();

      for (int[] direction : directions) {
        int nextX = tile.x + direction[0];
        int nextY = tile.y + direction[1];

        if (isInside(map, nextX, nextY)
            && !reached[nextX][nextY]
            && canMoveThrough(map.getTileType(nextX, nextY))) {
          reached[nextX][nextY] = true;
          toVisit.add(new GridPoint2(nextX, nextY));
        }
      }
    }

    return reached;
  }

  /**
   * Returns the tile at a position, treating anything outside the map as empty.
   *
   * @param map map to read
   * @param x tile x coordinate
   * @param y tile y coordinate
   * @return the tile, or {@code null} for an empty cell or a position off the map
   */
  private static TileType tileAt(LevelMapData map, int x, int y) {
    return isInside(map, x, y) ? map.getTileType(x, y) : null;
  }

  /**
   * Returns whether a position is on the map.
   *
   * @param map map to check against
   * @param x tile x coordinate
   * @param y tile y coordinate
   * @return {@code true} when the position is within the map's width and height
   */
  private static boolean isInside(LevelMapData map, int x, int y) {
    return x >= 0 && y >= 0 && x < map.getWidth() && y < map.getHeight();
  }

  /**
   * Returns whether a tile is empty enough for loot to sit in.
   *
   * <p>Ladders are excluded even though they can be walked through, because loot dropped onto a
   * ladder is awkward to pick up and looks out of place.
   *
   * @param tile tile to check, or {@code null} for an empty cell
   * @return {@code true} for empty cells and purely decorative tiles
   */
  private static boolean isOpen(TileType tile) {
    return tile == null || tile == TileType.DECORATIVE;
  }

  /**
   * Returns whether a tile can hold loot up.
   *
   * @param tile tile to check, or {@code null} for an empty cell
   * @return {@code true} for floors, walls and platforms
   */
  private static boolean isGround(TileType tile) {
    return tile == TileType.FLOOR || tile == TileType.WALL || tile == TileType.PLATFORM;
  }

  /**
   * Returns whether a tile is completely solid, blocking anything above or beside it.
   *
   * <p>Platforms are not solid here, because the player can jump up through them.
   *
   * @param tile tile to check, or {@code null} for an empty cell
   * @return {@code true} for floors and walls
   */
  private static boolean isSolid(TileType tile) {
    return tile == TileType.FLOOR || tile == TileType.WALL;
  }

  /**
   * Returns whether the player can move through a tile when working out what is reachable.
   *
   * @param tile tile to check, or {@code null} for an empty cell
   * @return {@code true} for anything that is neither solid nor a hazard
   */
  private static boolean canMoveThrough(TileType tile) {
    return !isSolid(tile) && tile != TileType.HAZARD;
  }

  private LootSpawnFinder() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
