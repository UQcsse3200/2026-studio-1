package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.CollisionType;
import com.csse3200.game.areas.terrain.TileType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@link LevelView} over a loaded {@link LevelMapData}.
 *
 * <p>Everything here is derived from the map's collision layer, so features asking about the level
 * never need to know which layer that is or how tile types map to behaviour.
 */
public class MapDataLevelView implements LevelView {
  private final LevelMapData map;

  /**
   * @param map the loaded map to read, never null
   */
  public MapDataLevelView(LevelMapData map) {
    if (map == null) {
      throw new IllegalArgumentException("LevelMapData must not be null.");
    }
    this.map = map;
  }

  @Override
  public String name() {
    return map.getName();
  }

  @Override
  public int width() {
    return map.getWidth();
  }

  @Override
  public int height() {
    return map.getHeight();
  }

  @Override
  public float tileSize() {
    return map.getTileSize();
  }

  @Override
  public boolean inBounds(int x, int y) {
    return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
  }

  @Override
  public TileDefinition tileAt(int x, int y) {
    MapLayerData layer = map.getCollisionLayer();
    return layer == null ? null : layer.get(x, y);
  }

  @Override
  public TileType tileTypeAt(int x, int y) {
    TileDefinition tile = tileAt(x, y);
    return tile == null ? null : tile.type();
  }

  @Override
  public boolean isSolid(int x, int y) {
    return collisionAt(x, y) == CollisionType.SOLID;
  }

  @Override
  public boolean isSupporting(int x, int y) {
    CollisionType collision = collisionAt(x, y);
    return collision == CollisionType.SOLID || collision == CollisionType.PLATFORM;
  }

  @Override
  public boolean isWalkable(int x, int y) {
    if (!inBounds(x, y)) {
      return false;
    }
    return collisionAt(x, y) == CollisionType.NONE;
  }

  @Override
  public boolean isHazard(int x, int y) {
    return collisionAt(x, y) == CollisionType.HAZARD;
  }

  @Override
  public GridPoint2 playerSpawn() {
    return map.getSpawns().getPlayer();
  }

  @Override
  public List<GridPoint2> groundTiles() {
    List<GridPoint2> tiles = new ArrayList<>();
    // Row 0 has nothing beneath it, so the scan starts one row up.
    for (int y = 1; y < map.getHeight(); y++) {
      for (int x = 0; x < map.getWidth(); x++) {
        if (isOpenForStanding(x, y) && isSupporting(x, y - 1)) {
          tiles.add(new GridPoint2(x, y));
        }
      }
    }
    return Collections.unmodifiableList(tiles);
  }

  @Override
  public List<Marker> markers(String kind) {
    return map.getSpawns().getMarkers(kind);
  }

  @Override
  public List<RoomTransition> transitions() {
    return map.getTransitions();
  }

  @Override
  public List<SubLevel> subLevels() {
    return map.getSubLevels();
  }

  @Override
  public SubLevel subLevelAt(int tileY) {
    return map.getSubLevelAt(tileY);
  }

  /**
   * Open space something can occupy while resting on the tile below. Ladders are excluded: they are
   * walkable, but an object left on one is awkward to reach and looks out of place.
   */
  private boolean isOpenForStanding(int x, int y) {
    TileType type = tileTypeAt(x, y);
    return type == null || type == TileType.DECORATIVE;
  }

  /** An empty cell collides with nothing, which is the same as a decorative tile. */
  private CollisionType collisionAt(int x, int y) {
    TileDefinition tile = tileAt(x, y);
    return tile == null ? CollisionType.NONE : tile.type().getCollisionType();
  }
}
