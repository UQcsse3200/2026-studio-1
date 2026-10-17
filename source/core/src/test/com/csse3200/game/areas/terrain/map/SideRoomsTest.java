package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests every side room, one parameterised case per room, against the same table of what each room
 * is meant to contain. The table is the specification: adding a room means adding one row here.
 *
 * <p>Every room has its own size and layout, given in the table. What they share is a player
 * arrival tile at (4, 2), an entry doorway cut into the left wall beside it, an exit doorway cut
 * into a side wall of the top storey so a cleared room is left from where it ends, one boss with a
 * mob, and some loot past the boss. Both doorways lead back to the parent map. The rules:
 *
 * <ul>
 *   <li>the room loads, has the expected name and size, and the player arrival tile has ground
 *       under it
 *   <li>it has exactly two doorways, each three tiles high and back to the right parent: the entry
 *       on the left wall at the arrival tile's height, and the exit on a side wall above it
 *   <li>neither doorway arrives on a tile that is inside the parent's own door back to this room
 *   <li>the parent map has a doorway that leads to this room and arrives at the room's own spawn
 *   <li>the enemies are exactly the ones in the table, counted by type
 *   <li>a stationary boss (the Cerberus) comes with at least eight other enemies, any other boss
 *       with at least five, so a boss that cannot chase is made hard by numbers instead
 *   <li>the player is never ambushed on arrival: no enemy within five tiles of the arrival tile
 *       (outside every melee reach), and the boss at least twenty tiles away
 *   <li>there is loot (at least the table's count), and every enemy type has a spawn factory
 * </ul>
 *
 * <p>Footing, headroom and hazards for every enemy in every map are covered by {@code
 * AllMapsEnemyPlacementTest}; the doorway graph by {@code MapGraphTest}.
 *
 * <p>The enemy types counted here are the names used in the map files ("skeleton",
 * "ranged-skeleton", "harpy" and so on).
 */
@ExtendWith(GameExtension.class)
class SideRoomsTest {
  private static final GridPoint2 ARRIVAL = new GridPoint2(4, 2);
  private static final int STOREY_HEIGHT = 6; // floors are at least six tiles apart in every room
  private static final int AMBUSH_RADIUS =
      5; // a skeleton's melee reach is 2 units, which is 4 tiles
  private static final int BOSS_MIN_DISTANCE = 20;
  private static final int MOB_FOR_STATIONARY_BOSS = 8;
  private static final int MOB_FOR_MOBILE_BOSS = 5;

  private static final Set<String> BOSS_TYPES =
      Set.of("cerberus", "medusa", "minotaur", "cyclops", "centaur", "zeus");

  private final JsonMapLoader loader = new JsonMapLoader();

  /** One row of the specification. */
  private record Room(
      String path,
      String name,
      int width,
      int height,
      String parent,
      GridPoint2 parentArrival,
      String parentDoorId,
      Map<String, Integer> enemies,
      int minLoot) {}

  private static Map<String, Integer> counts(Object... pairs) {
    Map<String, Integer> map = new TreeMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], (Integer) pairs[i + 1]);
    }
    return map;
  }

  private static Stream<Arguments> rooms() {
    return Stream.of(
        Arguments.of(
            new Room(
                "maps/nether-hounds-den.json",
                "Hound's Den",
                56,
                20,
                "maps/level1-greek.json",
                new GridPoint2(50, 34),
                "hounds-den-door",
                counts("cerberus", 1, "skeleton", 5, "ranged-skeleton", 3, "harpy", 2),
                3)),
        Arguments.of(
            new Room(
                "maps/nether-gorgon-gallery.json",
                "Gorgon's Gallery",
                44,
                22,
                "maps/level1-greek.json",
                new GridPoint2(4, 40),
                "gorgon-gallery-door",
                counts("medusa", 1, "skeleton", 4, "ranged-skeleton", 3, "harpy", 2),
                3)),
        Arguments.of(
            new Room(
                "maps/nether-minotaur-labyrinth.json",
                "Minotaur's Labyrinth",
                40,
                40,
                "maps/level1-greek.json",
                new GridPoint2(53, 46),
                "minotaur-labyrinth-door",
                counts("minotaur", 1, "centaur", 1, "skeleton", 3, "ranged-skeleton", 2),
                2)),
        Arguments.of(
            new Room(
                "maps/nether-shades-barracks.json",
                "Shades' Barracks",
                60,
                22,
                "maps/level1-greek.json",
                new GridPoint2(4, 52),
                "shades-barracks-door",
                counts("minotaur", 1, "skeleton", 8, "ranged-skeleton", 4),
                3)),
        Arguments.of(
            new Room(
                "maps/olympus-cyclops-forge.json",
                "Cyclops Forge",
                40,
                20,
                "maps/level2.json",
                new GridPoint2(35, 39),
                "cyclops-forge-door",
                counts("cyclops", 1, "skeleton", 3, "ranged-skeleton", 2),
                2)),
        Arguments.of(
            new Room(
                "maps/olympus-centaur-pavilion.json",
                "Centaur Pavilion",
                64,
                22,
                "maps/level2.json",
                new GridPoint2(35, 27),
                "centaur-pavilion-door",
                counts("centaur", 2, "skeleton", 2, "ranged-skeleton", 2, "harpy", 2),
                2)),
        Arguments.of(
            new Room(
                "maps/olympus-outer-guard.json",
                "Zeus's Outer Guard",
                48,
                26,
                "maps/level2.json",
                new GridPoint2(35, 51),
                "outer-guard-door",
                counts("cyclops", 1, "medusa", 1, "skeleton", 3, "ranged-skeleton", 3, "harpy", 2),
                3)),
        Arguments.of(
            new Room(
                "maps/olympus-thunder-hall.json",
                "Zeus's Thunder Hall",
                56,
                26,
                "maps/level2.json",
                new GridPoint2(35, 3),
                "thunder-hall-door",
                counts("medusa", 1, "skeleton", 2, "ranged-skeleton", 2, "ranged-harpy", 2),
                3)));
  }

  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
  }

  // ---------- the room itself ----------

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldLoadWithTheExpectedNameAndSize(Room room) {
    LevelMapData map = loader.load(room.path());

    assertEquals(room.name(), map.getName());
    assertEquals(room.width(), map.getWidth());
    assertEquals(room.height(), map.getHeight());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldPutThePlayerOnGroundAtTheArrivalTile(Room room) {
    LevelMapData map = loader.load(room.path());
    LevelView view = new MapDataLevelView(map);

    assertEquals(ARRIVAL, map.getSpawns().getPlayer());
    assertFalse(view.isSolid(ARRIVAL.x, ARRIVAL.y), "the arrival tile is open");
    assertTrue(view.isSupporting(ARRIVAL.x, ARRIVAL.y - 1), "and has ground under it");
  }

  // ---------- the doorway back ----------

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldHaveAnEntryOnTheLeftWallAndAnExitOnTheTopStoreyBothLeadingBackToItsParent(Room room) {
    LevelMapData map = loader.load(room.path());

    assertEquals(2, map.getTransitions().size(), "the way in, and the way out at the top");
    RoomTransition entry = map.getTransitions().get(0);
    RoomTransition exit = map.getTransitions().get(1);
    assertEquals(0, entry.getPosition().x, "the entry is cut into the left wall");
    assertEquals(ARRIVAL.y, entry.getPosition().y, "beside the arrival tile");
    int exitX = exit.getPosition().x;
    assertTrue(exitX == 0 || exitX == room.width() - 1, "the exit is cut into a side wall");
    assertTrue(
        exit.getPosition().y >= entry.getPosition().y + STOREY_HEIGHT,
        "the exit is at least a storey above the entry");
    for (RoomTransition door : map.getTransitions()) {
      assertEquals(3, door.getHeight());
      assertEquals(1, door.getWidth());
      assertEquals(room.parent(), door.getDestinationMap());
      assertNotNull(door.getTexture(), "a visible door");
    }
    assertEquals(room.parentArrival(), entry.getDestinationSpawn());
    GridPoint2 expectedExitArrival =
        switch (room.path()) {
          case "maps/nether-hounds-den.json" -> new GridPoint2(50, 40);
          case "maps/nether-gorgon-gallery.json" -> new GridPoint2(4, 46);
          case "maps/nether-minotaur-labyrinth.json" -> new GridPoint2(53, 52);
          case "maps/nether-shades-barracks.json" -> new GridPoint2(4, 61);
          default -> room.parentArrival();
        };
    assertEquals(expectedExitArrival, exit.getDestinationSpawn());
  }

  @Test
  void firstNetherRoomMustBeCompletedToReachTheSecondLayer() {
    LevelMapData nether = loader.load("maps/level1-greek.json");
    LevelView view = new MapDataLevelView(nether);
    LevelMapData houndsDen = loader.load("maps/nether-hounds-den.json");

    for (int x = 0; x < nether.getWidth(); x++) {
      assertTrue(view.isSolid(x, 39), "the first layer has a solid ceiling at x=" + x);
    }
    assertEquals(new GridPoint2(50, 34), houndsDen.getTransitions().get(0).getDestinationSpawn());
    assertEquals(new GridPoint2(50, 40), houndsDen.getTransitions().get(1).getDestinationSpawn());
    assertFalse(view.isSolid(50, 40), "the room exit lands above the ceiling");
    assertTrue(view.isSupporting(50, 39), "the room exit has a floor beneath it");
  }

  @Test
  void rightSideNetherLadderIsRemovedWithoutOpeningTheDividerFloors() {
    LevelMapData nether = loader.load("maps/level1-greek.json");
    LevelView view = new MapDataLevelView(nether);

    for (int y = 34; y <= 50; y++) {
      assertFalse(view.tileTypeAt(52, y) == TileType.LADDER, "old ladder tile at y=" + y);
    }
    for (int y : new int[] {39, 45, 51}) {
      assertTrue(view.isSolid(52, y), "the divider floor remains sealed at y=" + y);
    }
  }

  @Test
  void secondNetherRoomMustBeCompletedToReachTheThirdLayer() {
    LevelMapData nether = loader.load("maps/level1-greek.json");
    LevelView view = new MapDataLevelView(nether);
    LevelMapData gallery = loader.load("maps/nether-gorgon-gallery.json");

    RoomTransition entrance =
        nether.getTransitions().stream()
            .filter(door -> door.getId().equals("gorgon-gallery-door"))
            .findFirst()
            .orElseThrow();
    assertEquals(new GridPoint2(2, 40), entrance.getPosition());
    for (int x = 0; x < nether.getWidth(); x++) {
      assertTrue(view.isSolid(x, 45), "the second layer has a solid ceiling at x=" + x);
    }
    for (int x = 2; x <= 7; x++) {
      assertFalse(view.isSolid(x, 40), "the left-hand entrance is accessible at x=" + x);
      assertFalse(view.isHazard(x, 40), "the entrance path avoids lava at x=" + x);
      assertFalse(view.isSolid(x, 46), "the third-layer exit is accessible at x=" + x);
      assertFalse(view.isHazard(x, 46), "the third-layer exit avoids lava at x=" + x);
    }
    assertEquals(new GridPoint2(4, 40), gallery.getTransitions().get(0).getDestinationSpawn());
    assertEquals(new GridPoint2(4, 46), gallery.getTransitions().get(1).getDestinationSpawn());
    assertTrue(view.isSupporting(4, 39), "the back door lands on the second-layer floor");
    assertTrue(view.isSupporting(4, 45), "the completion exit lands on the third-layer floor");
    RoomTransition nextEntrance =
        nether.getTransitions().stream()
            .filter(door -> door.getId().equals("minotaur-labyrinth-door"))
            .findFirst()
            .orElseThrow();
    RoomTransition firstEntrance =
        nether.getTransitions().stream()
            .filter(door -> door.getId().equals("hounds-den-door"))
            .findFirst()
            .orElseThrow();
    assertEquals(new GridPoint2(55, 46), nextEntrance.getPosition());
    assertEquals(
        entrance.getPosition().y - firstEntrance.getPosition().y,
        nextEntrance.getPosition().y - entrance.getPosition().y,
        "the first and second layers have matching heights");
  }

  @Test
  void netherRightWallHasNoGapsOutsideRoomDoors() {
    LevelMapData nether = loader.load("maps/level1-greek.json");
    LevelView view = new MapDataLevelView(nether);

    for (int y = 37; y <= 44; y++) {
      assertTrue(view.isSolid(54, y), "the inner right wall is closed at y=" + y);
      assertTrue(view.isSolid(55, y), "the outer right wall is closed at y=" + y);
    }
    for (int y = 49; y <= 50; y++) {
      assertTrue(view.isSolid(54, y), "the inner right wall is closed at y=" + y);
      assertTrue(view.isSolid(55, y), "the outer right wall is closed at y=" + y);
    }
    assertTrue(view.isWalkable(55, 34), "the first-room door remains accessible");
    for (int y = 46; y <= 48; y++) {
      assertTrue(view.isWalkable(55, y), "the third-room door remains accessible at y=" + y);
    }
  }

  @Test
  void thirdNetherRoomLeadsToTheFourthAndFourthRoomUnlocksTheTopLayer() {
    LevelMapData nether = loader.load("maps/level1-greek.json");
    LevelView view = new MapDataLevelView(nether);
    LevelMapData labyrinth = loader.load("maps/nether-minotaur-labyrinth.json");
    LevelMapData barracks = loader.load("maps/nether-shades-barracks.json");

    for (int x = 0; x < nether.getWidth(); x++) {
      assertTrue(view.isSolid(x, 51), "the third layer has a solid ceiling at x=" + x);
    }
    RoomTransition entrance =
        nether.getTransitions().stream()
            .filter(door -> door.getId().equals("shades-barracks-door"))
            .findFirst()
            .orElseThrow();
    assertEquals(new GridPoint2(2, 52), entrance.getPosition());
    assertEquals(new GridPoint2(53, 46), labyrinth.getTransitions().get(0).getDestinationSpawn());
    assertEquals(new GridPoint2(53, 52), labyrinth.getTransitions().get(1).getDestinationSpawn());
    assertEquals(new GridPoint2(4, 52), barracks.getTransitions().get(0).getDestinationSpawn());
    assertEquals(new GridPoint2(4, 61), barracks.getTransitions().get(1).getDestinationSpawn());
    assertTrue(view.isSupporting(53, 51), "the third-room exit lands above its entrance");
    assertTrue(view.isSupporting(4, 51), "the fourth-room entrance has a floor");
    assertTrue(view.isSupporting(4, 60), "the fourth-room exit lands above its entrance");
    for (int x = 2; x <= 54; x++) {
      assertTrue(view.isWalkable(x, 52), "the route across the fourth layer is open at x=" + x);
    }
    for (int y = 52; y <= 56; y++) {
      assertFalse(view.isSolid(4, y), "the fourth-layer route is open on the left");
      assertTrue(view.isWalkable(52, y), "the old ladder is no longer a shortcut");
    }
    for (int x = 0; x < nether.getWidth(); x++) {
      assertTrue(view.isSolid(x, 57), "the fourth layer is sealed from the top at x=" + x);
    }
    assertTrue(view.isSolid(52, 58), "the old ladder shaft is filled with blocks");
    assertTrue(view.isSolid(52, 59), "the old ladder shaft is filled with blocks");
    for (int x = 4; x <= 52; x++) {
      assertTrue(view.isSupporting(x, 60), "the top layer has a continuous floor at x=" + x);
      assertTrue(view.isWalkable(x, 61), "the top layer has a clear path at x=" + x);
      assertTrue(view.isWalkable(x, 62), "the player has headroom at x=" + x);
    }
    RoomTransition levelTwoGate =
        nether.getTransitions().stream()
            .filter(door -> door.getId().equals("nether-gate-to-room2"))
            .findFirst()
            .orElseThrow();
    assertEquals(new GridPoint2(52, 61), levelTwoGate.getPosition());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldHaveNoFloorAboveTheExit(Room room) {
    LevelMapData map = loader.load(room.path());
    LevelView view = new MapDataLevelView(map);
    RoomTransition exit = map.getTransitions().get(1);
    int inside = exit.getPosition().x == 0 ? 1 : room.width() - 2;

    assertTrue(view.isSupporting(inside, exit.getPosition().y - 1), "the exit stands on a floor");
    // The top two rows are the ceiling; anything standable between it and the exit is a storey.
    for (int y = exit.getPosition().y; y < room.height() - 2; y++) {
      assertFalse(view.isSupporting(inside, y), "the exit is on the top storey, nothing above it");
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldNotStandTheArrivingPlayerInsideTheDoorBackToTheRoom(Room room) {
    LevelMapData parent = loader.load(room.parent());

    for (RoomTransition back : loader.load(room.path()).getTransitions()) {
      GridPoint2 arrival = back.getDestinationSpawn();
      for (RoomTransition door : parent.getTransitions()) {
        if (door.getDestinationMap().equals(room.path())) {
          boolean inside =
              arrival.x >= door.getPosition().x
                  && arrival.x < door.getPosition().x + door.getWidth()
                  && arrival.y >= door.getPosition().y
                  && arrival.y < door.getPosition().y + door.getHeight();
          assertFalse(inside, "arriving back would bounce the player straight into the room again");
        }
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldBeEnteredByExactlyOneDoorwayInItsParentThatArrivesAtTheRoomsSpawn(Room room) {
    LevelMapData parent = loader.load(room.parent());

    List<RoomTransition> inward = new ArrayList<>();
    for (RoomTransition door : parent.getTransitions()) {
      if (door.getDestinationMap().equals(room.path())) {
        inward.add(door);
      }
    }

    assertEquals(1, inward.size(), "one door in the parent leads here");
    assertEquals(room.parentDoorId(), inward.get(0).getId());
    assertEquals(ARRIVAL, inward.get(0).getDestinationSpawn());
  }

  // ---------- who is in the room ----------

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldContainExactlyTheEnemiesInTheTable(Room room) {
    Map<String, Integer> actual = countEnemies(loader.load(room.path()));

    assertEquals(room.enemies(), actual);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldHaveAtLeastOneBossAndEnoughOthersToMobThePlayer(Room room) {
    Map<String, Integer> actual = countEnemies(loader.load(room.path()));
    int bosses = 0;
    int total = 0;
    for (Map.Entry<String, Integer> entry : actual.entrySet()) {
      total += entry.getValue();
      if (BOSS_TYPES.contains(entry.getKey())) {
        bosses += entry.getValue();
      }
    }
    int others = total - bosses;
    int needed = actual.containsKey("cerberus") ? MOB_FOR_STATIONARY_BOSS : MOB_FOR_MOBILE_BOSS;

    assertTrue(bosses >= 1, "a boss room needs a boss");
    assertTrue(
        others >= needed,
        room.name() + " has " + others + " others but needs " + needed + " around its boss");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldNotAmbushThePlayerOnArrivalAndShouldKeepTheBossFar(Room room) {
    LevelMapData map = loader.load(room.path());
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
      int distance = Math.abs(spawn.getX() - ARRIVAL.x);
      if (distance < AMBUSH_RADIUS) {
        problems.add(
            spawn.getType()
                + " at "
                + spawn.getPosition()
                + " is "
                + distance
                + " tiles from arrival");
      }
      if (BOSS_TYPES.contains(spawn.getType()) && distance < BOSS_MIN_DISTANCE) {
        problems.add(spawn.getType() + " is only " + distance + " tiles from arrival");
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldHaveAnEnemyTypeFactoryForEveryEnemy(Room room) {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
    DefaultEntitySpawns.registerAll();

    List<String> unknown = new ArrayList<>();
    for (SpawnPoint spawn : loader.load(room.path()).getSpawns().getEnemies()) {
      if (!EntitySpawnRegistry.isRegistered(spawn.getType())) {
        unknown.add(spawn.getType());
      }
    }
    assertTrue(unknown.isEmpty(), "no spawn factory for " + unknown);
  }

  // ---------- the reward ----------

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldHaveLootWorthTheFight(Room room) {
    LevelMapData map = loader.load(room.path());

    assertTrue(
        map.getSpawns().getLoot().size() >= room.minLoot(),
        room.name() + " should have at least " + room.minLoot() + " loot spawns");
  }

  // ---------- the table covers every room ----------

  @Test
  void shouldHaveARowForEveryRoomFileOnDisk() {
    Set<String> inTable = new HashSet<>();
    rooms().forEach(arguments -> inTable.add(((Room) arguments.get()[0]).path()));

    List<String> missing = new ArrayList<>();
    for (FileHandle file : Gdx.files.internal("maps").list()) {
      String name = file.name();
      boolean isRoom = name.startsWith("nether-") || name.startsWith("olympus-");
      if (isRoom && name.endsWith(".json") && !inTable.contains("maps/" + name)) {
        missing.add("maps/" + name);
      }
    }
    assertTrue(missing.isEmpty(), "Add a row to the table for: " + missing);
  }

  @Test
  void shouldNotListARoomThatDoesNotExist() {
    List<String> gone = new ArrayList<>();
    rooms()
        .forEach(
            arguments -> {
              String path = ((Room) arguments.get()[0]).path();
              if (!Gdx.files.internal(path).exists()) {
                gone.add(path);
              }
            });
    assertTrue(gone.isEmpty(), "In the table but not on disk: " + gone);
  }

  private static Map<String, Integer> countEnemies(LevelMapData map) {
    Map<String, Integer> counts = new TreeMap<>();
    for (SpawnPoint spawn : map.getSpawns().getEnemies()) {
      counts.merge(spawn.getType(), 1, Integer::sum);
    }
    return counts;
  }
}
