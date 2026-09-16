package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
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
    return findGroundSpots(new MapDataLevelView(map));
  }

  /**
   * Returns every tile that loot could be placed on.
   *
   * <p>Where a tile counts as ground is the level's own rule, so this asks the level rather than
   * reading its layers.
   *
   * @param level level to search
   * @return the usable tiles, empty when the level has no open ground
   * @throws IllegalArgumentException if {@code level} is null
   */
  public static List<SpawnPoint> findGroundSpots(LevelView level) {
    if (level == null) {
      throw new IllegalArgumentException("LevelView must not be null.");
    }

    List<SpawnPoint> spots = new ArrayList<>();
    for (GridPoint2 tile : level.groundTiles()) {
      spots.add(new SpawnPoint("loot", tile.x, tile.y));
    }
    return spots;
  }

  private LootSpawnFinder() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
