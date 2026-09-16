package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.areas.terrain.TileType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads level maps from a custom JSON format.
 *
 * <p>The format uses human-editable rows of characters per layer, resolved against a legend:
 *
 * <pre>{@code
 * {
 *   "name": "Demo Level",
 *   "tileSize": 0.5,
 *   "legend": {
 *     "#": { "type": "WALL",  "texture": "images/environment/forest/grass_3.png" },
 *     ".": { "type": "FLOOR", "texture": "images/environment/forest/grass_1.png" }
 *   },
 *   "layers": {
 *     "background": ["....", "...."],
 *     "terrain":    ["####", "#..#"]
 *   },
 *   "spawns": {
 *     "player":  { "x": 1, "y": 1 },
 *     "enemies": [ { "type": "ghost", "x": 2, "y": 1 } ],
 *     "loot":    [ { "x": 3, "y": 1 } ]
 *   }
 * }
 * }</pre>
 *
 * <p>Rows are listed top-to-bottom for readability; the loader flips them so that {@code y = 0} is
 * the bottom row (matching world coordinates). A space, or any character absent from the legend,
 * means an empty cell. Spawn coordinates use world tile coordinates (bottom-left origin, y up).
 *
 * <p>An optional {@code backgroundTexture} renders one composed image behind the tile layers, for
 * maps whose art is authored as a single scene rather than per-tile. Unknown top-level keys are
 * ignored, so maps may carry an {@code authoring} block of design-time data the runtime does not
 * read. Every level map uses this one format; there is no per-level parsing path.
 */
public class JsonMapLoader implements MapLoader {
  private static final Logger logger = LoggerFactory.getLogger(JsonMapLoader.class);
  private static final float DEFAULT_TILE_SIZE = 0.5f;
  private static final char EMPTY_CELL = ' ';

  @Override
  public LevelMapData load(String path) {
    FileHandle file;
    try {
      file = Gdx.files.internal(path);
    } catch (Exception e) {
      throw new MapLoadException("Cannot access map file: " + path, e);
    }
    if (file == null || !file.exists()) {
      throw new MapLoadException("Map file not found: " + path);
    }

    String content;
    try {
      content = file.readString();
    } catch (Exception e) {
      throw new MapLoadException("Failed to read map file: " + path, e);
    }

    LevelMapData data = parse(content);
    logger.info(
        "Loaded map '{}' ({}x{}, {} layers)",
        data.getName(),
        data.getWidth(),
        data.getHeight(),
        data.getLayers().size());
    return data;
  }

  /**
   * Parse map JSON content into {@link LevelMapData}. Exposed separately from {@link #load(String)}
   * so parsing can be unit-tested without a libGDX file/graphics context.
   *
   * @param content the raw JSON text
   * @return the parsed map data
   * @throws MapLoadException if the content is malformed or structurally invalid
   */
  public LevelMapData parse(String content) {
    JsonValue root;
    try {
      root = new JsonReader().parse(content);
    } catch (Exception e) {
      throw new MapLoadException("Malformed map JSON", e);
    }
    if (root == null || !root.isObject()) {
      throw new MapLoadException("Map root must be a JSON object");
    }

    String name = root.getString("name", "unnamed");
    float tileSize = root.getFloat("tileSize", DEFAULT_TILE_SIZE);
    Map<String, TileDefinition> legend = parseLegend(root.get("legend"), name);

    // parse entity legend
    Map<String, JsonValue> entityLegend = parseEntityLegend(root.get("entityLegend"));

    JsonValue layersJson = root.get("layers");
    if (layersJson == null) {
      throw new MapLoadException("Map '" + name + "' has no 'layers' section");
    }
    if (!layersJson.isObject()) {
      throw new MapLoadException("Map '" + name + "' 'layers' must be a JSON object");
    }

    LayerRows collected = collectLayerRows(layersJson, name);
    SplitLayers split = splitLayers(collected, legend);
    int width = collected.width();
    int height = collected.height();

    validateEntityLayer(split.entityRows(), width, height, name);

    MapSpawns spawns = parseSpawns(root.get("spawns"), split.entityRows(), entityLegend, height);

    List<RoomTransition> transitions = parseTransitions(root.get("transitions"), name);

    validateSpawns(spawns, width, height, name);
    validateTransitions(transitions, width, height, name);

    return new LevelMapData(
        name,
        tileSize,
        width,
        height,
        legend,
        split.layers(),
        spawns,
        transitions,
        resolveBackgroundTexture(root, name));
  }

