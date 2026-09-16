package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.GridPoint2;
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
  void placesSpawnsFromTheEntitiesLayer() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": {
            "P": { "type": "PLAYER" },
            "S": { "type": "ENEMY", "enemyType": "skeleton" },
            "L": { "type": "LOOT" }
          },
          "layers": {
            "terrain":  ["####", "####"],
            "entities": ["  S ", "P  L"]
          }
        }
        """;
    LevelMapData map = loader.parse(json);

    // Rows are flipped, so the bottom row of the entities layer is y = 0.
    assertEquals(new GridPoint2(0, 0), map.getSpawns().getPlayer());
    assertEquals(1, map.getSpawns().getEnemies().size());
    assertEquals("skeleton", map.getSpawns().getEnemies().getFirst().getType());
    assertEquals(new GridPoint2(2, 1), map.getSpawns().getEnemies().getFirst().getPosition());
    assertEquals(new GridPoint2(3, 0), map.getSpawns().getLoot().getFirst().getPosition());
    // The entities layer is spawn data, not a tile layer.
    assertNull(map.getLayer("entities"));
    assertEquals(1, map.getLayers().size());
  }

  @Test
  void throwsWhenEntitiesLayerDoesNotMatchTheMapGrid() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": { "P": { "type": "PLAYER" } },
          "layers": {
            "terrain":  ["####", "####", "####"],
            "entities": ["P   "]
          }
        }
        """;
    assertThrows(MapLoadException.class, () -> loader.parse(json));
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
  void parsesRoomTransitions() {
    String json =
        """
        {
          "legend": {},
          "layers": { "terrain": ["    ", "    "] },
          "transitions": [
            {
              "id": "to-underworld",
              "x": 2,
              "y": 1,
              "width": 2,
              "height": 3,
              "texture": "door.png",
              "destinationMap": "maps/room2.json",
              "destinationSpawn": { "x": 4, "y": 5 }
            }
          ]
        }
        """;

    LevelMapData map = loader.parse(json);
    RoomTransition transition = map.getTransitions().getFirst();

    assertEquals("to-underworld", transition.getId());
    assertEquals(new com.badlogic.gdx.math.GridPoint2(2, 1), transition.getPosition());
    assertEquals(2, transition.getWidth());
    assertEquals(3, transition.getHeight());
    assertEquals("door.png", transition.getTexture());
    assertEquals("maps/room2.json", transition.getDestinationMap());
    assertEquals(new com.badlogic.gdx.math.GridPoint2(4, 5), transition.getDestinationSpawn());
    assertTrue(map.getTexturePaths().contains("door.png"));
  }

  @Test
  void rejectsTransitionWithoutDestinationMap() {
    String json =
        """
        {
          "legend": {},
          "layers": { "terrain": [" "] },
          "transitions": [ { "x": 0, "y": 0 } ]
        }
        """;

    assertThrows(MapLoadException.class, () -> loader.parse(json));
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
  void loadsRoomOneDoorwayAndTemporaryRoomTwo() {
    LevelMapData roomOne = loader.load("maps/demo.json");
    LevelMapData roomTwo = loader.load("maps/room2.json");

    assertEquals("Underworld Dungeon", roomOne.getName());
    assertEquals(40, roomOne.getWidth());
    assertEquals(66, roomOne.getHeight());
    assertEquals(new GridPoint2(8, 63), roomOne.getSpawns().getPlayer());
    assertEquals(TileType.WALL, roomOne.getTileType(3, 0));
    assertEquals(TileType.PLATFORM, roomOne.getTileType(20, 15));
    assertEquals(TileType.PLATFORM, roomOne.getTileType(35, 5));
    assertEquals(TileType.PLATFORM, roomOne.getTileType(4, 20));
    assertEquals(TileType.PLATFORM, roomOne.getTileType(20, 60));
    assertEquals(TileType.PLATFORM, roomOne.getTileType(26, 30));
    assertEquals(31, roomOne.getSpawns().getEnemies().get(1).getY());
    assertEquals(1, roomOne.getTransitions().size());
    assertEquals("maps/room2.json", roomOne.getTransitions().getFirst().getDestinationMap());
    assertEquals(new GridPoint2(34, 61), roomOne.getTransitions().getFirst().getPosition());
    assertEquals("Underworld (Temporary)", roomTwo.getName());
    assertEquals(40, roomTwo.getWidth());
    assertEquals(22, roomTwo.getHeight());
  }

  @Test
  void loadsGreekLevelOneDesignMap() {
    LevelMapData levelOne = loader.load("maps/level1-greek.json");

    assertEquals(56, levelOne.getWidth());
    assertEquals(64, levelOne.getHeight());
    assertEquals(new GridPoint2(3, 3), levelOne.getSpawns().getPlayer());
    assertEquals(4, levelOne.getSpawns().getEnemies().size());
    assertEquals(TileType.LADDER, levelOne.getTileType(6, 6));
    // Transparent ladders and ledges must render over a background rather than the clear colour.
    assertNotNull(levelOne.getLayer("background"));
    assertEquals(TileType.DECORATIVE, levelOne.getLayer("background").get(26, 33).type());
    // The Nether endpoint keeps the ladder passage open beside its solid marble landing.
    assertEquals(TileType.LADDER, levelOne.getTileType(26, 33));
    assertEquals(TileType.PLATFORM, levelOne.getTileType(27, 33));
    assertEquals(TileType.PLATFORM, levelOne.getTileType(26, 22));
    // The dungeon ladder is continuous through the former gate-block obstruction.
    for (int y = 18; y <= 21; y++) {
      assertEquals(TileType.LADDER, levelOne.getTileType(30, y));
    }
    // The blue Styx hazards beside the Nether entrance are regular walkable floor tiles.
    assertEquals(TileType.FLOOR, levelOne.getTileType(14, 33));
    assertEquals(TileType.FLOOR, levelOne.getTileType(25, 33));
    assertEquals(TileType.FLOOR, levelOne.getTileType(28, 33));
    assertEquals(TileType.FLOOR, levelOne.getTileType(32, 33));
    assertEquals(TileType.HAZARD, levelOne.getTileType(8, 12));
    assertEquals(
        "images/level1/hazard-spikes-bronze-512px.png",
        levelOne.getCollisionLayer().get(8, 12).texture());
    assertEquals(TileType.WALL, levelOne.getTileType(5, 0));
    assertEquals(1, levelOne.getTransitions().size());
    assertEquals("maps/level2.json", levelOne.getTransitions().getFirst().getDestinationMap());
    // Level 1 declares a composed background, but the artwork has not been supplied yet, so the
    // map still loads and renders from its tile layers.
    assertNull(levelOne.getBackgroundTexture());
  }

  @Test
  void loadsLevelTwoMountainAndItsSummitExit() {
    LevelMapData levelTwo = loader.load("maps/level2.json");

    assertEquals("Level 2 — Climb Mount Olympus", levelTwo.getName());
    assertEquals(80, levelTwo.getWidth());
    assertEquals(180, levelTwo.getHeight());
    assertEquals(new GridPoint2(3, 2), levelTwo.getSpawns().getPlayer());
    assertEquals(TileType.WALL, levelTwo.getTileType(1, 1));
    assertEquals(TileType.PLATFORM, levelTwo.getTileType(27, 155));
    // Storm clouds stay walkable; only explicitly authored hazards damage the player.
    assertEquals(TileType.PLATFORM, levelTwo.getTileType(27, 143));
    assertEquals(TileType.PLATFORM, levelTwo.getTileType(31, 131));
    assertNull(levelTwo.getTileType(42, 75));
    // Hazards live in the collision layer, as they do in level 1.
    assertNull(levelTwo.getLayer("hazards"));
    assertEquals(TileType.HAZARD, levelTwo.getTileType(13, 45));
    assertEquals(TileType.DECORATIVE, levelTwo.getTileType(59, 173));
    assertEquals(TileType.WALL, levelTwo.getTileType(59, 165));
    assertTrue(
        levelTwo.getSpawns().getEnemies().stream()
            .anyMatch(
                spawn ->
                    "skeleton".equals(spawn.getType())
                        && spawn.getPosition().equals(new GridPoint2(43, 147))));
    assertEquals(6, levelTwo.getSpawns().getEnemies().size());
    assertEquals(6, levelTwo.getSpawns().getLoot().size());
    assertEquals("maps/level3.json", levelTwo.getTransitions().getFirst().getDestinationMap());
    assertEquals(new GridPoint2(67, 166), levelTwo.getTransitions().getFirst().getPosition());
    assertEquals("images/level2/level2-map.png", levelTwo.getBackgroundTexture());

    LevelMapData levelThree = loader.load("maps/level3.json");
    assertEquals(new GridPoint2(2, 2), levelThree.getSpawns().getPlayer());
  }

  @Test
  void loadsLevelThreeThroneRoomWithASingleBoss() {
    LevelMapData levelThree = loader.load("maps/level3.json");

    assertEquals("Level 3 — Zeus's Palace", levelThree.getName());
    assertEquals(40, levelThree.getWidth());
    assertEquals(16, levelThree.getHeight());
    // The player arrives on the spawn level 2's summit exit sends them to.
    assertEquals(new GridPoint2(2, 2), levelThree.getSpawns().getPlayer());
    assertEquals(1, levelThree.getSpawns().getEnemies().size());
    assertEquals("ghostking", levelThree.getSpawns().getEnemies().getFirst().getType());
    assertEquals(
        new GridPoint2(32, 2), levelThree.getSpawns().getEnemies().getFirst().getPosition());
    // Only the shell collides: the throne room's furniture is all walk-through decoration.
    for (MapLayerData layer : levelThree.getLayers()) {
      for (int x = 0; x < levelThree.getWidth(); x++) {
        for (int y = 0; y < levelThree.getHeight(); y++) {
          TileDefinition tile = layer.get(x, y);
          if (tile != null && tile.type() != TileType.WALL) {
            assertEquals(TileType.DECORATIVE, tile.type());
          }
        }
      }
    }
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
