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
  private final List<RoomTransition> transitions;
  private final String backgroundTexture;
  private final List<SubLevel> subLevels;

  /**
   * Creates map data with no transitions, background or sub-levels. For anything more, use {@link
   * #builder(String)}.
   */
  public LevelMapData(
      String name,
      float tileSize,
      int width,
      int height,
      Map<String, TileDefinition> legend,
      List<MapLayerData> layers,
      MapSpawns spawns) {
    this(
        builder(name)
            .tileSize(tileSize)
            .size(width, height)
            .legend(legend)
            .layers(layers)
            .spawns(spawns));
  }

  private LevelMapData(Builder builder) {
    String name = builder.name;
    float tileSize = builder.tileSize;
    int width = builder.width;
    int height = builder.height;
    Map<String, TileDefinition> legend = builder.legend;
    List<MapLayerData> layers = builder.layers;
    MapSpawns spawns = builder.spawns;
    List<RoomTransition> transitions = builder.transitions;
    String backgroundTexture = builder.backgroundTexture;
    List<SubLevel> subLevels = builder.subLevels;
    this.subLevels = subLevels == null ? Collections.emptyList() : subLevels;
    this.name = name;
    this.tileSize = tileSize;
    this.width = width;
    this.height = height;
    this.legend = legend;
    this.layers = layers;
    this.spawns = spawns;
    this.transitions = transitions;
    this.backgroundTexture = backgroundTexture;
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
   * @return doorways defined by this map (unmodifiable)
   */
  public List<RoomTransition> getTransitions() {
    return Collections.unmodifiableList(transitions);
  }

  /**
   * The named sections of this map the game treats as separate places, such as level 1's dungeon
   * and Nether. Empty for a map that is one place.
   *
   * @return the sub-levels in file order (unmodifiable)
   */
  public List<SubLevel> getSubLevels() {
    return Collections.unmodifiableList(subLevels);
  }

  /**
   * Finds the sub-level a tile row falls in.
   *
   * @param tileY tile y
   * @return the sub-level containing that row, or null if the map has none or none match
   */
  public SubLevel getSubLevelAt(int tileY) {
    for (SubLevel subLevel : subLevels) {
      if (subLevel.containsRow(tileY)) {
        return subLevel;
      }
    }
    return null;
  }

  /**
   * @return an optional composed background image which spans this entire map
   */
  public String getBackgroundTexture() {
    return backgroundTexture;
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
    for (RoomTransition transition : transitions) {
      if (transition.getTexture() != null) {
        paths.add(transition.getTexture());
      }
    }
    if (backgroundTexture != null) {
      paths.add(backgroundTexture);
    }
    return paths;
  }

  /**
   * @return true if the map has no layers (an "empty" map)
   */
  public boolean isEmpty() {
    return layers.isEmpty() || width == 0 || height == 0;
  }

  /**
   * Starts building map data.
   *
   * @param name the map's name
   * @return a builder with empty legend, layers, spawns, transitions and sub-levels
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  /** Assembles a {@link LevelMapData} one part at a time. */
  public static final class Builder {
    private final String name;
    private float tileSize = 0.5f;
    private int width;
    private int height;
    private Map<String, TileDefinition> legend = Collections.emptyMap();
    private List<MapLayerData> layers = Collections.emptyList();
    private MapSpawns spawns = new MapSpawns();
    private List<RoomTransition> transitions = Collections.emptyList();
    private String backgroundTexture;
    private List<SubLevel> subLevels = Collections.emptyList();

    private Builder(String name) {
      this.name = name;
    }

    /**
     * @param tileSize the world size of one tile
     * @return this builder
     */
    public Builder tileSize(float tileSize) {
      this.tileSize = tileSize;
      return this;
    }

    /**
     * @param width width in tiles
     * @param height height in tiles
     * @return this builder
     */
    public Builder size(int width, int height) {
      this.width = width;
      this.height = height;
      return this;
    }

    /**
     * @param legend symbols to tile definitions
     * @return this builder
     */
    public Builder legend(Map<String, TileDefinition> legend) {
      this.legend = legend;
      return this;
    }

    /**
     * @param layers tile layers in draw order, back to front
     * @return this builder
     */
    public Builder layers(List<MapLayerData> layers) {
      this.layers = layers;
      return this;
    }

    /**
     * @param spawns the map's spawn data
     * @return this builder
     */
    public Builder spawns(MapSpawns spawns) {
      this.spawns = spawns;
      return this;
    }

    /**
     * @param transitions doorways out of the map
     * @return this builder
     */
    public Builder transitions(List<RoomTransition> transitions) {
      this.transitions = transitions == null ? Collections.emptyList() : transitions;
      return this;
    }

    /**
     * @param backgroundTexture one image stretched over the whole map, or null
     * @return this builder
     */
    public Builder backgroundTexture(String backgroundTexture) {
      this.backgroundTexture = backgroundTexture;
      return this;
    }

    /**
     * @param subLevels named sections of the map, empty if it is one place
     * @return this builder
     */
    public Builder subLevels(List<SubLevel> subLevels) {
      this.subLevels = subLevels == null ? Collections.emptyList() : subLevels;
      return this;
    }

    /**
     * @return the assembled map data
     */
    public LevelMapData build() {
      return new LevelMapData(this);
    }
  }
}
