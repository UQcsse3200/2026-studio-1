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

  /**
   * Bug #194: a gap inside a solid band of ground has floor below it but is sealed in above, so
   * loot placed there is drawn inside the ground where nobody can reach it.
   */
  @Test
  void shouldNotPlaceLootInAGapSealedInsideTheGround() {
    LevelMapData map = map("###", "   ", "###");

    assertTrue(
        LootSpawnFinder.findGroundSpots(map).isEmpty(),
        "a gap with solid ground above and below should not hold loot");
  }

  /** Bug #194: loot needs open space above it, so a tile under a ceiling is skipped. */
  @Test
  void shouldNotPlaceLootUnderALowCeiling() {
    LevelMapData map = map("#  ", "   ", "###");

    List<GridPoint2> spots = positions(LootSpawnFinder.findGroundSpots(map));

    assertEquals(
        List.of(new GridPoint2(1, 1), new GridPoint2(2, 1)),
        spots,
        "only the tiles with open space above them should be spots");
  }

  /**
   * Bug #194: an open pocket walled off from the rest of the map can pass the ground and headroom
   * checks, so a spot must also be reachable from where the player starts.
   */
  @Test
  void shouldNotPlaceLootInAPocketThePlayerCannotReach() {
    // Two rooms split by a wall. The player starts in the left room.
    LevelMapData map = mapWithPlayer(1, 1, "#######", "#  #  #", "#  #  #", "#######");

    List<GridPoint2> spots = positions(LootSpawnFinder.findGroundSpots(map));

    assertEquals(
        List.of(new GridPoint2(1, 1), new GridPoint2(2, 1)),
        spots,
        "only the floor of the room the player starts in should be used");
  }

  /** A map with no player spawn has no starting point, so every room is allowed. */
  @Test
  void shouldAllowEveryRoomWhenTheMapHasNoPlayerSpawn() {
    LevelMapData map = map("#######", "#  #  #", "#  #  #", "#######");

    assertEquals(
        4,
        LootSpawnFinder.findGroundSpots(map).size(),
        "without a player spawn, the floor of both rooms should be used");
  }

  @Test
  void shouldRejectAMissingMap() {
    assertThrows(
        IllegalArgumentException.class,
        () -> LootSpawnFinder.findGroundSpots((LevelMapData) null),
        "finding spots without a map should be rejected");
  }

  /** Builds a map from text rows, top row first, with no player spawn. */
  private LevelMapData map(String... rows) {
    return buildMap(null, rows);
  }

  /** Builds a map from text rows, top row first, with the player starting on tile (x, y). */
  private LevelMapData mapWithPlayer(int playerX, int playerY, String... rows) {
    return buildMap(
        "\"spawns\": { \"player\": { \"x\": " + playerX + ", \"y\": " + playerY + " } }", rows);
  }

  private LevelMapData buildMap(String spawnsJson, String... rows) {
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
    json.append("] }");
    if (spawnsJson != null) {
      json.append(", ").append(spawnsJson);
    }
    json.append(" }");
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
