package com.csse3200.game.components.loot;

import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the tiles on a map where a piece of loot can rest on the ground.
 *
 * <p>Most maps do not declare loot spawn points, so loot needs somewhere sensible to go without a
 * map author placing it by hand. A tile qualifies when it is open space and the tile directly below
 * it is something you can stand on. That one rule keeps loot out of walls, stops it floating in the
 * air, and keeps it off hazards.
 *
 * <p>This only checks the tiles immediately around each spot, so a tile inside a sealed pocket of
 * wall can still qualify. Maps that need tighter control can declare their own loot spawn points,
 * which are always used first.
 */
public class LootSpawnFinder {

  /**
   * Returns every tile that loot could be placed on, scanning from the bottom of the map up.
   *
   * @param map map to search
   * @return the usable tiles, empty when the map has no open ground
   * @throws IllegalArgumentException if {@code map} is null
   */
  public static List<SpawnPoint> findGroundSpots(LevelMapData map) {
    if (map == null) {
      throw new IllegalArgumentException("LevelMapData must not be null.");
    }

    List<SpawnPoint> spots = new ArrayList<>();

    // Row 0 has nothing beneath it, so the scan starts one row up.
    for (int y = 1; y < map.getHeight(); y++) {
      for (int x = 0; x < map.getWidth(); x++) {
        TileType tile = map.getTileType(x, y);
        TileType below = map.getTileType(x, y - 1);
        if (isOpen(tile) && isGround(below)) {
          spots.add(new SpawnPoint("loot", x, y));
        }
      }
    }

    return spots;
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

  private LootSpawnFinder() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
