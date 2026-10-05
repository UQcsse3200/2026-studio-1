package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
  void carriesExtraLegendKeysAsTileProperties() {
    String json =
        """
        {
          "legend": {
            "~": { "type": "HAZARD", "texture": "lava.png", "damage": "15", "hidden": "true" },
            "#": { "type": "WALL", "texture": "wall.png" }
          },
          "layers": { "terrain": ["~#"] }
        }
        """;
    LevelMapData map = loader.parse(json);

    TileDefinition hazard = map.getLegend().get("~");
    assertEquals(15, hazard.getInt("damage", 0));
    assertTrue(hazard.flag("hidden"));
    // type and texture stay out of the property bag.
    assertFalse(hazard.has("type"));
    assertFalse(hazard.has("texture"));
    assertTrue(map.getLegend().get("#").properties().isEmpty());
  }

  @Test
  void placesMarkersFromTheEntitiesLayer() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": {
            "N": { "type": "MARKER", "kind": "npc", "id": "traveler" },
            "T": { "type": "MARKER", "kind": "light", "radius": "6" },
            "C": { "type": "MARKER", "kind": "CHECKPOINT" }
          },
          "layers": {
            "terrain":  ["####", "####"],
            "entities": ["T  C", "N   "]
          }
        }
        """;
    MapSpawns spawns = loader.parse(json).getSpawns();

    Marker npc = spawns.getMarkers("npc").getFirst();
    assertEquals("traveler", npc.id());
    assertEquals(new GridPoint2(0, 0), npc.position());

    Marker light = spawns.getMarkers("light").getFirst();
    assertEquals(6, light.getInt("radius", 0));
    assertNull(light.id());
    // kind, id and type stay out of the property bag.
    assertFalse(light.properties().containsKey("kind"));
    assertFalse(light.properties().containsKey("type"));

    // Kinds are matched ignoring case, so map authors can write them however they like.
    assertEquals(1, spawns.getMarkers("checkpoint").size());
    assertEquals(3, spawns.getAllMarkers().size());
  }

  @Test
  void ignoresAMarkerWithNoKind() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": { "X": { "type": "MARKER", "id": "nameless" } },
          "layers": { "terrain": ["##"], "entities": ["X "] }
        }
        """;
    assertTrue(loader.parse(json).getSpawns().getAllMarkers().isEmpty());
  }

  @Test
  void markersDoNotDisturbPlayerEnemyOrLootSpawns() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": {
            "P": { "type": "PLAYER" },
            "S": { "type": "ENEMY", "enemyType": "skeleton" },
            "L": { "type": "LOOT" },
            "N": { "type": "MARKER", "kind": "npc" }
          },
          "layers": { "terrain": ["####"], "entities": ["PSLN"] }
        }
        """;
    MapSpawns spawns = loader.parse(json).getSpawns();

    assertEquals(new GridPoint2(0, 0), spawns.getPlayer());
    assertEquals(1, spawns.getEnemies().size());
    assertEquals(1, spawns.getLoot().size());
    assertEquals(1, spawns.getMarkers("npc").size());
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
              "destinationMap": "maps/level2.json",
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
    assertEquals("maps/level2.json", transition.getDestinationMap());
    assertEquals(new com.badlogic.gdx.math.GridPoint2(4, 5), transition.getDestinationSpawn());
    assertTrue(map.getTexturePaths().contains("door.png"));
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

  static Stream<Arguments> structurallyInvalidMaps() {
    return Stream.of(
        Arguments.of(
            "entities layer does not match the map grid",
            """
            {
              "legend": { "#": { "type": "WALL" } },
              "entityLegend": { "P": { "type": "PLAYER" } },
              "layers": {
                "terrain":  ["####", "####", "####"],
                "entities": ["P   "]
              }
            }
            """),
        Arguments.of(
            "transition has no destination map",
            """
            {
              "legend": {},
              "layers": { "terrain": [" "] },
              "transitions": [ { "x": 0, "y": 0 } ]
            }
            """),
        Arguments.of(
            "legend uses an unknown tile type",
            """
            { "legend": { "#": { "type": "NONSENSE" } }, "layers": { "terrain": ["#"] } }
            """));
  }

  @ParameterizedTest(name = "rejects a map where the {0}")
  @MethodSource("structurallyInvalidMaps")
  void rejectsStructurallyInvalidMaps(String problem, String json) {
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
  void loadsGreekLevelOneDesignMap() {
    LevelMapData levelOne = loader.load("maps/level1-greek.json");

    assertEquals(56, levelOne.getWidth());
    assertEquals(64, levelOne.getHeight());
    assertEquals(new GridPoint2(3, 3), levelOne.getSpawns().getPlayer());
    assertEquals(12, levelOne.getSpawns().getEnemies().size());
    assertEquals(TileType.LADDER, levelOne.getTileType(6, 6));
    // Transparent ladders and ledges render over the parallax backdrops, so the background layer
    // is kept for parity with the other levels but holds no tiles.
    assertNotNull(levelOne.getLayer("background"));
    assertNull(levelOne.getLayer("background").get(26, 33));
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
    // Level 1 is drawn from its tiles, with no composed image behind them.
    assertNull(levelOne.getBackgroundTexture());
    // The dungeon and the Nether each draw their own parallax backdrop instead of background tiles.
    assertEquals(4, levelOne.getBackdrop("dungeon").size());
    assertEquals(4, levelOne.getBackdrop("nether").size());
  }

  @Test
  void readsBackdropLayersForASubLevel() {
    String json =
        """
        {
          "name": "Cavern",
          "legend": { "#": { "type": "WALL", "texture": "wall.png" } },
          "layers": { "terrain": ["#"] },
          "backdrops": {
            "cave": [
              { "texture": "far.png" },
              { "texture": "fog.png", "scroll": 0.6, "drift": { "x": 0.15, "y": 0.4 } }
            ],
            "*": [ { "texture": "sky.png", "spansMap": true } ]
          }
        }
        """;

    LevelMapData map = loader.parse(json);

    assertEquals(
        List.of(
            new BackdropLayer("far.png", 0f, 0f, 0f),
            new BackdropLayer("fog.png", 0.6f, 0.15f, 0.4f)),
        map.getBackdrop("cave"));
    // Anywhere without a backdrop of its own falls back to the map-wide one.
    List<BackdropLayer> wholeMap = List.of(new BackdropLayer("sky.png", 0f, 0f, 0f, true));
    assertEquals(wholeMap, map.getBackdrop("elsewhere"));
    assertEquals(wholeMap, map.getBackdrop(null));
    assertTrue(
        map.getTexturePaths().containsAll(List.of("wall.png", "far.png", "fog.png", "sky.png")));
  }

  @Test
  void readsOverlaysAndTheRowsTheyAreSeenIn() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["#"] },
          "overlays": {
            "*": [ { "texture": "rain.png", "scroll": 1, "rows": { "from": 10, "to": 20 } } ]
          }
        }
        """;

    LevelMapData map = loader.parse(json);

    BackdropLayer rain = map.getOverlay(null).getFirst();
    assertEquals(new BackdropLayer.Rows(10, 20), rain.rows());
    assertEquals(1f, rain.visibilityAt(15f));
    assertEquals(0.5f, rain.visibilityAt(24f));
    assertEquals(0f, rain.visibilityAt(40f));
    assertTrue(map.getBackdrop(null).isEmpty());
    assertTrue(map.getTexturePaths().contains("rain.png"));
  }

  @Test
  void loadsTheLightSpriteOfAGlowingTile() {
    String json =
        """
        {
          "legend": { "L": { "type": "DECORATIVE", "texture": "lamp.png", "glow": "light.png" } },
          "layers": { "terrain": ["L"] }
        }
        """;

    LevelMapData map = loader.parse(json);

    assertTrue(map.getTexturePaths().containsAll(List.of("lamp.png", "light.png")));
  }

  @Test
  void rejectsABackdropLayerWithNoTexture() {
    String json =
        """
        {
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["#"] },
          "backdrops": { "cave": [ { "scroll": 0.5 } ] }
        }
        """;

    assertThrows(MapLoadException.class, () -> loader.parse(json));
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
    assertEquals("images/level2/level2-foreground.png", levelTwo.getBackgroundTexture());
    // One sky spans the whole climb, so both sub-levels share the map-wide backdrop.
    assertEquals(4, levelTwo.getBackdrop("base").size());
    assertEquals(levelTwo.getBackdrop("base"), levelTwo.getBackdrop("skies"));
    assertTrue(levelTwo.getBackdrop("skies").getFirst().spansMap());
    // Rain falls only around the storm clouds near the top of the climb.
    assertEquals(new BackdropLayer.Rows(128, 160), levelTwo.getOverlay("skies").getFirst().rows());

    LevelMapData levelThree = loader.load("maps/level3.json");
    assertEquals(
        levelThree.getSpawns().getPlayer(),
        levelTwo.getTransitions().getFirst().getDestinationSpawn());
  }

  @Test
  void loadsLevelThreeThroneRoomWithASingleBoss() {
    LevelMapData levelThree = loader.load("maps/level3.json");

    assertEquals("Level 3 — Zeus's Palace", levelThree.getName());
    assertEquals(52, levelThree.getWidth());
    assertEquals(26, levelThree.getHeight());
    // The player arrives on the spawn level 2's summit exit sends them to.
    assertEquals(new GridPoint2(8, 3), levelThree.getSpawns().getPlayer());
    assertEquals(1, levelThree.getSpawns().getEnemies().size());
    assertEquals("zeus", levelThree.getSpawns().getEnemies().getFirst().getType());
    assertEquals(
        new GridPoint2(26, 3), levelThree.getSpawns().getEnemies().getFirst().getPosition());
    assertEquals(5, levelThree.getSpawns().getLoot().size());

    // The throne balcony is a platform over Zeus's spot, and ladders climb both walls.
    assertEquals(TileType.PLATFORM, levelThree.getTileType(26, 18));
    assertEquals(TileType.LADDER, levelThree.getTileType(3, 5));
    assertEquals(TileType.LADDER, levelThree.getTileType(48, 5));
    // The throne room's furniture is all walk-through decoration, kept out of the collision layer.
    MapLayerData furniture = levelThree.getLayer("background");
    for (int x = 0; x < levelThree.getWidth(); x++) {
      for (int y = 0; y < levelThree.getHeight(); y++) {
        TileDefinition tile = furniture.get(x, y);
        if (tile != null) {
          assertEquals(TileType.DECORATIVE, tile.type());
        }
      }
    }
  }

  @Test
  void levelThreeChargedFloorIsSolidGroundUnderAHazard() {
    LevelMapData levelThree = loader.load("maps/level3.json");

    MapLayerData hazards = levelThree.getLayer("hazards");
    for (int x : new int[] {10, 11, 40, 41}) {
      // A hazard is a sensor, so the strip needs real floor beneath it to be stood on.
      assertEquals(TileType.FLOOR, levelThree.getTileType(x, 2));
      assertEquals(TileType.HAZARD, hazards.get(x, 2).type());
    }
    TileDefinition charged = hazards.get(10, 2);
    assertEquals("images/effects/lighting/glow-electric.png", charged.get("glow"));
    assertEquals(4, charged.getInt("frames", 0));
    assertTrue(levelThree.getTexturePaths().contains(charged.get("animation")));
  }

  @Test
  void levelThreeBackdropFlickersLightningBehindTheArches() {
    LevelMapData levelThree = loader.load("maps/level3.json");

    List<BackdropLayer> backdrop = levelThree.getBackdrop(null);
    assertEquals(7, backdrop.size());
    BackdropLayer.Flicker lightning = backdrop.get(1).flicker();
    assertEquals(4, lightning.frames());
    assertEquals(0, lightning.frameAt(0f));
    assertEquals(3, lightning.frameAt(0.3f));
    assertEquals(-1, lightning.frameAt(0.4f));
    assertEquals(-1, lightning.frameAt(-1f));
    assertNull(backdrop.getLast().flicker());
    // The rain is drawn thin so it never hides an attack's warning.
    assertEquals(0.8f, levelThree.getOverlay(null).getFirst().alpha(), 0.001f);
    assertEquals(0.8f, levelThree.getOverlay(null).getFirst().visibilityAt(5f), 0.001f);
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
