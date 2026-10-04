package com.csse3200.game.components.room;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Placement rules for the enemy spawns in the shipped Level 1 map. Data tests: they load the map
 * and ask the level view about tiles, so no physics world is needed.
 *
 * <p>Geometry, copied from the real code: {@code GameArea.positionEntityAt} centres the entity's
 * bounding box on the spawn tile, and {@code PhysicsUtils.setScaledCollider} puts the collider at
 * the horizontal centre and the BOTTOM of that box. So the collider's feet sit below the spawn
 * tile's bottom edge by (entity height / 2 - tile / 2) minus the collider's own bottom offset.
 */
@ExtendWith(GameExtension.class)
class LevelOneEnemySpawnPlacementTest {
  private static final String MAP = "maps/level1-greek.json";
  private static final double TILE = 0.5; // world units per tile, from the map file's tileSize
  private static final double EPS = 1e-6;

  // Physics lifts a body out of a surface it starts inside, so feet may start up to half a tile
  // below the top of the tile they stand on (every skeleton does: 2 x 0.5 tile entity offset).
  private static final double FEET_SINK_ALLOWANCE = 0.5;
  private static final double MAX_DROP = 1.5; // tiles an enemy may fall before it lands

  /** Entity size and collider, in WORLD units, copied from NPCFactory. */
  private record Body(
      double scaleX,
      double scaleY,
      double colliderWidth,
      double colliderHeight,
      double colliderBottom) {}

  /** Collider rectangle in TILE units. */
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

  private static final Map<String, Body> BODIES =
      Map.of(
          "skeleton",
          new Body(1.0, 1.0, 0.45, 0.6, 0.0),
          "ranged-skeleton",
          new Body(1.0, 1.0, 0.45, 0.6, 0.0),
          "minotaur",
          new Body(
              4.0,
              MINOTAUR_HEIGHT,
              4.0 * 0.8,
              MINOTAUR_HEIGHT * 0.7 - MINOTAUR_FEET,
              MINOTAUR_FEET),
          "cyclops",
          new Body(2.0, 1.5, 2.0 * 0.4, 1.5 * 0.5, 0.0),
          "centaur",
          new Body(4.0, 4.0, 4.0 * 0.4, 4.0 * 0.5, 0.0));

  private LevelView level;
  private List<SpawnPoint> enemies;

  @BeforeEach
  void setUp() {
    LevelMapData map = new JsonMapLoader().load(MAP);
    level = new MapDataLevelView(map);
    enemies = map.getSpawns().getEnemies();
    // No count and no map size here: the list changes as enemies are added or moved.
    assertFalse(enemies.isEmpty(), "Level 1 lists no enemies.");
  }

  @Test
  void everyEnemySpawnIsInsideTheMap() {
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      if (!level.inBounds(spawn.getX(), spawn.getY())) {
        problems.add(
            describe(spawn) + " is outside the " + level.width() + " by " + level.height());
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noEnemyColliderIsInsideAWallOrCeiling() {
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      Box box = boxOf(spawn);
      for (GridPoint2 tile : tilesTouching(box)) {
        // How far above the collider's feet the tile's top edge is. A small value means the feet
        // are resting in the surface; a larger one means the tile is beside or above the body.
        double riseAboveFeet = (tile.y + 1) - box.bottom();
        if (level.isSolid(tile.x, tile.y) && riseAboveFeet > FEET_SINK_ALLOWANCE + EPS) {
          problems.add(
              describe(spawn) + " overlaps a wall or floor tile at " + tile.x + ", " + tile.y);
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyEnemyLandsOnASurfaceWithinOneAndAHalfTiles() {
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      Box box = boxOf(spawn);
      Double supportTop = supportTopUnder(box);
      if (supportTop == null) {
        problems.add(describe(spawn) + " has nothing to stand on");
      } else if (box.bottom() - supportTop > MAX_DROP + EPS) {
        problems.add(
            describe(spawn) + " falls " + (box.bottom() - supportTop) + " tiles before landing");
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
  }

  @Test
  void everyEnemyHasHeadroomForItsCollider() {
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      Box box = boxOf(spawn);
      Double supportTop = supportTopUnder(box);
      if (supportTop == null) {
        continue; // reported by everyEnemyLandsOnASurfaceWithinOneAndAHalfTiles
      }
      // Once landed, the body occupies [supportTop, supportTop + height) over its footprint.
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
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noEnemyStartsInsideAHazard() {
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      for (GridPoint2 tile : tilesTouching(boxOf(spawn))) {
        if (level.isHazard(tile.x, tile.y)) {
          problems.add(describe(spawn) + " starts in a hazard tile at " + tile.x + ", " + tile.y);
        }
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void noTwoEnemiesShareATile() {
    Map<String, SpawnPoint> seen = new HashMap<>();
    List<String> problems = new ArrayList<>();
    for (SpawnPoint spawn : enemies) {
      SpawnPoint earlier = seen.put(spawn.getX() + "," + spawn.getY(), spawn);
      if (earlier != null) {
        problems.add(describe(spawn) + " is on the same tile as " + describe(earlier));
      }
    }
    assertTrue(problems.isEmpty(), () -> String.join("\n", problems));
  }

  @Test
  void everyEnemyTypeHasARegisteredFactory() {
    // Same call the game makes at start up. Registering only stores factories: nothing is built,
    // so no atlases or physics are needed. The registry is static, so resetRegistry cleans up.
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
    DefaultEntitySpawns.registerAll();

    Set<String> unknown = new LinkedHashSet<>();
    for (SpawnPoint spawn : enemies) {
      if (!EntitySpawnRegistry.isRegistered(spawn.getType())) {
        unknown.add(spawn.getType());
      }
    }
    assertTrue(
        unknown.isEmpty(),
        () ->
            "No spawn factory is registered for "
                + unknown
                + ". Registered: "
                + EntitySpawnRegistry.registeredNames());
  }

  private static String describe(SpawnPoint spawn) {
    return spawn.getType() + " at " + spawn.getX() + ", " + spawn.getY();
  }

  private static Body bodyOf(String type) {
    Body body = BODIES.get(type);
    assertNotNull(body, "No collider recorded for enemy type '" + type + "': add it to BODIES");
    return body;
  }

  /** The collider rectangle, in tiles, once the game has centred the entity on the spawn tile. */
  private static Box boxOf(SpawnPoint spawn) {
    Body body = bodyOf(spawn.getType());
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
  private Double supportTopUnder(Box box) {
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
