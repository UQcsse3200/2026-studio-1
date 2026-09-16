package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Maps here are drawn as text, top row first, using a legend where {@code #} is a wall, {@code -}
 * is a platform, {@code ^} is a hazard, {@code H} is a ladder and a space is open air.
 */
@ExtendWith(GameExtension.class)
class LootSpawnFinderTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  /** Acceptance criterion: loot rests on the ground rather than floating or sinking into it. */
  @Test
  void shouldFindTheOpenTilesDirectlyAboveTheFloor() {
    LevelMapData map = map("   ", "###");

    List<GridPoint2> spots = positions(LootSpawnFinder.findGroundSpots(map));

    assertEquals(
        List.of(new GridPoint2(0, 1), new GridPoint2(1, 1), new GridPoint2(2, 1)),
        spots,
        "every open tile standing on the wall row should be a spot");
  }

  /** Acceptance criterion: loot is never placed inside a wall. */
  @Test
  void shouldNeverPlaceLootInsideAWall() {
    LevelMapData map = map("###", "###");

    assertTrue(
        LootSpawnFinder.findGroundSpots(map).isEmpty(),
        "a map that is solid wall has nowhere to put loot");
  }

  /** Loot must not float: only the tile directly above the ground counts, not the air above it. */
  @Test
  void shouldNotPlaceLootInMidAir() {
    LevelMapData map = map("   ", "   ", "###");

    List<GridPoint2> spots = positions(LootSpawnFinder.findGroundSpots(map));

    assertEquals(3, spots.size(), "only the row sitting on the floor should be used");
    for (GridPoint2 spot : spots) {
      assertEquals(1, spot.y, "spot " + spot + " should be on the row just above the floor");
    }
  }

  /** Platforms can be stood on, so loot may rest on them too. */
  @Test
  void shouldTreatPlatformsAsGround() {
    LevelMapData map = map("   ", "---");

    assertEquals(
        3,
        LootSpawnFinder.findGroundSpots(map).size(),
        "every tile above a platform should be a spot");
  }

  /** Loot resting on a hazard would hurt the player picking it up. */
  @Test
  void shouldNotPlaceLootOnAHazard() {
    LevelMapData map = map("   ", "^^^");

    assertTrue(
        LootSpawnFinder.findGroundSpots(map).isEmpty(),
        "tiles above hazards should not be used for loot");
  }

  /** A ladder tile is not open floor space, so nothing should be dropped onto one. */
  @Test
  void shouldNotPlaceLootOnALadderTile() {
    LevelMapData map = map("H  ", "###");

    List<GridPoint2> spots = positions(LootSpawnFinder.findGroundSpots(map));

    assertEquals(
        List.of(new GridPoint2(1, 1), new GridPoint2(2, 1)),
        spots,
        "the ladder tile should be skipped");
  }

  /** The bottom row has nothing beneath it, so nothing there can be standing on the ground. */
  @Test
  void shouldIgnoreTheBottomRow() {
    LevelMapData map = map("   ");

    assertTrue(
        LootSpawnFinder.findGroundSpots(map).isEmpty(), "a single open row has no ground under it");
  }

  @Test
  void shouldRejectAMissingMap() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LootSpawnFinder.findGroundSpots((LevelMapData) null),
        "finding spots without a map should be rejected");
  }

  /** Builds a map from text rows, top row first. */
  private LevelMapData map(String... rows) {
    StringBuilder json = new StringBuilder();
    json.append("{ \"legend\": {")
        .append("\"#\": { \"type\": \"WALL\" },")
        .append("\"-\": { \"type\": \"PLATFORM\" },")
        .append("\"^\": { \"type\": \"HAZARD\" },")
        .append("\"H\": { \"type\": \"LADDER\" }")
        .append("}, \"layers\": { \"terrain\": [");
    for (int i = 0; i < rows.length; i++) {
      json.append('"').append(rows[i]).append('"');
      if (i < rows.length - 1) {
        json.append(',');
      }
    }
    json.append("] } }");
    return loader.parse(json.toString());
  }

  private List<GridPoint2> positions(List<SpawnPoint> spots) {
    List<GridPoint2> positions = new ArrayList<>();
    for (SpawnPoint spot : spots) {
      positions.add(spot.getPosition());
    }
    return positions;
  }
}
