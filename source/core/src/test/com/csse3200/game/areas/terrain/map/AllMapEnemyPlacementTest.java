package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The Level 1 enemy placement rules, applied to EVERY map in {@code assets/maps}, so a new room or
 * an edited level cannot start an enemy inside a wall, over a pit or with nowhere to stand.
 *
 * <p>It generalises {@code LevelOneEnemySpawnPlacementTest}, which keeps its own copy for Level 1.
 * The method is the same: from the spawn tile work out where the enemy's collider will be (the game
 * centres the entity on the tile), then check it against the map. For each enemy:
 *
 * <ol>
 *   <li>it starts inside the map
 *   <li>its collider is not inside a wall, floor or ceiling, allowing the feet to start up to half
 *       a tile in a surface (physics lifts a body out of a surface it starts in)
 *   <li>it lands on a supporting surface within one and a half tiles of where it starts
 *   <li>once landed, its collider fits: no solid tile in the space it occupies
 *   <li>it does not start in a hazard tile
 *   <li>no two enemies share a tile (a saved kill is keyed by map name and tile)
 * </ol>
 *
 * <p><b>Flying enemies</b> (harpy, ranged harpy) hover, so rules 2 and 3 do not apply. For them the
 * test checks that the two tiles around the spawn, each way, are free of walls and floors, so they
 * do not start stuck. A one way ledge nearby is allowed.
 *
 * <p><b>Collider sizes</b> are copied from {@code NPCFactory} in the table below: entity scale and
 * the collider fractions, in world units. When an enemy's size changes in the factory, change it
 * here. An enemy type missing from the table fails with a clear message instead of being skipped.
 *
 * <p>The map list is read from the folder, so a new map is covered the moment its file exists.
 */
@ExtendWith(GameExtension.class)
class AllMapsEnemyPlacementTest {
  private static final double TILE = 0.5; // world units per tile; every shipped map uses 0.5
  private static final double EPS = 1e-6;
  private static final double FEET_SINK_ALLOWANCE = 0.5; // tiles a body may start inside a surface
  private static final double MAX_DROP = 1.5; // tiles an enemy may fall before it lands
  private static final int FLYER_CLEARANCE = 2; // tiles each way

  private static final Set<String> FLYERS = Set.of("harpy", "ranged-harpy");

  /** Entity size and collider, in WORLD units, copied from NPCFactory. */
  private record Body(
      double scaleX,
      double scaleY,
      double colliderWidth,
      double colliderHeight,
      double colliderBottom) {}

  private record Box(double left, double bottom, double width, double height) {
    double right() {
      return left + width;
    }

    double top() {
      return bottom + height;
    }
  }

  // The Minotaur builds its own collider (NPCFactory.createMinotaur): 16 px shorter from below.
  private static final double MINOTAUR_HEIGHT = 4.0 * 80.0 / 96.0;
  private static final double MINOTAUR_FEET = 16.0 * (MINOTAUR_HEIGHT / 80.0);
  private static final double CENTAUR_HEIGHT = 4.0 * 53.0 / 66.0;
  private static final double CERBERUS_HEIGHT = 1.5 * 96.0 / 150.0;

  private static final Map<String, Body> BODIES = new HashMap<>();

  static {
    BODIES.put("skeleton", new Body(1.0, 1.0, 0.45, 0.6, 0.0));
    BODIES.put("ranged-skeleton", new Body(1.0, 1.0, 0.45, 0.6, 0.0));
    BODIES.put(
        "minotaur",
        new Body(
            4.0, MINOTAUR_HEIGHT, 4.0 * 0.8, MINOTAUR_HEIGHT * 0.7 - MINOTAUR_FEET, MINOTAUR_FEET));
    BODIES.put("centaur", new Body(4.0, CENTAUR_HEIGHT, 4.0 * 0.4, CENTAUR_HEIGHT * 0.5, 0.0));
    BODIES.put("cyclops", new Body(2.0, 1.5, 2.0 * 0.4, 1.5 * 0.5, 0.0));
    BODIES.put("medusa", new Body(2.0, 1.5, 2.0 * 0.4, 1.5 * 0.5, 0.0));
    BODIES.put("zeus", new Body(2.0, 1.5, 2.0 * 0.4, 1.5 * 0.5, 0.0));
    BODIES.put("cerberus", new Body(1.5, CERBERUS_HEIGHT, 1.5 * 0.45, CERBERUS_HEIGHT * 0.6, 0.0));
  }

