package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainGameScreenTest {
  private final JsonMapLoader loader = new JsonMapLoader();

  private LevelView level(String mapPath) {
    return new MapDataLevelView(loader.load(mapPath));
  }

  @Test
  void usesMountainTitlesForLevelTwo() {
    LevelView levelTwo = level("maps/level2.json");

    assertEquals("BASE", MainGameScreen.subLevelTitle(levelTwo, false));
    assertEquals("SKIES", MainGameScreen.subLevelTitle(levelTwo, true));
  }

  @Test
  void preservesUnderworldTitlesForLevelOne() {
    LevelView levelOne = level("maps/level1-greek.json");

    assertEquals("DUNGEON", MainGameScreen.subLevelTitle(levelOne, false));
    assertEquals("NETHER", MainGameScreen.subLevelTitle(levelOne, true));
  }

  @Test
  void hasNoTitlesForALevelThatIsOnePlace() {
    LevelView levelThree = level("maps/level3.json");

    assertNull(MainGameScreen.subLevelTitle(levelThree, false));
    assertNull(MainGameScreen.subLevelTitle(levelThree, true));
  }

  @Test
  void hasNoTitlesWithoutALevel() {
    assertNull(MainGameScreen.subLevelTitle(null, false));
  }
}
