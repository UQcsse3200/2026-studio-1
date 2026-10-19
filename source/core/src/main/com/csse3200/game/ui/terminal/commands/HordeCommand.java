package com.csse3200.game.ui.terminal.commands;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Spawns a requested number of regular enemies on nearby ground for debugging. */
public class HordeCommand implements Command {
  private static final int MAX_ENEMIES = 20;
  private static final int MIN_DISTANCE_TILES = 2;
  private static final int MAX_DISTANCE_TILES = 12;
  private static final String ENEMY_TYPE = "skeleton";

  private final Supplier<LevelGameArea> getLevelGameArea;
  private final Consumer<String> showOutput;

  public HordeCommand(Supplier<LevelGameArea> getLevelGameArea, Consumer<String> showOutput) {
    this.getLevelGameArea = getLevelGameArea;
    this.showOutput = showOutput;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    Integer count = parseCount(args);
    if (count == null) {
      return false;
    }

    LevelGameArea area = getLevelGameArea.get();
    if (area == null || area.getPlayer() == null || area.getLevel() == null) {
      return false;
    }

    LevelView level = area.getLevel();
    float tileSize = level.tileSize();
    if (tileSize <= 0f) {
      return false;
    }
    Entity player = area.getPlayer();
    Vector2 playerCenter = player.getCenterPosition();
    if (playerCenter == null) {
      return false;
    }

    List<GridPoint2> spawnTiles = findSpawnTiles(level, playerCenter, tileSize, count);
    // Choose all positions first so a crowded area does not produce a partial horde.
    if (spawnTiles.size() < count) {
      showOutput.accept("Not enough nearby ground to spawn " + count + " enemies.");
      return false;
    }

    int spawned = spawnEnemies(area, spawnTiles, tileSize);
    showOutput.accept("Spawned " + spawned + " of " + count + " skeletons.");
    return spawned == count;
  }

  private static Integer parseCount(ArrayList<String> args) {
    if (args.size() != 1) {
      return null;
    }

    try {
      int count = Integer.parseInt(args.getFirst());
      return count >= 1 && count <= MAX_ENEMIES ? count : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static List<GridPoint2> findSpawnTiles(
      LevelView level, Vector2 playerCenter, float tileSize, int count) {
    int playerX = (int) Math.floor(playerCenter.x / tileSize);
    int playerY = (int) Math.floor(playerCenter.y / tileSize);

    // Ground tiles have support below; keep only nearby spots with room for an enemy to stand.
    List<GridPoint2> candidates = new ArrayList<>(level.groundTiles());
    candidates.removeIf(
        tile ->
            Math.abs(tile.x - playerX) < MIN_DISTANCE_TILES
                || Math.abs(tile.x - playerX) > MAX_DISTANCE_TILES
                || Math.abs(tile.y - playerY) > 2
                || !level.isWalkable(tile.x, tile.y)
                || !level.inBounds(tile.x, tile.y + 1)
                || !level.isWalkable(tile.x, tile.y + 1));
    candidates.sort(
        Comparator.comparingInt(tile -> Math.abs(tile.x - playerX) + Math.abs(tile.y - playerY)));

    List<GridPoint2> spawnTiles = new ArrayList<>();
    Set<Integer> usedColumns = new HashSet<>();
    // One enemy per column avoids stacking enemies on platforms directly above each other.
    for (GridPoint2 tile : candidates) {
      if (usedColumns.add(tile.x)) {
        spawnTiles.add(tile);
      }
      if (spawnTiles.size() == count) {
        break;
      }
    }
    return spawnTiles;
  }

  private static int spawnEnemies(LevelGameArea area, List<GridPoint2> spawnTiles, float tileSize) {
    int spawned = 0;
    for (GridPoint2 tile : spawnTiles) {
      if (area.spawnEnemy(ENEMY_TYPE, new Vector2(tile.x * tileSize, tile.y * tileSize))) {
        spawned++;
      }
    }
    return spawned;
  }
}
