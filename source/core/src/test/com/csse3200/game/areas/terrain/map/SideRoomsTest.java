package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
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
 * <p>Every room has its own size and layout, given in the table. What they share is one doorway cut
 * into the left wall that leads back to the parent map, a player arrival tile at (4, 2), one boss
 * with a mob, and some loot past the boss. The rules:
 *
 * <ul>
 *   <li>the room loads, has the expected name and size, and the player arrival tile has ground
 *       under it
 *   <li>it has exactly one doorway: on the left wall, three tiles high, back to the right parent,
 *       and arriving on a tile that is not inside the parent's own door back to this room
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
                new GridPoint2(53, 40),
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
                new GridPoint2(53, 48),
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
                new GridPoint2(53, 54),
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
                counts("zeus", 1, "skeleton", 2, "ranged-skeleton", 2, "ranged-harpy", 2),
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
  void shouldHaveExactlyOneDoorwayOnTheLeftWallLeadingBackToItsParent(Room room) {
    LevelMapData map = loader.load(room.path());

    assertEquals(1, map.getTransitions().size(), "one way in, one way out");
    RoomTransition door = map.getTransitions().get(0);
    assertEquals(0, door.getPosition().x, "cut into the left wall");
    assertEquals(3, door.getHeight());
    assertEquals(1, door.getWidth());
    assertEquals(room.parent(), door.getDestinationMap());
    assertEquals(room.parentArrival(), door.getDestinationSpawn());
    assertNotNull(door.getTexture(), "a visible door");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rooms")
  void shouldNotStandTheArrivingPlayerInsideTheDoorBackToTheRoom(Room room) {
    LevelMapData parent = loader.load(room.parent());
    GridPoint2 arrival = loader.load(room.path()).getTransitions().get(0).getDestinationSpawn();

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
