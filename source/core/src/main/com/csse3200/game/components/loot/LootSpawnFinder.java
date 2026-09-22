package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
 *
 * <p>What counts as open, ground, solid or a hazard is the level's own rule, read through {@link
 * LevelView}, so it stays correct when new tile types are added. Only the loot specific rules, room
 * above and reachability, live here.
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
    return findGroundSpots(new MapDataLevelView(map));
  }

  /**
   * Returns every tile that loot could be placed on, scanning from the bottom of the level up.
   *
   * @param level level to search
   * @return the usable tiles, empty when the level has no open, reachable ground
   * @throws IllegalArgumentException if {@code level} is null
   */
  public static List<SpawnPoint> findGroundSpots(LevelView level) {
    if (level == null) {
      throw new IllegalArgumentException("LevelView must not be null.");
    }

    boolean[][] reachable = findReachableTiles(level);
    List<SpawnPoint> spots = new ArrayList<>();

    // groundTiles() already covers open and standing on ground, bottom row up.
    for (GridPoint2 tile : level.groundTiles()) {
      boolean hasRoomAbove = !level.isSolid(tile.x, tile.y + 1);
      boolean playerCanReach = reachable[tile.x][tile.y];

      if (hasRoomAbove && playerCanReach) {
        spots.add(new SpawnPoint("loot", tile.x, tile.y));
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
   * @param level level to search
   * @return {@code reachable[x][y]} for each tile; when the level has no usable player spawn, every
   *     tile is marked reachable
   */
  private static boolean[][] findReachableTiles(LevelView level) {
    boolean[][] reached = new boolean[level.width()][level.height()];

    GridPoint2 start = level.playerSpawn();
    if (start == null || !level.inBounds(start.x, start.y)) {
      // No starting point to search from, so every tile is allowed.
      for (boolean[] column : reached) {
        Arrays.fill(column, true);
      }
      return reached;
    }

    Deque<GridPoint2> toVisit = new ArrayDeque<>();
    reached[start.x][start.y] = true;
    toVisit.add(start);

    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!toVisit.isEmpty()) {
      GridPoint2 tile = toVisit.poll();

      for (int[] direction : directions) {
        int nextX = tile.x + direction[0];
        int nextY = tile.y + direction[1];

        if (level.inBounds(nextX, nextY)
            && !reached[nextX][nextY]
            && canMoveThrough(level, nextX, nextY)) {
          reached[nextX][nextY] = true;
          toVisit.add(new GridPoint2(nextX, nextY));
        }
      }
    }

    return reached;
  }

  /**
   * Returns whether the player can move through a tile when working out what is reachable.
   *
   * <p>Platforms count as passable, because the player can jump up through them.
   *
   * @param level level to read
   * @param x tile x coordinate
   * @param y tile y coordinate
   * @return {@code true} for anything that is neither solid nor a hazard
   */
  private static boolean canMoveThrough(LevelView level, int x, int y) {
    return !level.isSolid(x, y) && !level.isHazard(x, y);
  }

  private LootSpawnFinder() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
