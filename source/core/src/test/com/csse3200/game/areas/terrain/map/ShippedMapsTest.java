package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  @Test
  void everySymbolUsedByAShippedMapIsDeclared() {
    List<String> failures = new ArrayList<>();
    JsonReader reader = new JsonReader();

    for (String mapPath : shippedMaps()) {
      JsonValue root = reader.parse(Gdx.files.internal(mapPath));
      JsonValue layers = root.get("layers");
      if (layers == null) {
        continue;
      }

      Set<Character> tileSymbols = declaredSymbols(root.get("legend"));
      Set<Character> entitySymbols = declaredSymbols(root.get("entityLegend"));
      for (JsonValue layer = layers.child; layer != null; layer = layer.next) {
        Set<Character> declared = "entities".equals(layer.name) ? entitySymbols : tileSymbols;
        int rowNumber = 0;
        for (JsonValue row = layer.child; row != null; row = row.next, rowNumber++) {
          String cells = row.asString();
          for (int column = 0; column < cells.length(); column++) {
            char symbol = cells.charAt(column);
            if (symbol != ' ' && !declared.contains(symbol)) {
              failures.add(
                  mapPath
                      + " layer '"
                      + layer.name
                      + "' uses undeclared symbol '"
                      + symbol
                      + "' at row "
                      + rowNumber
                      + ", column "
                      + column);
            }
          }
        }
      }
    }

    assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
  }

  private static Set<Character> declaredSymbols(JsonValue legend) {
    Set<Character> symbols = new HashSet<>();
    if (legend == null) {
      return symbols;
    }
    for (JsonValue entry = legend.child; entry != null; entry = entry.next) {
      if (entry.name != null && entry.name.length() == 1) {
        symbols.add(entry.name.charAt(0));
      }
    }
    return symbols;
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
