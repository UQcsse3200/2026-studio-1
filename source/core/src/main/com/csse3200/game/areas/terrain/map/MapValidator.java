package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.CollisionType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Checks a loaded map for the mistakes that make a level unplayable or confusing.
 *
 * <p>A map that loads but is wrong costs more time than a map that refuses to load, because the
 * mistake is found during a playtest by whoever happens to walk there. This class turns those
 * mistakes into a list, so a test can fail the build on them instead.
 *
 * <p>Problems are split by how certain they are:
 *
 * <ul>
 *   <li>{@link Severity#ERROR} is structural and never intentional: no collision layer, a spawn
 *       outside the map, a doorway to a file that does not exist. Builds should fail on these.
 *   <li>{@link Severity#WARNING} is a judgement call that is usually a mistake: a player spawn with
 *       no ground under it, a map with no player spawn at all. Worth reading, not worth blocking.
 * </ul>
 *
 * <p>Checks that need the filesystem, such as whether a doorway's destination map exists, only run
 * when {@link #validate(LevelMapData, Predicate)} is given a way to test for a file.
 */
public final class MapValidator {
  private static final String OUTSIDE_THE_MAP = " is outside the map";

  private MapValidator() {}

  /** The start of every message about a map: {@code Map 'name' }. */
  private static String about(String mapName) {
    return "Map '" + mapName + "' ";
  }

  /** The start of every message about one doorway: {@code Map 'name' transition 'id'}. */
  private static String aboutTransition(String mapName, String id) {
    return about(mapName) + "transition '" + id + "'";
  }

  /** How sure the validator is that a problem is a mistake. */
  public enum Severity {
    /** Structural, never intentional. A build should fail. */
    ERROR,
    /** Usually a mistake, occasionally deliberate. Worth reading. */
    WARNING
  }

  /**
   * One thing wrong with a map.
   *
   * @param severity how sure the validator is
   * @param message what is wrong, naming the map and the position
   */
  public record Problem(Severity severity, String message) {
    @Override
    public String toString() {
      return severity + ": " + message;
    }
  }

  /**
   * Runs every check that does not need the filesystem.
   *
   * @param map the loaded map
   * @return the problems found, empty if the map is sound
   */
  public static List<Problem> validate(LevelMapData map) {
    return validate(map, null);
  }

  /**
   * Runs every check, including those needing the filesystem.
   *
   * @param map the loaded map
   * @param assetExists tests whether an asset path resolves to a file, or null to skip those checks
   * @return the problems found, empty if the map is sound
   */
  public static List<Problem> validate(LevelMapData map, Predicate<String> assetExists) {
    List<Problem> problems = new ArrayList<>();
    if (map == null) {
      problems.add(new Problem(Severity.ERROR, "Map is null"));
      return problems;
    }

    String name = map.getName();
    if (map.isEmpty()) {
      problems.add(new Problem(Severity.ERROR, about(name) + "has no layers"));
      return problems;
    }

    checkCollisionLayer(map, name, problems);
    checkPlayerSpawn(map, name, problems);
    checkSpawnBounds(map, name, problems);
    checkTransitions(map, name, assetExists, problems);
    checkTextures(map, name, assetExists, problems);
    return problems;
  }

  /**
   * @param problems problems from a validate call
   * @return only the errors, which are the ones a build should fail on
   */
  public static List<Problem> errorsIn(List<Problem> problems) {
    return problems.stream().filter(problem -> problem.severity() == Severity.ERROR).toList();
  }

  /** Without a collision layer nothing is solid and the player falls out of the world. */
  private static void checkCollisionLayer(LevelMapData map, String name, List<Problem> problems) {
    if (map.getCollisionLayer() == null) {
      problems.add(
          new Problem(
              Severity.ERROR,
              about(name)
                  + "has no '"
                  + LevelMapData.COLLISION_LAYER
                  + "' or '"
                  + LevelMapData.TERRAIN_LAYER
                  + "' layer, so nothing in it is solid. Check the layer names for a typo."));
    }
  }

  private static void checkPlayerSpawn(LevelMapData map, String name, List<Problem> problems) {
    GridPoint2 spawn = map.getSpawns().getPlayer();
    if (spawn == null) {
      problems.add(new Problem(Severity.WARNING, about(name) + "defines no player spawn"));
      return;
    }
    if (outOfBounds(map, spawn.x, spawn.y)) {
      problems.add(
          new Problem(Severity.ERROR, about(name) + "player spawn " + spawn + OUTSIDE_THE_MAP));
      return;
    }
    describeFooting(map, spawn, "player spawn", name, problems);
  }

  /** A spawn inside a wall, or hanging over nothing, is nearly always an authoring slip. */
  private static void describeFooting(
      LevelMapData map, GridPoint2 tile, String what, String name, List<Problem> problems) {
    if (isSolid(map, tile.x, tile.y)) {
      problems.add(
          new Problem(
              Severity.WARNING, about(name) + what + " " + tile + " is inside a solid tile"));
    }
    if (tile.y > 0 && !isSupporting(map, tile.x, tile.y - 1)) {
      problems.add(
          new Problem(
              Severity.WARNING, about(name) + what + " " + tile + " has no ground beneath it"));
    }
  }

  private static void checkSpawnBounds(LevelMapData map, String name, List<Problem> problems) {
    MapSpawns spawns = map.getSpawns();
    for (SpawnPoint spawn : spawns.getEnemies()) {
      if (outOfBounds(map, spawn.getX(), spawn.getY())) {
        problems.add(
            new Problem(
                Severity.ERROR,
                about(name) + "enemy spawn " + spawn.getPosition() + OUTSIDE_THE_MAP));
      }
    }
    for (SpawnPoint spawn : spawns.getLoot()) {
      if (outOfBounds(map, spawn.getX(), spawn.getY())) {
        problems.add(
            new Problem(
                Severity.ERROR,
                about(name) + "loot spawn " + spawn.getPosition() + OUTSIDE_THE_MAP));
      }
    }
    for (Marker marker : spawns.getAllMarkers()) {
      if (outOfBounds(map, marker.position().x, marker.position().y)) {
        problems.add(
            new Problem(
                Severity.ERROR,
                about(name)
                    + "marker of kind '"
                    + marker.kind()
                    + "' at "
                    + marker.position()
                    + OUTSIDE_THE_MAP));
      }
    }
  }

  private static void checkTransitions(
      LevelMapData map, String name, Predicate<String> assetExists, List<Problem> problems) {
    for (RoomTransition transition : map.getTransitions()) {
      String doorway = aboutTransition(name, transition.getId());
      GridPoint2 position = transition.getPosition();

      if (outOfBounds(map, position.x, position.y)) {
        problems.add(new Problem(Severity.ERROR, doorway + " at " + position + OUTSIDE_THE_MAP));
      }

      String destination = transition.getDestinationMap();
      if (destination == null || destination.isBlank()) {
        problems.add(new Problem(Severity.ERROR, doorway + " has no destination"));
      } else if (assetExists != null && !assetExists.test(destination)) {
        problems.add(
            new Problem(
                Severity.ERROR, doorway + " leads to '" + destination + "', which does not exist"));
      }

      if (transition.getDestinationSpawn() == null) {
        problems.add(
            new Problem(
                Severity.WARNING,
                doorway
                    + " names no arrival tile, so the player lands on the destination's own"
                    + " player spawn"));
      }
    }
  }

  private static void checkTextures(
      LevelMapData map, String name, Predicate<String> assetExists, List<Problem> problems) {
    if (assetExists == null) {
      return;
    }
    for (String texture : map.getTexturePaths()) {
      if (!assetExists.test(texture)) {
        problems.add(
            new Problem(
                Severity.ERROR, about(name) + "uses texture '" + texture + "', which is missing"));
      }
    }
  }

  private static boolean outOfBounds(LevelMapData map, int x, int y) {
    return x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight();
  }

  private static boolean isSolid(LevelMapData map, int x, int y) {
    MapLayerData layer = map.getCollisionLayer();
    if (layer == null) {
      return false;
    }
    TileDefinition tile = layer.get(x, y);
    return tile != null && tile.type().getCollisionType() == CollisionType.SOLID;
  }

  /** Ground, a one-way platform and a ladder all stop the player falling out of the level. */
  private static boolean isSupporting(LevelMapData map, int x, int y) {
    MapLayerData layer = map.getCollisionLayer();
    if (layer == null) {
      return false;
    }
    TileDefinition tile = layer.get(x, y);
    if (tile == null) {
      return false;
    }
    CollisionType collision = tile.type().getCollisionType();
    return collision == CollisionType.SOLID || collision == CollisionType.PLATFORM;
  }
}
