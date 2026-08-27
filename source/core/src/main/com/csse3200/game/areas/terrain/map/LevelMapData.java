package com.csse3200.game.areas.terrain.map;

import com.csse3200.game.areas.terrain.TileType;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fully parsed contents of a level map file: dimensions, the legend of tile symbols, the
 * ordered list of layers, and all spawn data. This is a pure data object with no rendering
 * dependencies, so it can be produced and asserted on in unit tests without a graphics context.
 *
 * <p>Rendering is handled separately by {@code TerrainFactory.createTerrainFromMap}, which turns
 * this data into a {@code TiledMap}. Collision (#16) reads tile types via {@link
 * #getCollisionLayer()}.
 */
public class LevelMapData {
  /** Preferred name of the layer used to derive collision, if present. */
  public static final String COLLISION_LAYER = "collision";

  /** Fallback layer used for collision when no explicit collision layer exists. */
  public static final String TERRAIN_LAYER = "terrain";

  private final String name;
  private final float tileSize;
  private final int width;
  private final int height;
  private final Map<String, TileDefinition> legend;
  private final List<MapLayerData> layers;
  private final MapSpawns spawns;

  public LevelMapData(
      String name,
      float tileSize,
      int width,
      int height,
      Map<String, TileDefinition> legend,
      List<MapLayerData> layers,
      MapSpawns spawns) {
    this.name = name;
    this.tileSize = tileSize;
    this.width = width;
    this.height = height;
    this.legend = legend;
    this.layers = layers;
    this.spawns = spawns;
  }

  public String getName() {
    return name;
  }

  /**
   * @return the world size of one tile, matching the units used by {@code TerrainComponent}
   */
  public float getTileSize() {
    return tileSize;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * @return the legend mapping symbols to tile definitions (unmodifiable)
   */
  public Map<String, TileDefinition> getLegend() {
    return Collections.unmodifiableMap(legend);
  }

  /**
   * @return the layers in draw order, back to front (unmodifiable)
   */
  public List<MapLayerData> getLayers() {
    return Collections.unmodifiableList(layers);
  }

  /**
   * Find a layer by name.
   *
   * @param layerName the layer name
   * @return the matching layer, or null if not present
   */
  public MapLayerData getLayer(String layerName) {
    for (MapLayerData layer : layers) {
      if (layer.getName().equals(layerName)) {
        return layer;
      }
    }
    return null;
  }

  /**
   * The layer the collision system (#16) should read to build colliders: the explicit "collision"
   * layer if defined, otherwise the "terrain" layer.
   *
   * @return the collision source layer, or null if neither exists
   */
  public MapLayerData getCollisionLayer() {
    MapLayerData collision = getLayer(COLLISION_LAYER);
    return collision != null ? collision : getLayer(TERRAIN_LAYER);
  }

  /**
   * Convenience accessor for the collision-relevant tile type at a cell.
   *
   * @return the {@link TileType} at {@code (x, y)} in the collision layer, or {@code null} if the
   *     cell is empty or no collision layer exists
   */
  public TileType getTileType(int x, int y) {
    MapLayerData layer = getCollisionLayer();
    if (layer == null) {
      return null;
    }
    TileDefinition def = layer.get(x, y);
    return def == null ? null : def.type();
  }

  public MapSpawns getSpawns() {
    return spawns;
  }

  /**
   * All distinct, non-null texture paths referenced by the legend. Used by a game area to know
   * which textures to load before building the terrain.
   *
   * @return the set of texture asset paths
   */
  public Set<String> getTexturePaths() {
    Set<String> paths = new LinkedHashSet<>();
    for (TileDefinition def : legend.values()) {
      if (def.texture() != null) {
        paths.add(def.texture());
      }
    }
    return paths;
  }

  /**
   * @return true if the map has no layers (an "empty" map)
   */
  public boolean isEmpty() {
    return layers.isEmpty() || width == 0 || height == 0;
  }
}
