package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MapDataLevelViewTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  /** A four-wide room: solid floor, a platform ledge, a hazard, a ladder and a decorative tile. */
  private LevelView level() {
    String json =
        """
        {
          "name": "Sample",
          "tileSize": 0.5,
          "legend": {
            "#": { "type": "WALL" },
            "=": { "type": "PLATFORM" },
            "^": { "type": "HAZARD", "damage": "15" },
            "l": { "type": "LADDER" },
            ".": { "type": "DECORATIVE" }
          },
          "entityLegend": {
            "P": { "type": "PLAYER" },
            "N": { "type": "MARKER", "kind": "npc", "id": "traveler" }
          },
          "layers": {
            "terrain":  ["  l.", " ==l", "#^##"],
            "entities": ["    ", "N   ", "P   "]
          }
        }
        """;
    return new MapDataLevelView(loader.parse(json));
  }

  @Test
  void reportsTheLevelsOwnMeasurements() {
    LevelView level = level();

    assertEquals("Sample", level.name());
    assertEquals(4, level.width());
    assertEquals(3, level.height());
    assertEquals(0.5f, level.tileSize());
  }

  @Test
  void answersOutOfBoundsWithoutThrowing() {
    LevelView level = level();

    assertFalse(level.inBounds(-1, 0));
    assertFalse(level.inBounds(4, 0));
    assertNull(level.tileAt(99, 99));
    assertNull(level.tileTypeAt(99, 99));
    assertFalse(level.isSolid(99, 99));
    assertFalse(level.isWalkable(99, 99));
  }

  @Test
  void classifiesTilesByWhatTheyDoRatherThanWhatTheyAre() {
    LevelView level = level();

    // Bottom row, y = 0: wall, hazard, wall, wall.
    assertTrue(level.isSolid(0, 0));
    assertTrue(level.isSupporting(0, 0));
    assertFalse(level.isWalkable(0, 0));
    assertTrue(level.isHazard(1, 0));

    // Middle row, y = 1: platforms hold you up but are not solid.
    assertTrue(level.isSupporting(1, 1));
    assertFalse(level.isSolid(1, 1));

    // A ladder is walkable and holds nothing up.
    assertTrue(level.isWalkable(3, 1));
    assertFalse(level.isSupporting(3, 1));

    // An empty cell is walkable.
    assertTrue(level.isWalkable(0, 2));
  }

  @Test
  void exposesTilePropertiesThroughTheView() {
    assertEquals(15, level().tileAt(1, 0).getInt("damage", 0));
  }

  @Test
  void findsOpenGroundAboveSolidFooting() {
    List<GridPoint2> ground = level().groundTiles();

    // Above the bottom-row wall at (0,0) is open, so (0,1) qualifies.
    assertTrue(ground.contains(new GridPoint2(0, 1)));
    // Above the platforms at y = 1 is open, so (1,2) and (2,2) qualify.
    assertTrue(ground.contains(new GridPoint2(1, 2)));
    // A ladder holds nothing up, so the tile above it does not qualify.
    assertFalse(ground.contains(new GridPoint2(3, 2)));
  }

  @Test
  void servesSpawnsAndMarkers() {
    LevelView level = level();

    assertEquals(new GridPoint2(0, 0), level.playerSpawn());
    assertEquals(1, level.markers("npc").size());
    assertEquals("traveler", level.markers("NPC").getFirst().id());
    assertTrue(level.markers("pet").isEmpty());
  }

  @Test
  void tileTypeMatchesTheLegend() {
    assertEquals(TileType.PLATFORM, level().tileTypeAt(1, 1));
    assertEquals(TileType.DECORATIVE, level().tileTypeAt(3, 2));
  }

  @Test
  void rejectsANullMap() {
    assertThrows(IllegalArgumentException.class, () -> new MapDataLevelView(null));
  }
}
