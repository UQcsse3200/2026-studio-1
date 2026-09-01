package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class JsonMapLoaderTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  // ---------- parse(): happy paths ----------

  @Test
  void parsesValidContent() {
    String json =
        """
        {
          "name": "My Level",
          "tileSize": 1.5,
          "legend": { "#": { "type": "WALL", "texture": "a.png" } },
          "layers": { "terrain": ["##", "# "] },
          "spawns": { "player": { "x": 1, "y": 0 } }
        }
        """;
    LevelMapData map = loader.parse(json);

    assertEquals("My Level", map.getName());
    assertEquals(1.5f, map.getTileSize());
    assertEquals(2, map.getWidth());
    assertEquals(2, map.getHeight());
    assertEquals(1, map.getLayers().size());
    assertEquals(TileType.WALL, map.getTileType(0, 0));
  }

  @Test
  void usesDefaultsWhenNameAndTileSizeMissing() {
    String json =
        """
        { "layers": { "terrain": ["#"] }, "legend": { "#": { "type": "WALL" } } }
        """;
    LevelMapData map = loader.parse(json);

    assertEquals("unnamed", map.getName());
    assertEquals(0.5f, map.getTileSize());
  }

  @Test
  void emptyLegendWhenNoLegendSection() {
    LevelMapData map = loader.parse("{ \"layers\": { \"terrain\": [\"##\"] } }");
    assertTrue(map.getLegend().isEmpty());
    // unknown symbols with no legend become empty cells
    assertNull(map.getTileType(0, 0));
  }

  @Test
  void defaultsTileTypeToDecorativeWhenTypeMissing() {
    String json =
        """
        { "legend": { "x": { "texture": "a.png" } }, "layers": { "terrain": ["x"] } }
        """;
    LevelMapData map = loader.parse(json);
    assertEquals(TileType.DECORATIVE, map.getTileType(0, 0));
  }

  // ---------- parse(): layers, flipping, dimensions ----------

  @Test
  void flipsRowsSoTopRowIsHighestY() {
    String json =
        """
        {
          "legend": {
            "A": { "type": "WALL" },
            "B": { "type": "PLATFORM" },
            "C": { "type": "HAZARD" }
          },
          "layers": { "terrain": ["A", "B", "C"] }
        }
        """;
    LevelMapData map = loader.parse(json);
    MapLayerData terrain = map.getLayer("terrain");

    assertEquals(TileType.WALL, terrain.get(0, 2).type()); // top row -> highest y
    assertEquals(TileType.PLATFORM, terrain.get(0, 1).type());
    assertEquals(TileType.HAZARD, terrain.get(0, 0).type()); // bottom row -> y=0
  }

  @Test
  void widthIsMaxRowLength() {
    String json =
        """
        { "legend": { "#": { "type": "WALL" } }, "layers": { "terrain": ["#", "###"] } }
        """;
    LevelMapData map = loader.parse(json);
    assertEquals(3, map.getWidth());
    assertEquals(2, map.getHeight());
  }

  @Test
  void skipsSpacesAndUnknownSymbols() {
    String json =
        """
        { "legend": { "#": { "type": "WALL" } }, "layers": { "terrain": ["# ?"] } }
        """;
    LevelMapData map = loader.parse(json);
    MapLayerData terrain = map.getLayer("terrain");

    assertNotNull(terrain.get(0, 0)); // '#'
    assertNull(terrain.get(1, 0)); // space
    assertNull(terrain.get(2, 0)); // unknown '?'
  }

  @Test
  void emptyMapWhenLayersEmpty() {
    LevelMapData map = loader.parse("{ \"layers\": {} }");
    assertTrue(map.isEmpty());
    assertEquals(0, map.getWidth());
    assertEquals(0, map.getHeight());
  }

  // ---------- parse(): spawns ----------

  @Test
  void parsesPlayerEnemyAndLootSpawns() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##", "##"] },
          "spawns": {
            "player": { "x": 1, "y": 1 },
            "enemies": [ { "type": "ghost", "x": 0, "y": 1 } ],
            "loot": [ { "x": 1, "y": 0 } ]
          }
        }
        """;
    MapSpawns spawns = loader.parse(json).getSpawns();

    assertEquals(1, spawns.getPlayer().x);
    assertEquals(1, spawns.getPlayer().y);
    assertEquals(1, spawns.getEnemies().size());
    assertEquals("ghost", spawns.getEnemies().get(0).getType());
    assertEquals(1, spawns.getLoot().size());
  }

  @Test
  void emptySpawnsWhenNoSpawnsSection() {
    MapSpawns spawns =
        loader.parse("{ \"legend\": {}, \"layers\": { \"t\": [\"#\"] } }").getSpawns();
    assertNull(spawns.getPlayer());
    assertTrue(spawns.getEnemies().isEmpty());
    assertTrue(spawns.getLoot().isEmpty());
  }

  @Test
  void allowsOutOfBoundsSpawnsWithoutThrowing() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["#"] },
          "spawns": {
            "player": { "x": 99, "y": 99 },
            "enemies": [ { "type": "ghost", "x": -1, "y": 0 } ],
            "loot": [ { "x": 5, "y": 5 } ]
          }
        }
        """;
    MapSpawns spawns = loader.parse(json).getSpawns();
    assertEquals(99, spawns.getPlayer().x);
    assertEquals(1, spawns.getEnemies().size());
  }

  // ---------- parse(): error handling ----------

  @Test
  void throwsOnMalformedJson() {
    assertThrows(MapLoadException.class, () -> loader.parse("{ \"layers\": [ }"));
  }

  @Test
  void throwsWhenRootIsNotObject() {
    assertThrows(MapLoadException.class, () -> loader.parse("\"just a string\""));
  }

  @Test
  void throwsWhenContentIsEmpty() {
    assertThrows(MapLoadException.class, () -> loader.parse(""));
  }

  @Test
  void throwsWhenNoLayersSection() {
    assertThrows(MapLoadException.class, () -> loader.parse("{ \"name\": \"x\" }"));
  }

  @Test
  void throwsWhenLayersNotObject() {
    assertThrows(MapLoadException.class, () -> loader.parse("{ \"layers\": [\"##\"] }"));
  }

  @Test
  void throwsWhenLayerNotArray() {
    assertThrows(
        MapLoadException.class,
        () -> loader.parse("{ \"layers\": { \"terrain\": { \"a\": 1 } } }"));
  }

  @Test
  void throwsOnUnknownTileType() {
    String json =
        """
        { "legend": { "#": { "type": "NONSENSE" } }, "layers": { "terrain": ["#"] } }
        """;
    assertThrows(MapLoadException.class, () -> loader.parse(json));
  }

  // ---------- load(): file access ----------

  @Test
  void loadsValidFileFromAssets() {
    LevelMapData map = loader.load("test/files/test_map.json");
    assertEquals("Test Map", map.getName());
    assertEquals(2, map.getWidth());
    assertEquals(2, map.getHeight());
    assertEquals(2, map.getLayers().size());
    assertEquals(TileType.WALL, map.getTileType(0, 0));
    assertEquals(2, map.getTexturePaths().size());
    assertEquals("ghost", map.getSpawns().getEnemies().get(0).getType());
  }

  @Test
  void throwsWhenFileMissing() {
    assertThrows(MapLoadException.class, () -> loader.load("maps/does_not_exist.json"));
  }

  @Test
  void throwsWhenFilesBackendUnavailable() {
    Files original = Gdx.files;
    try {
      Gdx.files = null;
      assertThrows(MapLoadException.class, () -> loader.load("anything.json"));
    } finally {
      Gdx.files = original;
    }
  }

  @Test
  void throwsWhenPathIsUnreadable() {
    // A directory exists() but cannot be read as a string -> read failure branch.
    assertThrows(MapLoadException.class, () -> loader.load("test/files"));
  }
}