  private final JsonMapLoader loader = new JsonMapLoader();

  static Stream<Arguments> everyMap() {
    List<Arguments> maps = new ArrayList<>();
    for (FileHandle file : Gdx.files.internal("maps").list()) {
      if (file.name().endsWith(".json")) {
        maps.add(Arguments.of("maps/" + file.name()));
      }
    }
    maps.sort((a, b) -> ((String) a.get()[0]).compareTo((String) b.get()[0]));
    return maps.stream();
  }

  // ---------- the rules, one test each ----------

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void everyEnemySpawnIsInsideTheMap(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies(path)) {
      if (!level.inBounds(spawn.getX(), spawn.getY())) {
        problems.add(
            describe(spawn) + " is outside the " + level.width() + " by " + level.height());
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void everyEnemyTypeHasAColliderInTheTable(String path) {
    List<String> unknown = new ArrayList<>();
    for (SpawnPoint spawn : enemies(path)) {
      if (!FLYERS.contains(spawn.getType()) && !BODIES.containsKey(spawn.getType())) {
        unknown.add(spawn.getType());
      }
    }
    assertTrue(unknown.isEmpty(), path + ": add collider sizes to BODIES for " + unknown);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void noGroundEnemyColliderIsInsideAWallOrCeiling(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : groundEnemies(path)) {
      Box box = boxOf(spawn);
      for (GridPoint2 tile : tilesTouching(box)) {
        double riseAboveFeet = (tile.y + 1) - box.bottom();
        if (level.isSolid(tile.x, tile.y) && riseAboveFeet > FEET_SINK_ALLOWANCE + EPS) {
          problems.add(
              describe(spawn) + " overlaps a wall or floor tile at " + tile.x + ", " + tile.y);
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void everyGroundEnemyLandsOnASurfaceWithinOneAndAHalfTiles(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : groundEnemies(path)) {
      Box box = boxOf(spawn);
      Double supportTop = supportTopUnder(level, box);
      if (supportTop == null) {
        problems.add(describe(spawn) + " has nothing to stand on");
      } else if (box.bottom() - supportTop > MAX_DROP + EPS) {
        problems.add(
            describe(spawn) + " falls " + (box.bottom() - supportTop) + " tiles before landing");
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void everyGroundEnemyHasHeadroomForItsCollider(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : groundEnemies(path)) {
      Box box = boxOf(spawn);
      Double supportTop = supportTopUnder(level, box);
      if (supportTop == null) {
        continue; // reported by the landing rule
      }
      Box landed = new Box(box.left(), supportTop, box.width(), box.height());
      for (GridPoint2 tile : tilesTouching(landed)) {
        if (level.isSolid(tile.x, tile.y)) {
          problems.add(
              describe(spawn)
                  + " does not fit above its surface: tile "
                  + tile.x
                  + ", "
                  + tile.y
                  + " is solid");
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void noGroundEnemyStartsInsideAHazard(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : groundEnemies(path)) {
      for (GridPoint2 tile : tilesTouching(boxOf(spawn))) {
        if (level.isHazard(tile.x, tile.y)) {
          problems.add(describe(spawn) + " starts in a hazard tile at " + tile.x + ", " + tile.y);
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void noFlyingEnemyStartsStuckInAWallOrFloor(String path) {
    LevelView level = view(path);
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies(path)) {
      if (!FLYERS.contains(spawn.getType())) {
        continue;
      }
      for (int x = spawn.getX() - FLYER_CLEARANCE; x <= spawn.getX() + FLYER_CLEARANCE; x++) {
        for (int y = spawn.getY() - FLYER_CLEARANCE; y <= spawn.getY() + FLYER_CLEARANCE; y++) {
          if (level.inBounds(x, y) && level.isSolid(x, y)) {
            problems.add(
                describe(spawn)
                    + " has a solid tile at "
                    + x
                    + ", "
                    + y
                    + " within "
                    + FLYER_CLEARANCE
                    + " tiles");
          }
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void noTwoEnemiesShareATile(String path) {
    Map<String, SpawnPoint> seen = new HashMap<>();
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies(path)) {
      SpawnPoint earlier = seen.put(spawn.getX() + "," + spawn.getY(), spawn);
      if (earlier != null) {
        problems.add(describe(spawn) + " is on the same tile as " + describe(earlier));
      }
    }
    assertTrue(problems.isEmpty(), () -> path + "\n" + String.join("\n", problems));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyMap")
  void everyMapHasAtLeastOneThingToCheck(String path) {
    // A map with no enemies at all would pass every rule above without being checked. Level 3 and
    // the
    // side rooms all have enemies; if a map is meant to be empty, remove it from this rule.
    assertFalse(enemies(path).isEmpty(), path + " lists no enemies");
  }

  // ---------- helpers ----------

  private LevelView view(String path) {
    return new MapDataLevelView(loader.load(path));
  }

  private List<SpawnPoint> enemies(String path) {
    LevelMapData map = loader.load(path);
    assertNotNull(map.getSpawns(), path + " has no spawn data");
    return map.getSpawns().getEnemies();
  }

  private List<SpawnPoint> groundEnemies(String path) {
    List<SpawnPoint> ground = new ArrayList<>();
    for (SpawnPoint spawn : enemies(path)) {
      if (!FLYERS.contains(spawn.getType()) && BODIES.containsKey(spawn.getType())) {
        ground.add(spawn);
      }
    }
    return ground;
  }

  private static String describe(SpawnPoint spawn) {
    return spawn.getType() + " at " + spawn.getX() + ", " + spawn.getY();
  }

  /** The collider rectangle, in tiles, once the game has centred the entity on the spawn tile. */
  private static Box boxOf(SpawnPoint spawn) {
    Body body = BODIES.get(spawn.getType());
    double entityLeft = spawn.getX() * TILE + (TILE - body.scaleX()) / 2;
    double entityBottom = spawn.getY() * TILE + (TILE - body.scaleY()) / 2;
    double left = entityLeft + (body.scaleX() - body.colliderWidth()) / 2; // centred in x
    double bottom = entityBottom + body.colliderBottom(); // bottom aligned
    return new Box(
        left / TILE, bottom / TILE, body.colliderWidth() / TILE, body.colliderHeight() / TILE);
  }

  private static List<GridPoint2> tilesTouching(Box box) {
    List<GridPoint2> tiles = new ArrayList<>();
    for (int x = (int) Math.floor(box.left() + EPS); x < Math.ceil(box.right() - EPS); x++) {
      for (int y = (int) Math.floor(box.bottom() + EPS); y < Math.ceil(box.top() - EPS); y++) {
        tiles.add(new GridPoint2(x, y));
      }
    }
    return tiles;
  }

  /** Top edge, in tiles, of the highest supporting tile under the feet; null if there is none. */
  private static Double supportTopUnder(LevelView level, Box box) {
    Double best = null;
    int startRow = (int) Math.floor(box.bottom() + FEET_SINK_ALLOWANCE + EPS);
    for (int x = (int) Math.floor(box.left() + EPS); x < Math.ceil(box.right() - EPS); x++) {
      for (int y = Math.min(startRow, level.height() - 1); y >= 0; y--) {
        if (level.isSupporting(x, y)) {
          if (best == null || y + 1 > best) {
            best = (double) (y + 1);
          }
          break;
        }
      }
    }
    return best;
  }
}
