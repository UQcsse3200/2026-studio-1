package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MapValidatorTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  private List<MapValidator.Problem> validate(String json) {
    return MapValidator.validate(loader.parse(json));
  }

  private static boolean mentions(List<MapValidator.Problem> problems, String fragment) {
    return problems.stream().anyMatch(problem -> problem.message().contains(fragment));
  }

  private static List<MapValidator.Problem> errors(List<MapValidator.Problem> problems) {
    return MapValidator.errorsIn(problems);
  }

  @Test
  void findsNothingWrongWithASoundMap() {
    String json =
        """
        {
          "name": "Sound",
          "legend": { "#": { "type": "WALL" } },
          "entityLegend": { "P": { "type": "PLAYER" } },
          "layers": {
            "terrain":  ["  ", "##"],
            "entities": ["P ", "  "]
          }
        }
        """;
    assertTrue(validate(json).isEmpty());
  }

  @Test
  void reportsAMapWithNoCollisionLayer() {
    // "terrian" is the misspelling this check exists to catch.
    String json =
        """
        {
          "name": "Typo",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrian": ["##"] }
        }
        """;
    List<MapValidator.Problem> problems = validate(json);

    assertEquals(1, errors(problems).size());
    assertTrue(mentions(problems, "nothing in it is solid"));
  }

  @Test
  void reportsAPlayerSpawnOutsideTheMap() {
    String json =
        """
        {
          "name": "Adrift",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##"] },
          "spawns": { "player": { "x": 40, "y": 40 } }
        }
        """;
    assertTrue(mentions(errors(validate(json)), "player spawn (40, 40) is outside the map"));
  }

  @Test
  void warnsAboutAPlayerSpawnWithNothingUnderIt() {
    String json =
        """
        {
          "name": "Midair",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["  ", "  ", "##"] },
          "spawns": { "player": { "x": 0, "y": 2 } }
        }
        """;
    List<MapValidator.Problem> problems = validate(json);

    assertTrue(mentions(problems, "has no ground beneath it"));
    // A judgement call, so it must not fail a build.
    assertTrue(errors(problems).isEmpty());
  }

  @Test
  void warnsAboutAPlayerSpawnInsideAWall() {
    String json =
        """
        {
          "name": "Entombed",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##", "##"] },
          "spawns": { "player": { "x": 1, "y": 1 } }
        }
        """;
    assertTrue(mentions(validate(json), "is inside a solid tile"));
  }

  @Test
  void acceptsAPlayerStandingOnAPlatform() {
    String json =
        """
        {
          "name": "Ledge",
          "legend": { "=": { "type": "PLATFORM" } },
          "layers": { "terrain": ["  ", "=="] },
          "spawns": { "player": { "x": 0, "y": 1 } }
        }
        """;
    assertTrue(validate(json).isEmpty());
  }

  @Test
  void reportsEnemyLootAndMarkerSpawnsOutsideTheMap() {
    String json =
        """
        {
          "name": "Strays",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##"] },
          "spawns": {
            "enemies": [ { "type": "skeleton", "x": 9, "y": 9 } ],
            "loot":    [ { "x": 8, "y": 8 } ]
          }
        }
        """;
    List<MapValidator.Problem> problems = validate(json);

    assertEquals(2, errors(problems).size());
    assertTrue(mentions(problems, "enemy spawn"));
    assertTrue(mentions(problems, "loot spawn"));
  }

  @Test
  void reportsADoorwayWithNoDestination() {
    String json =
        """
        {
          "name": "Nowhere",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##"] },
          "transitions": [ { "id": "door", "x": 0, "y": 0, "destinationMap": "maps/gone.json" } ]
        }
        """;
    // Without a file check the destination is accepted; with one it is an error.
    assertTrue(errors(validate(json)).isEmpty());

    List<MapValidator.Problem> checked =
        MapValidator.validate(loader.parse(json), Set.of("maps/level2.json")::contains);

    assertTrue(mentions(errors(checked), "which does not exist"));
  }

  @Test
  void reportsATextureThatIsNotThere() {
    String json =
        """
        {
          "name": "Bare",
          "legend": { "#": { "type": "WALL", "texture": "images/nope.png" } },
          "layers": { "terrain": ["##"] }
        }
        """;
    List<MapValidator.Problem> checked = MapValidator.validate(loader.parse(json), path -> false);

    assertTrue(mentions(errors(checked), "images/nope.png"));
  }

  @Test
  void warnsWhenAMapDefinesNoPlayerSpawn() {
    String json =
        """
        {
          "name": "Empty",
          "legend": { "#": { "type": "WALL" } },
          "layers": { "terrain": ["##"] }
        }
        """;
    List<MapValidator.Problem> problems = validate(json);

    assertTrue(mentions(problems, "defines no player spawn"));
    assertTrue(errors(problems).isEmpty());
  }

  @Test
  void reportsANullMap() {
    assertEquals(1, errors(MapValidator.validate(null)).size());
  }
}
