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

    String[] entityRows = null;

    for (int i = 0; i < layerNames.size(); i++) {

      String layerName = layerNames.get(i);

      if (layerName.equals("entities")) {
        entityRows = layerRows.get(i);
        continue;
      }

      layers.add(buildLayer(layerName, layerRows.get(i), legend, width, height));
    }

    MapSpawns spawns = parseSpawns(root.get("spawns"), entityRows, entityLegend, width, height);

    validateSpawns(spawns, width, height, name);

    return new LevelMapData(name, tileSize, width, height, legend, layers, spawns);
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
      String[] rows, Map<String, JsonValue> entityLegend, MapSpawns spawns, int width, int height) {

    for (int r = 0; r < rows.length; r++) {

      String row = rows[r];

      // Same coordinate conversion used by buildLayer().
      int y = height - 1 - r;

      for (int x = 0; x < row.length(); x++) {

        char ch = row.charAt(x);

        if (ch == EMPTY_CELL) {
          continue;
        }

        String symbol = String.valueOf(ch);
        JsonValue definition = entityLegend.get(symbol);

        if (definition == null) {
          logger.warn("Unknown entity symbol '{}' in entities layer - ignored", ch);
          continue;
        }

        String type = definition.getString("type", "").trim().toUpperCase(Locale.ROOT);

        switch (type) {
          case "PLAYER":
            if (spawns.getPlayer() != null) {
              logger.warn("Multiple player spawn points found; replacing previous player spawn");
            }

            spawns.setPlayer(new GridPoint2(x, y));
            break;

          case "ENEMY":
            String enemyType = definition.getString("enemyType", null);

            spawns.addEnemy(new SpawnPoint(enemyType, x, y));
            break;

          case "LOOT":
            String lootType = definition.getString("lootType", null);

            spawns.addLoot(new SpawnPoint(lootType, x, y));
            break;

          default:
            logger.warn("Unknown entity type '{}' for symbol '{}'", type, symbol);
        }
      }
    }
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

  private MapSpawns parseSpawns(
      JsonValue spawnsJson,
      String[] entityRows,
      Map<String, JsonValue> entityLegend,
      int width,
      int height) {

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
      parseEntityLayer(entityRows, entityLegend, spawns, width, height);
    }

    return spawns;
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

  private static boolean outOfBounds(int x, int y, int width, int height) {
    return x < 0 || x >= width || y < 0 || y >= height;
  }
}
