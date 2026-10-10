package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.screens.MainGameScreen;
import com.csse3200.game.ui.terminal.commands.TeleportCommand;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class TeleportCommandTest {
  /** Every side-room map constant in MainGameScreen ends with this. */
  private static final String SIDE_ROOM_SUFFIX = "_SIDE_ROOM";

  /** Every main-level map constant in MainGameScreen ends with this. */
  private static final String LEVEL_SUFFIX = "_ROOM_MAP";

  @Test
  void teleportsToARegisteredDestination() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command = new TeleportCommand(Map.of("lvl2", () -> teleported.set(true)));

    assertTrue(command.action(args("lvl2")));
    assertTrue(teleported.get());
  }

  @Test
  void rejectsUnknownOrIncorrectArguments() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command = new TeleportCommand(Map.of("lvl2", () -> teleported.set(true)));

    assertFalse(command.action(args()));
    assertFalse(command.action(args("secret-loot")));
    assertFalse(command.action(args("lvl2", "extra")));
    assertFalse(teleported.get());
  }

  @ParameterizedTest(name = "tp {0}")
  @ValueSource(
      strings = {
        "MinotaurLabyrinth",
        "GorgonGallery",
        "ShadesBarracks",
        "HoundsDen",
        "OuterGuard",
        "ThunderHall",
        "CyclopsForge",
        "CentaurPavilion"
      })
  void teleportsToEachSideRoom(String roomName) {
    List<String> visited = new ArrayList<>();
    TeleportCommand command = new TeleportCommand(recordingDestinations(visited));

    assertTrue(command.action(args(roomName)));
    assertEquals(List.of(roomName), visited, "only the room that was asked for should run");
  }

  @Test
  void acceptsMoreThanTenDestinations() {
    List<String> visited = new ArrayList<>();
    Map<String, Runnable> destinations = recordingDestinations(visited);
    TeleportCommand command = new TeleportCommand(destinations);

    for (String name : destinations.keySet()) {
      assertTrue(command.action(args(name)), name);
    }

    assertEquals(12, visited.size());
  }

  @Test
  void treatsNamesAsCaseSensitive() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command =
        new TeleportCommand(Map.of("CyclopsForge", () -> teleported.set(true)));

    assertFalse(command.action(args("cyclopsforge")));
    assertFalse(command.action(args("CYCLOPSFORGE")));
    assertFalse(teleported.get());
  }

  @Test
  void rejectsASideRoomNameSplitIntoTwoWords() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command = new TeleportCommand(Map.of("HoundsDen", () -> teleported.set(true)));

    assertFalse(command.action(args("Hounds", "Den")));
    assertFalse(teleported.get());
  }

  @Test
  void runsTheTeleportEachTimeItIsAsked() {
    AtomicInteger count = new AtomicInteger();
    TeleportCommand command = new TeleportCommand(Map.of("ThunderHall", count::incrementAndGet));

    command.action(args("ThunderHall"));
    command.action(args("ThunderHall"));

    assertEquals(2, count.get());
  }

  @Test
  void ignoresChangesToTheTableAfterItIsBuilt() {
    AtomicBoolean teleported = new AtomicBoolean();
    Map<String, Runnable> destinations = new HashMap<>();
    destinations.put("lvl2", () -> {});
    TeleportCommand command = new TeleportCommand(destinations);

    destinations.put("GorgonGallery", () -> teleported.set(true));

    assertFalse(command.action(args("GorgonGallery")));
    assertFalse(teleported.get());
  }

  @Test
  void everyDestinationPointsAtAMapFileThatExists() {
    Map<String, String> paths = mapConstants(SIDE_ROOM_SUFFIX);
    paths.putAll(mapConstants(LEVEL_SUFFIX));
    List<String> missing = new ArrayList<>();

    for (Map.Entry<String, String> entry : paths.entrySet()) {
      if (!Gdx.files.internal(entry.getValue()).exists()) {
        missing.add(entry.getKey() + " -> " + entry.getValue());
      }
    }

    assertEquals(11, paths.size(), () -> "Map constants found: " + paths.keySet());
    assertTrue(missing.isEmpty(), () -> "No map file for: " + missing);
  }

  @Test
  void hasEightSideRoomsEachWithItsOwnMap() {
    Map<String, String> sideRooms = mapConstants(SIDE_ROOM_SUFFIX);

    assertEquals(8, sideRooms.size(), () -> "Side rooms found: " + sideRooms.keySet());
    assertEquals(8, new HashSet<>(sideRooms.values()).size(), "two side rooms share a map file");
  }

  @Test
  void everySideRoomStartsThePlayerOnTheSharedSpawn() {
    GridPoint2 sharedSpawn = (GridPoint2) constant("SIDE_ROOM_SPAWN");
    JsonMapLoader loader = new JsonMapLoader();
    List<String> wrong = new ArrayList<>();

    for (Map.Entry<String, String> entry : mapConstants(SIDE_ROOM_SUFFIX).entrySet()) {
      LevelMapData map = loader.load(entry.getValue());
      if (!sharedSpawn.equals(map.getSpawns().getPlayer())) {
        wrong.add(entry.getKey() + " spawns the player at " + map.getSpawns().getPlayer());
      }
    }

    assertTrue(wrong.isEmpty(), () -> "Expected " + sharedSpawn + " but: " + wrong);
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }

  /** Builds the twelve destinations the game registers; each one adds its own name to the list. */
  private static Map<String, Runnable> recordingDestinations(List<String> visited) {
    Map<String, Runnable> destinations = new HashMap<>();
    for (String name :
        List.of(
            "lvl1dungeon",
            "lvl1nether",
            "lvl2",
            "lvl3",
            "MinotaurLabyrinth",
            "GorgonGallery",
            "ShadesBarracks",
            "HoundsDen",
            "OuterGuard",
            "ThunderHall",
            "CyclopsForge",
            "CentaurPavilion")) {
      destinations.put(name, () -> visited.add(name));
    }
    return destinations;
  }

  /**
   * Reads the map path constants from MainGameScreen, so the tests check the paths the game really
   * uses. The screen itself is never built.
   *
   * @param suffix the ending of the constant names to collect
   * @return constant name to map path, in name order
   */
  private static Map<String, String> mapConstants(String suffix) {
    Map<String, String> paths = new TreeMap<>();
    for (Field field : MainGameScreen.class.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())
          && field.getType() == String.class
          && field.getName().endsWith(suffix)) {
        paths.put(field.getName(), (String) constant(field.getName()));
      }
    }
    return paths;
  }

  /** Reads one private constant of MainGameScreen by name. */
  private static Object constant(String name) {
    try {
      Field field = MainGameScreen.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(null);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("MainGameScreen has no constant called " + name, e);
    }
  }
}
