package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the whole set of shipped maps as one connected game, not one map at a time.
 *
 * <p>{@code ShippedMapsTest} already proves each map loads and has no blocking errors. It does not
 * prove the maps fit together. This class does, so adding a room or editing a doorway cannot
 * silently strand the player, bounce them back through the door they just used, or drop them inside
 * a wall.
 *
 * <p>The rules, each its own test:
 *
 * <ul>
 *   <li>every map has a unique name (enemy kills are saved as {@code name:x,y})
 *   <li>every doorway leads to a map that exists, and arrives inside that map
 *   <li>every arrival tile is open and has something to stand on
 *   <li>no arrival tile is inside a doorway that leads straight back (an instant bounce)
 *   <li>every map can be reached on foot from the first map
 *   <li>every side room can be left again: from the room a doorway leads to, there is a way back
 *       (the main line, Level 1 to 2 to 3, only goes forward and is exempt)
 *   <li>every map has a player spawn with ground under it, because dying and loading a save both
 *       put the player on it
 *   <li>no enemy starts inside a solid tile
 *   <li>no two doorways in one map overlap
 *   <li>every doorway has open standing room beside it
 * </ul>
 *
 * <p>When the game gets a new final map, add it to {@link #LAST_MAPS} and {@link #FORWARD_ONLY}.
 */
@ExtendWith(GameExtension.class)
class MapGraphTest {
  private static final String START = "maps/level1-greek.json";

  /** Maps the player is never expected to leave: the end of the game. */
  private static final Set<String> LAST_MAPS = Set.of("maps/level3.json");

  /**
   * The main line of the game, which only goes forward: Level 1 leads to Level 2 leads to Level 3
   * and there is deliberately no door back. Every other map is a side room and must have a way
   * back.
   */
  private static final Set<String> FORWARD_ONLY = Set.of("maps/level2.json", "maps/level3.json");

  private final JsonMapLoader loader = new JsonMapLoader();
  private final Map<String, LevelMapData> maps = new LinkedHashMap<>();

  @BeforeEach
  void loadEveryShippedMap() {
    maps.clear();
    for (FileHandle file : Gdx.files.internal("maps").list()) {
      if (file.name().endsWith(".json")) {
        String path = "maps/" + file.name();
        maps.put(path, loader.load(path));
      }
    }
    assertFalse(maps.isEmpty(), "No maps found in assets/maps");
    assertTrue(maps.containsKey(START), "The game starts in " + START);
  }

  // ---------- names ----------

  @Test
  void everyMapHasAUniqueName() {
    Map<String, String> byName = new HashMap<>();
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          String earlier = byName.put(map.getName(), path);
          if (earlier != null) {
            problems.add("'" + map.getName() + "' is used by both " + earlier + " and " + path);
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  // ---------- doorways ----------

  @Test
  void everyDoorwayLeadsToAMapThatExists() {
    List<String> problems = new ArrayList<>();
    forEachDoorway(
        (path, door) -> {
          if (!maps.containsKey(door.getDestinationMap())) {
            problems.add(describe(path, door) + " leads to missing " + door.getDestinationMap());
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyDoorwayNamesAnArrivalTileInsideItsDestination() {
    List<String> problems = new ArrayList<>();
    forEachDoorway(
        (path, door) -> {
          LevelMapData destination = maps.get(door.getDestinationMap());
          GridPoint2 spawn = door.getDestinationSpawn();
          if (destination == null) {
            return;
          }
          if (spawn == null) {
            problems.add(describe(path, door) + " names no arrival tile");
          } else if (!inBounds(destination, spawn)) {
            problems.add(describe(path, door) + " arrives outside the map at " + spawn);
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyArrivalTileIsOpenWithSomethingToStandOn() {
    List<String> problems = new ArrayList<>();
    forEachDoorway(
        (path, door) -> {
          LevelMapData destination = maps.get(door.getDestinationMap());
          GridPoint2 spawn = door.getDestinationSpawn();
          if (destination == null || spawn == null || !inBounds(destination, spawn)) {
            return;
          }
          LevelView view = new MapDataLevelView(destination);
          if (view.isSolid(spawn.x, spawn.y)) {
            problems.add(describe(path, door) + " arrives inside a solid tile at " + spawn);
          }
          if (spawn.y > 0 && !view.isSupporting(spawn.x, spawn.y - 1)) {
            problems.add(describe(path, door) + " arrives with nothing under " + spawn);
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noArrivalTileIsInsideADoorwayThatLeadsStraightBack() {
    List<String> problems = new ArrayList<>();
    forEachDoorway(
        (path, door) -> {
          LevelMapData destination = maps.get(door.getDestinationMap());
          GridPoint2 spawn = door.getDestinationSpawn();
          if (destination == null || spawn == null) {
            return;
          }
          for (RoomTransition back : destination.getTransitions()) {
            if (back.getDestinationMap().equals(path) && contains(back, spawn)) {
              problems.add(
                  describe(path, door)
                      + " arrives at "
                      + spawn
                      + ", inside '"
                      + back.getId()
                      + "', which leads straight back");
            }
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noTwoDoorwaysInOneMapOverlap() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          List<RoomTransition> doors = map.getTransitions();
          for (int i = 0; i < doors.size(); i++) {
            for (int j = i + 1; j < doors.size(); j++) {
              if (overlap(doors.get(i), doors.get(j))) {
                problems.add(
                    path
                        + ": '"
                        + doors.get(i).getId()
                        + "' overlaps '"
                        + doors.get(j).getId()
                        + "'");
              }
            }
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyDoorwayHasOpenStandingRoomBesideIt() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          LevelView view = new MapDataLevelView(map);
          for (RoomTransition door : map.getTransitions()) {
            int left = door.getPosition().x - 1;
            int right = door.getPosition().x + door.getWidth();
            int y = door.getPosition().y;
            boolean leftOk = standable(view, left, y);
            boolean rightOk = standable(view, right, y);
            if (!leftOk && !rightOk) {
              problems.add(
                  describe(path, door) + " has no open ground beside it to walk up to it from");
            }
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  // ---------- the graph ----------

  @Test
  void nonReachableJSONFilesAreValid() {
    Set<String> reachable = reachableFrom(START);
    Set<String> stranded = new HashSet<>(maps.keySet());
    stranded.removeAll(reachable);

    assertTrue(
        stranded.contains("maps/template.json"),
        "No doorway path from " + START + " reaches: " + stranded.stream().toList().getFirst());
    assertTrue(
        stranded.contains("maps/demo.json"),
        "No doorway path from " + START + " reaches: " + stranded.stream().toList().getLast());
  }

  @Test
  void everyRoomCanBeLeftAgain() {
    List<String> problems = new ArrayList<>();
    forEachDoorway(
        (path, door) -> {
          String destination = door.getDestinationMap();
          if (!maps.containsKey(destination) || FORWARD_ONLY.contains(destination)) {
            return;
          }
          if (!reachableFrom(destination).contains(path)) {
            problems.add(describe(path, door) + " leads to a room with no way back to " + path);
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void theLastMapIsReachableAndEverythingElseHasAnExitOrIsASideRoom() {
    for (String last : LAST_MAPS) {
      assertTrue(maps.containsKey(last), "LAST_MAPS names a map that does not exist: " + last);
      assertTrue(reachableFrom(START).contains(last), last + " cannot be reached from " + START);
    }
    List<String> deadEnds = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          if (!LAST_MAPS.contains(path) && map.getTransitions().isEmpty()) {
            deadEnds.add(path);
          }
        });
    assertTrue(
        deadEnds.isEmpty(), "Maps with no doorway that are not the end of the game: " + deadEnds);
  }

  // ---------- spawns ----------

  @Test
  void everyMapHasAPlayerSpawnWithGroundUnderIt() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          GridPoint2 spawn = map.getSpawns().getPlayer();
          if (spawn == null) {
            problems.add(
                path + " has no player spawn (a revive or a load would have nowhere to go)");
            return;
          }
          LevelView view = new MapDataLevelView(map);
          if (!inBounds(map, spawn)) {
            problems.add(path + " player spawn " + spawn + " is outside the map");
          } else if (view.isSolid(spawn.x, spawn.y)) {
            problems.add(path + " player spawn " + spawn + " is inside a solid tile");
          } else if (spawn.y > 0 && !view.isSupporting(spawn.x, spawn.y - 1)) {
            problems.add(path + " player spawn " + spawn + " has nothing under it");
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noEnemyStartsInsideASolidTileOrAHazard() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          LevelView view = new MapDataLevelView(map);
          for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
            if (!view.inBounds(spawn.getX(), spawn.getY())) {
              problems.add(
                  path
                      + ": "
                      + spawn.getType()
                      + " at "
                      + spawn.getPosition()
                      + " is outside the map");
            } else if (view.isSolid(spawn.getX(), spawn.getY())) {
              problems.add(
                  path
                      + ": "
                      + spawn.getType()
                      + " at "
                      + spawn.getPosition()
                      + " is inside a solid tile");
            } else if (view.isHazard(spawn.getX(), spawn.getY())) {
              problems.add(
                  path
                      + ": "
                      + spawn.getType()
                      + " at "
                      + spawn.getPosition()
                      + " starts in a hazard");
            }
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noTwoEnemiesShareATileInAnyMap() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          Set<String> seen = new HashSet<>();
          for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
            String key = spawn.getX() + "," + spawn.getY();
            if (!seen.add(key)) {
              problems.add(path + ": two enemies at " + key + " would share one saved kill id");
            }
          }
        });
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyMapDoorwaySitsInsideItsOwnMap() {
    List<String> problems = new ArrayList<>();
    maps.forEach(
        (path, map) -> {
          for (RoomTransition door : map.getTransitions()) {
            GridPoint2 corner = door.getPosition();
            boolean inside =
                corner.x >= 0
                    && corner.y >= 0
                    && corner.x + door.getWidth() <= map.getWidth()
                    && corner.y + door.getHeight() <= map.getHeight();
            if (!inside) {
              problems.add(
                  describe(path, door)
                      + " is not wholly inside the "
                      + map.getWidth()
                      + " by "
                      + map.getHeight()
                      + " map");
            }
          }
        });
    assertEquals(0, problems.size(), () -> String.join("\n", problems));
    assertNotNull(maps.get(START));
  }

  // ---------- helpers ----------

  private interface DoorwayVisitor {
    void visit(String mapPath, RoomTransition door);
  }

  private void forEachDoorway(DoorwayVisitor visitor) {
    maps.forEach((path, map) -> map.getTransitions().forEach(door -> visitor.visit(path, door)));
  }

  private Set<String> reachableFrom(String start) {
    Set<String> seen = new HashSet<>();
    Deque<String> queue = new ArrayDeque<>();
    queue.add(start);
    seen.add(start);
    while (!queue.isEmpty()) {
      LevelMapData map = maps.get(queue.removeFirst());
      if (map == null) {
        continue;
      }
      for (RoomTransition door : map.getTransitions()) {
        if (maps.containsKey(door.getDestinationMap()) && seen.add(door.getDestinationMap())) {
          queue.add(door.getDestinationMap());
        }
      }
    }
    return seen;
  }

  private static boolean inBounds(LevelMapData map, GridPoint2 tile) {
    return tile.x >= 0 && tile.y >= 0 && tile.x < map.getWidth() && tile.y < map.getHeight();
  }

  private static boolean contains(RoomTransition door, GridPoint2 tile) {
    GridPoint2 corner = door.getPosition();
    return tile.x >= corner.x
        && tile.x < corner.x + door.getWidth()
        && tile.y >= corner.y
        && tile.y < corner.y + door.getHeight();
  }

  private static boolean overlap(RoomTransition a, RoomTransition b) {
    GridPoint2 pa = a.getPosition();
    GridPoint2 pb = b.getPosition();
    return pa.x < pb.x + b.getWidth()
        && pb.x < pa.x + a.getWidth()
        && pa.y < pb.y + b.getHeight()
        && pb.y < pa.y + a.getHeight();
  }

  private static boolean standable(LevelView view, int x, int y) {
    return view.inBounds(x, y) && !view.isSolid(x, y) && (y == 0 || view.isSupporting(x, y - 1));
  }

  private static String describe(String path, RoomTransition door) {
    return path + " doorway '" + door.getId() + "'";
  }
}