  /**
   * Collects each layer's raw rows and computes the overall map dimensions. The {@code entities}
   * layer is collected but does not contribute to the dimensions, since entity positions should not
   * affect map size.
   */
  private LayerRows collectLayerRows(JsonValue layersJson, String mapName) {
    List<String> layerNames = new ArrayList<>();
    List<String[]> layerRows = new ArrayList<>();
    int width = 0;
    int height = 0;

    for (JsonValue layer = layersJson.child; layer != null; layer = layer.next) {
      if (!layer.isArray()) {
        throw new MapLoadException(
            "Layer '" + layer.name + "' in map '" + mapName + "' must be an array of strings");
      }

      String[] rows = layer.asStringArray();
      layerNames.add(layer.name);
      layerRows.add(rows);

      if (layer.name.equals("entities")) {
        continue;
      }

      height = Math.max(height, rows.length);
      for (String row : rows) {
        width = Math.max(width, row.length());
      }
    }

    return new LayerRows(layerNames, layerRows, width, height);
  }

  /**
   * Builds the typed tile grids from the collected rows, flipping each so {@code y = 0} is the
   * bottom, and separates out the raw {@code entities} layer rows (if present) rather than treating
   * them as a tile layer.
   */
  private SplitLayers splitLayers(LayerRows collected, Map<String, TileDefinition> legend) {
    List<MapLayerData> layers = new ArrayList<>();
    String[] entityRows = null;

    for (int i = 0; i < collected.names().size(); i++) {
      String layerName = collected.names().get(i);
      String[] rows = collected.rows().get(i);

      if (layerName.equals("entities")) {
        entityRows = rows;
      } else {
        layers.add(buildLayer(layerName, rows, legend, collected.width(), collected.height()));
      }
    }

    return new SplitLayers(layers, entityRows);
  }

  private record LayerRows(List<String> names, List<String[]> rows, int width, int height) {}

  private record SplitLayers(List<MapLayerData> layers, String[] entityRows) {}

  /**
   * Checks the entities layer lines up with the map grid.
   *
   * <p>The entities layer is excluded from the map's dimensions, so an entities grid that is
   * shorter than the map would otherwise load without complaint and silently place every spawn in
   * the wrong row, since rows are flipped against the map height rather than their own length.
   *
   * @param rows the raw entities-layer rows, or null when the map has no entities layer
   * @param width the map width in tiles
   * @param height the map height in tiles
   * @param mapName the map's name, for the error message
   * @throws MapLoadException if the entities layer does not match the map grid
   */
  private void validateEntityLayer(String[] rows, int width, int height, String mapName) {
    if (rows == null) {
      return;
    }
    if (rows.length != height) {
      throw new MapLoadException(
          "Map '"
              + mapName
              + "' entities layer has "
              + rows.length
              + " rows but the map is "
              + height
              + " tiles tall");
    }
    for (String row : rows) {
      if (row.length() > width) {
        throw new MapLoadException(
            "Map '"
                + mapName
                + "' entities layer has a row of "
                + row.length()
                + " characters but the map is "
                + width
                + " tiles wide");
      }
    }
  }

  /**
   * Resolves the optional composed background image, which is stretched over the whole map behind
   * the tile layers.
   *
   * <p>A map may name its artwork before the image lands in assets. Loading a texture that is not
   * there fails the level, so a declared-but-missing background is warned about and dropped,
   * leaving the map to render from its tile layers alone.
   *
   * @param root the parsed map object
   * @param mapName the map's name, for logging
   * @return the background texture path, or null if absent or not yet supplied
   */
  private String resolveBackgroundTexture(JsonValue root, String mapName) {
    String path = root.getString("backgroundTexture", null);
    if (path == null || path.isBlank()) {
      return null;
    }
    if (Gdx.files != null && !Gdx.files.internal(path).exists()) {
      logger.warn(
          "Map '{}' declares background image '{}', which does not exist yet - rendering the map"
              + " without it",
          mapName,
          path);
      return null;
    }
    return path;
  }

