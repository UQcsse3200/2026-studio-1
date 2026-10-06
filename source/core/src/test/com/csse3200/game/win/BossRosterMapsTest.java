package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.entities.spawn.EnemyId;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks {@link BossRoster} against the real map files, so the roster cannot quietly drift away
 * from the maps. A kill id is a map name plus a tile, so moving a boss marker or renaming a room
 * breaks it; this test turns that into a failing build instead of a win screen that never reaches
 * Glory.
 *
 * <p>The rules:
 *
 * <ul>
 *   <li>every roster entry names a map that exists, and that map has an enemy of the listed type on
 *       the listed tile
 *   <li>every roster id is exactly what the game itself builds with {@code EnemyId.of}
 *   <li>the final boss is the Zeus on Level 3's marker tile
 *   <li>every boss-type enemy in a side room is in the roster, so adding a boss without a roster
 *       row fails
 *   <li>in each room the required boss is the highest boss, the one guarding the exit on the top
 *       storey, and no room has two required bosses
 * </ul>
 */
@ExtendWith(GameExtension.class)
class BossRosterMapsTest {
  private static final Set<String> BOSS_TYPES =
    Set.of("cerberus", "medusa", "minotaur", "cyclops", "centaur", "zeus");

  private final JsonMapLoader loader = new JsonMapLoader();
  private final Map<String, LevelMapData> byName = new HashMap<>();
  private final Map<String, String> pathByName = new HashMap<>();

  @BeforeEach
  void loadEveryMap() {
    byName.clear();
    pathByName.clear();
    for (FileHandle file : Gdx.files.internal("maps").list()) {
      if (file.name().endsWith(".json")) {
        String path = "maps/" + file.name();
        LevelMapData map = loader.load(path);
        byName.put(map.getName(), map);
        pathByName.put(map.getName(), path);
      }
    }
  }

  // ---------- the roster against the maps ----------

  @Test
  void everyRosterEntryNamesAMapThatExists() {
    List<String> missing = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (!byName.containsKey(boss.getRoomName())) {
        missing.add(boss.getRoomName());
      }
    }
    assertTrue(missing.isEmpty(), "No map is named: " + missing);
  }

  @Test
  void everyRosterEntryHasAnEnemyOfThatTypeOnThatTile() {
    List<String> problems = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      LevelMapData map = byName.get(boss.getRoomName());
      if (map == null) {
        continue; // reported by the previous test
      }
      boolean found = false;
      for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
        if (boss.getId().equals(EnemyId.of(map.getName(), spawn.getPosition()))
          && boss.getEnemyType().equals(spawn.getType())) {
          found = true;
        }
      }
      if (!found) {
        problems.add(
          boss.getId()
            + " ("
            + boss.getEnemyType()
            + ") is not in "
            + pathByName.get(boss.getRoomName()));
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyRosterIdIsExactlyWhatTheGameBuildsForThatSpawn() {
    // EnemyId.of is what LevelGameArea uses to save a kill; a hand-typed id that differs by one
    // character would never match a saved kill.
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      LevelMapData map = byName.get(boss.getRoomName());
      assertNotNull(map, boss.getRoomName());
      GridPoint2 tile = tileOf(boss.getId());
      assertEquals(EnemyId.of(map.getName(), tile), boss.getId());
    }
  }

  // ---------- the final boss ----------

  @Test
  void theFinalBossIsTheZeusOnLevelThreesMarkerTile() {
    LevelMapData levelThree =
      byName.get(
        BossRoster.FINAL_BOSS_ID.substring(0, BossRoster.FINAL_BOSS_ID.lastIndexOf(':')));
    assertNotNull(levelThree, "no map is named like the final boss's map");

    boolean found = false;
    for (SpawnPoint spawn : levelThree.getSpawns().getEnemies()) {
      if ("zeus".equals(spawn.getType())
        && BossRoster.FINAL_BOSS_ID.equals(
        EnemyId.of(levelThree.getName(), spawn.getPosition()))) {
        found = true;
      }
    }
    assertTrue(found, "Level 3 has no Zeus on the final boss tile");
  }

  @Test
  void theFinalBossIsTheOnlyZeusInLevelThree() {
    LevelMapData levelThree =
      byName.get(
        BossRoster.FINAL_BOSS_ID.substring(0, BossRoster.FINAL_BOSS_ID.lastIndexOf(':')));
    long zeus =
      levelThree.getSpawns().getEnemies().stream()
        .filter(s -> "zeus".equals(s.getType()))
        .count();

    assertEquals(1, zeus, "two Zeus in Level 3 would make the win ambiguous");
  }

  // ---------- the side rooms against the roster ----------

  @Test
  void everyBossInASideRoomIsInTheRoster() {
    Set<String> rosterIds = new HashSet<>();
    BossRoster.getMiniBosses().forEach(boss -> rosterIds.add(boss.getId()));
    List<String> unlisted = new ArrayList<>();

    for (Map.Entry<String, String> entry : pathByName.entrySet()) {
      if (!isSideRoom(entry.getValue())) {
        continue;
      }
      LevelMapData map = byName.get(entry.getKey());
      for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
        String id = EnemyId.of(map.getName(), spawn.getPosition());
        if (BOSS_TYPES.contains(spawn.getType()) && !rosterIds.contains(id)) {
          unlisted.add(id + " (" + spawn.getType() + ")");
        }
      }
    }
    assertTrue(unlisted.isEmpty(), "Add these bosses to BossRoster: " + unlisted);
  }

  @Test
  void everySideRoomHasExactlyOneRequiredBossAndItIsTheHighestBoss() {
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, String> entry : pathByName.entrySet()) {
      if (!isSideRoom(entry.getValue())) {
        continue;
      }
      LevelMapData map = byName.get(entry.getKey());
      int highest = -1;
      for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
        if (BOSS_TYPES.contains(spawn.getType())) {
          highest = Math.max(highest, spawn.getY());
        }
      }
      int required = 0;
      for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
        if (boss.getRoomName().equals(map.getName()) && boss.isRequired()) {
          required++;
          if (tileOf(boss.getId()).y != highest) {
            problems.add(
              boss.getId() + " is required but is not the highest boss (y " + highest + ")");
          }
        }
      }
      if (required != 1) {
        problems.add(map.getName() + " has " + required + " required bosses, expected 1");
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyRosterRoomIsASideRoomOnDisk() {
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      String path = pathByName.get(boss.getRoomName());
      assertNotNull(path, boss.getRoomName());
      assertTrue(
        isSideRoom(path),
        boss.getRoomName() + " should be a nether- or olympus- room, not " + path);
    }
  }

  // ---------- helpers ----------

  private static boolean isSideRoom(String path) {
    String name = path.substring(path.lastIndexOf('/') + 1);
    return name.startsWith("nether-") || name.startsWith("olympus-");
  }

  /** Reads the tile out of an id of the form {@code <map name>:<x>,<y>}. */
  private static GridPoint2 tileOf(String id) {
    String tile = id.substring(id.lastIndexOf(':') + 1);
    String[] parts = tile.split(",");
    return new GridPoint2(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }
}
