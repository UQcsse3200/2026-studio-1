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
 *     "#": { "type": "WALL",  "texture": "images/grass_3.png" },
 *     ".": { "type": "FLOOR", "texture": "images/grass_1.png" }
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
 */
public class JsonMapLoader implements MapLoader {
  private static final Logger logger = LoggerFactory.getLogger(JsonMapLoader.class);
  private static final float DEFAULT_TILE_SIZE = 0.5f;
  private static final char EMPTY_CELL = ' ';
  private static final String LEVEL_TWO_BACKGROUND = "images/level2/level2-map.png";

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

    JsonValue layersJson = root.get("layers");
    if (layersJson == null) {
      if (root.get("tiles") != null) {
        return parseAuthoredLevel(root);
      }
      throw new MapLoadException("Map '" + name + "' has no 'layers' section");
    }
    if (!layersJson.isObject()) {
      throw new MapLoadException("Map '" + name + "' 'layers' must be a JSON object");
    }

    // First pass: collect raw rows and compute overall dimensions.
    List<String> layerNames = new ArrayList<>();
    List<String[]> layerRows = new ArrayList<>();
    int width = 0;
    int height = 0;
    for (JsonValue layer = layersJson.child; layer != null; layer = layer.next) {
      if (!layer.isArray()) {
        throw new MapLoadException(
            "Layer '" + layer.name + "' in map '" + name + "' must be an array of strings");
      }
      String[] rows = layer.asStringArray();
      layerNames.add(layer.name);
      layerRows.add(rows);
      height = Math.max(height, rows.length);
      for (String row : rows) {
        width = Math.max(width, row.length());
      }
    }

    // Second pass: build the typed tile grids, flipping rows so y=0 is the bottom.
    List<MapLayerData> layers = new ArrayList<>();
    for (int i = 0; i < layerNames.size(); i++) {
      layers.add(buildLayer(layerNames.get(i), layerRows.get(i), legend, width, height));
    }

    MapSpawns spawns = parseSpawns(root.get("spawns"));
    List<RoomTransition> transitions = parseTransitions(root.get("transitions"), name);
    validateSpawns(spawns, width, height, name);
    validateTransitions(transitions, width, height, name);