  private Map<String, TileDefinition> parseLegend(JsonValue legendJson, String mapName) {
    Map<String, TileDefinition> legend = new HashMap<>();
    if (legendJson == null) {
      return legend;
    }
    for (JsonValue entry = legendJson.child; entry != null; entry = entry.next) {
      String symbol = entry.name;
      String typeStr = entry.getString("type", "DECORATIVE");
      String texture = entry.getString("texture", null);
      TileType type;
      try {
        type = TileType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new MapLoadException(
            "Unknown tile type '"
                + typeStr
                + "' for legend symbol '"
                + symbol
                + "' in map '"
                + mapName
                + "'",
            e);
      }
      legend.put(symbol, new TileDefinition(type, texture));
    }
    return legend;
  }

  private Map<String, JsonValue> parseEntityLegend(JsonValue entityLegendJson) {
    Map<String, JsonValue> entityLegend = new HashMap<>();

    if (entityLegendJson == null) {
      return entityLegend;
    }

    for (JsonValue entry = entityLegendJson.child; entry != null; entry = entry.next) {

      entityLegend.put(entry.name, entry);
    }

    return entityLegend;
  }

  private void parseEntityLayer(
      String[] rows, Map<String, JsonValue> entityLegend, MapSpawns spawns, int height) {
    for (int r = 0; r < rows.length; r++) {
      String row = rows[r];
      int y = height - 1 - r; // Same coordinate conversion used by buildLayer().

      for (int x = 0; x < row.length(); x++) {
        char ch = row.charAt(x);
        if (ch == EMPTY_CELL) {
          continue;
        }
        placeEntity(ch, x, y, entityLegend, spawns);
      }
    }
  }

  /** Resolves one entities-layer symbol against the entity legend and records its spawn. */
  private void placeEntity(
      char ch, int x, int y, Map<String, JsonValue> entityLegend, MapSpawns spawns) {
    String symbol = String.valueOf(ch);
    JsonValue definition = entityLegend.get(symbol);
    if (definition == null) {
      logger.warn("Unknown entity symbol '{}' in entities layer - ignored", ch);
      return;
    }

    String type = definition.getString("type", "").trim().toUpperCase(Locale.ROOT);
    switch (type) {
      case "PLAYER" -> placePlayerSpawn(x, y, spawns);
      case "ENEMY" ->
          spawns.addEnemy(new SpawnPoint(definition.getString("enemyType", null), x, y));
      case "LOOT" -> spawns.addLoot(new SpawnPoint(definition.getString("lootType", null), x, y));
      default -> logger.warn("Unknown entity type '{}' for symbol '{}'", type, symbol);
    }
  }

  private void placePlayerSpawn(int x, int y, MapSpawns spawns) {
    if (spawns.getPlayer() != null) {
      logger.warn("Multiple player spawn points found; replacing previous player spawn");
    }
    spawns.setPlayer(new GridPoint2(x, y));
  }

  private MapLayerData buildLayer(
      String name, String[] rows, Map<String, TileDefinition> legend, int width, int height) {
    MapLayerData layer = new MapLayerData(name, width, height);
    for (int r = 0; r < rows.length; r++) {
      String row = rows[r];
      int y = height - 1 - r; // flip: text row 0 is the top of the map
      for (int c = 0; c < row.length(); c++) {
        char ch = row.charAt(c);
        if (ch == EMPTY_CELL) {
          continue;
        }
        TileDefinition def = legend.get(String.valueOf(ch));
        if (def == null) {
          logger.warn("Unknown map symbol '{}' in layer '{}' - treated as empty", ch, name);
          continue;
        }
        layer.set(c, y, def);
      }
    }
    return layer;
  }

