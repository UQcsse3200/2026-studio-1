package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and validates every map the game ships with.
 *
 * <p>This is the check that protects map files from each other. Every other map test asserts
 * something about one particular map; this one asserts that whatever anybody edits, the result
 * still loads and is playable. A map added to {@code assets/maps} is picked up with no change here,
 * and CI runs it on every push, so a broken map fails a pull request instead of a playtest.
 *
 * <p>Errors fail the test. Warnings are judgement calls, so they are logged and left to a human.
 */
@ExtendWith(GameExtension.class)
class ShippedMapsTest {
  private static final Logger logger = LoggerFactory.getLogger(ShippedMapsTest.class);
  private static final String MAPS_DIRECTORY = "maps";

  private final JsonMapLoader loader = new JsonMapLoader();

  @Test
  void everyShippedMapLoadsAndIsPlayable() {
    List<String> maps = shippedMaps();
    assertFalse(maps.isEmpty(), "No maps found in assets/" + MAPS_DIRECTORY);

    List<String> failures = new ArrayList<>();
    for (String mapPath : maps) {
      failures.addAll(problemsIn(mapPath));
    }

    assertTrue(
        failures.isEmpty(),
        () -> "Shipped maps are not playable:\n  " + String.join("\n  ", failures));
  }

  /** Collects the blocking problems in one map, logging anything that is only worth a look. */
  private List<String> problemsIn(String mapPath) {
    List<String> failures = new ArrayList<>();
    LevelMapData map;
    try {
      map = loader.load(mapPath);
    } catch (MapLoadException e) {
      failures.add(mapPath + ": does not load - " + e.getMessage());
      return failures;
    }

    for (MapValidator.Problem problem :
        MapValidator.validate(map, path -> Gdx.files.internal(path).exists())) {
      if (problem.severity() == MapValidator.Severity.ERROR) {
        failures.add(mapPath + ": " + problem.message());
      } else {
        logger.warn("{}: {}", mapPath, problem.message());
      }
    }
    return failures;
  }

  private static List<String> shippedMaps() {
    List<String> maps = new ArrayList<>();
    for (FileHandle file : Gdx.files.internal(MAPS_DIRECTORY).list()) {
      if (file.name().endsWith(".json")) {
        maps.add(MAPS_DIRECTORY + "/" + file.name());
      }
    }
    maps.sort(String::compareTo);
    return maps;
  }
}