    return new LevelMapData(name, tileSize, width, height, legend, layers, spawns, transitions);
  }

  /**
   * Adapts the Level 2 art team's concise blueprint format to the runtime map representation. Its
   * {@code tiles} rows are still used directly for collision; the provided composed artwork is
   * rendered as a single background so the intended mountain scene is preserved exactly.
   */
  private LevelMapData parseAuthoredLevel(JsonValue root) {
    String name = root.getString("name", "unnamed");
    JsonValue tilesJson = root.get("tiles");
    if (!tilesJson.isArray()) {
      throw new MapLoadException("Authored map '" + name + "' tiles must be an array of strings");
    }

    String[] rows = tilesJson.asStringArray();
    int width = root.getInt("width", widestRow(rows));
    int height = root.getInt("height", rows.length);
    if (width <= 0 || height <= 0 || rows.length != height || widestRow(rows) > width) {
      throw new MapLoadException("Authored map '" + name + "' has inconsistent dimensions");
    }

    Map<String, TileDefinition> legend = authoredLegend();
    MapLayerData collision = buildLayer("collision", rows, legend, width, height);
    MapLayerData hazards = buildStormHazardLayer(rows, width, height);
    MapSpawns spawns = parseAuthoredSpawns(root.get("entry"), root.get("objects"), hazards, height);
    List<RoomTransition> transitions = parseAuthoredTransitions(root.get("exit"), height);

    validateSpawns(spawns, width, height, name);
    validateTransitions(transitions, width, height, name);
    return new LevelMapData(
        name,
        DEFAULT_TILE_SIZE,
        width,
        height,
        legend,
        List.of(collision, hazards),
        spawns,
        transitions,
        root.getString("backgroundTexture", LEVEL_TWO_BACKGROUND));
  }

  private static int widestRow(String[] rows) {
    int width = 0;
    for (String row : rows) {
      width = Math.max(width, row.length());
    }
    return width;
  }

  private static Map<String, TileDefinition> authoredLegend() {
    Map<String, TileDefinition> legend = new HashMap<>();
    // Soil, marble, granite, and the summit are solid mountain geometry. Shelves and clouds are
    // one-way surfaces, so the authored three-cell jumps remain reachable from below.
    for (String symbol : List.of("O", "E", "Q", "R", "g", "S", "I")) {
      legend.put(symbol, new TileDefinition(TileType.WALL, null));
    }
    for (String symbol : List.of("K", "c", "d", "f", "t", "m")) {
      legend.put(symbol, new TileDefinition(TileType.PLATFORM, null));
    }
    legend.put("P", new TileDefinition(TileType.DECORATIVE, null));
    return legend;
  }

  private static MapLayerData buildStormHazardLayer(String[] rows, int width, int height) {
    MapLayerData hazards = new MapLayerData("hazards", width, height);
    TileDefinition storm = new TileDefinition(TileType.HAZARD, null);
    for (int row = 0; row < rows.length; row++) {
      for (int x = 0; x < rows[row].length(); x++) {
        if (rows[row].charAt(x) == 'm') {
          hazards.set(x, height - 1 - row, storm);
        }
      }
    }
    return hazards;
  }

  private static MapSpawns parseAuthoredSpawns(
      JsonValue entryJson, JsonValue objectsJson, MapLayerData hazards, int height) {
    MapSpawns spawns = new MapSpawns();
    if (entryJson != null) {
      spawns.setPlayer(
          new GridPoint2(
              entryJson.getInt("x", 0), authoredYToWorld(entryJson.getInt("y", 0), height)));
    }
    if (objectsJson == null) {
      return spawns;
    }

    TileDefinition hazard = new TileDefinition(TileType.HAZARD, null);
    for (JsonValue object = objectsJson.child; object != null; object = object.next) {
      String id = object.getString("id", "");
      int x = object.getInt("x", 0);
      int y = authoredYToWorld(object.getInt("y", 0), height);
      switch (id) {
        case "enemy-skeleton-hoplite" -> spawns.addEnemy(new SpawnPoint("skeleton", x, y));
        // The current combat roster has no centaur/cyclops classes. These map to the existing
        // ranged skeleton and boss respectively, retaining working combat at authored locations.
        case "enemy-centaur" -> spawns.addEnemy(new SpawnPoint("ranged-skeleton", x, y));
        case "enemy-cyclops" -> spawns.addEnemy(new SpawnPoint("ghostKing", x, y));
        case "item-bow-artemis",
            "item-bronze-spear",
            "item-cloud-flask",
            "item-aegis-fragment",
            "item-olive-branch",
            "item-laurel" ->
            spawns.addLoot(new SpawnPoint(id, x, y));
        case "hazard-rockslide", "hazard-falling-column" -> hazards.set(x, y, hazard);
        default -> {
          // Decorative authored objects are represented by the composed map artwork.
        }
      }
    }
    return spawns;
  }

  private static List<RoomTransition> parseAuthoredTransitions(JsonValue exitJson, int height) {
    if (exitJson == null) {
      return List.of();
    }
    return List.of(
        new RoomTransition(
            "mountain-summit-to-level-3",
            new GridPoint2(
                exitJson.getInt("x", 0), authoredYToWorld(exitJson.getInt("y", 0), height)),
            2,
            2,
            null,
            "maps/level3.json",
            new GridPoint2(2, 2)));
  }

  /** The art blueprint uses image coordinates (top-left origin); runtime maps use bottom-left. */
  private static int authoredYToWorld(int authoredY, int height) {
    return height - 1 - authoredY;
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
        } else {
          layer.set(c, y, def);
        }
      }
    }
    return layer;
  }

  private MapSpawns parseSpawns(JsonValue spawnsJson) {
    MapSpawns spawns = new MapSpawns();
    if (spawnsJson == null) {
      return spawns;
    }

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