  private MapSpawns parseSpawns(
      JsonValue spawnsJson, String[] entityRows, Map<String, JsonValue> entityLegend, int height) {

    MapSpawns spawns = new MapSpawns();

    // Existing coordinate-based spawns are still supported.
    if (spawnsJson != null) {
      JsonValue player = spawnsJson.get("player");

      if (player != null) {
        spawns.setPlayer(new GridPoint2(player.getInt("x", 0), player.getInt("y", 0)));
      }

      JsonValue enemies = spawnsJson.get("enemies");

      if (enemies != null) {
        for (JsonValue e = enemies.child; e != null; e = e.next) {

          spawns.addEnemy(
              new SpawnPoint(e.getString("type", null), e.getInt("x", 0), e.getInt("y", 0)));
        }
      }

      JsonValue loot = spawnsJson.get("loot");

      if (loot != null) {
        for (JsonValue l = loot.child; l != null; l = l.next) {

          spawns.addLoot(
              new SpawnPoint(l.getString("type", null), l.getInt("x", 0), l.getInt("y", 0)));
        }
      }
    }

    // New symbol-based spawns.
    if (entityRows != null) {
      parseEntityLayer(entityRows, entityLegend, spawns, height);
    }

    return spawns;
  }

  private List<RoomTransition> parseTransitions(JsonValue transitionsJson, String mapName) {
    List<RoomTransition> transitions = new ArrayList<>();
    if (transitionsJson == null) {
      return transitions;
    }
    if (!transitionsJson.isArray()) {
      throw new MapLoadException("Map '" + mapName + "' 'transitions' must be an array");
    }

    int index = 0;
    for (JsonValue doorway = transitionsJson.child; doorway != null; doorway = doorway.next) {
      String destinationMap = doorway.getString("destinationMap", null);
      if (destinationMap == null || destinationMap.isBlank()) {
        throw new MapLoadException(
            "Transition " + index + " in map '" + mapName + "' has no destinationMap");
      }

      JsonValue destinationSpawnJson = doorway.get("destinationSpawn");
      GridPoint2 destinationSpawn = null;
      if (destinationSpawnJson != null) {
        destinationSpawn =
            new GridPoint2(
                destinationSpawnJson.getInt("x", 0), destinationSpawnJson.getInt("y", 0));
      }

      transitions.add(
          new RoomTransition(
              doorway.getString("id", "transition-" + index),
              new GridPoint2(doorway.getInt("x", 0), doorway.getInt("y", 0)),
              Math.max(1, doorway.getInt("width", 1)),
              Math.max(1, doorway.getInt("height", 1)),
              doorway.getString("texture", null),
              destinationMap,
              destinationSpawn));
      index++;
    }
    return transitions;
  }

  /** Warn (but don't fail) on spawns that fall outside the map bounds. */
  private void validateSpawns(MapSpawns spawns, int width, int height, String mapName) {
    if (width == 0 || height == 0) {
      return;
    }
    if (spawns.getPlayer() != null
        && outOfBounds(spawns.getPlayer().x, spawns.getPlayer().y, width, height)) {
      logger.warn("Player spawn {} is out of bounds in map '{}'", spawns.getPlayer(), mapName);
    }
    for (SpawnPoint sp : spawns.getEnemies()) {
      if (outOfBounds(sp.getX(), sp.getY(), width, height)) {
        logger.warn("Enemy spawn {} is out of bounds in map '{}'", sp, mapName);
      }
    }
    for (SpawnPoint sp : spawns.getLoot()) {
      if (outOfBounds(sp.getX(), sp.getY(), width, height)) {
        logger.warn("Loot spawn {} is out of bounds in map '{}'", sp, mapName);
      }
    }
  }

  /** Warn (but don't fail) when a doorway is outside the source map. */
  private void validateTransitions(
      List<RoomTransition> transitions, int width, int height, String mapName) {
    for (RoomTransition transition : transitions) {
      GridPoint2 position = transition.getPosition();
      if (outOfBounds(position.x, position.y, width, height)) {
        logger.warn("Transition '{}' is out of bounds in map '{}'", transition.getId(), mapName);
      }
    }
  }

  private static boolean outOfBounds(int x, int y, int width, int height) {
    return x < 0 || x >= width || y < 0 || y >= height;
  }
}
