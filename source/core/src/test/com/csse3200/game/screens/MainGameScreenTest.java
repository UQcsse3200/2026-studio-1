package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.ui.terminal.commands.WhereCommand;
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

  @Test
  void whereLocationIsUnavailableWithoutAnAreaPlayerOrLevel() {
    LevelGameArea area = mock(LevelGameArea.class);
    String mapPath = "maps/level1-greek.json";

    assertNull(MainGameScreen.whereLocation(null, mapPath));
    assertNull(MainGameScreen.whereLocation(area, mapPath));

    when(area.getPlayer()).thenReturn(mock(Entity.class));
    assertNull(MainGameScreen.whereLocation(area, mapPath));
  }

  @Test
  void whereLocationUsesTheCurrentTileAndSublevel() {
    LevelGameArea area = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    SubLevel nether =
        new SubLevel("nether", "NETHER", new SubLevel.Bounds(0, 0, 56, 32), null, null, null);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(3.9f, 9.2f));
    when(level.tileSize()).thenReturn(0.5f);
    when(level.subLevelAt(18)).thenReturn(nether);
    when(level.name()).thenReturn("Level 1 - Out of the Underworld");

    assertEquals(
        new WhereCommand.Location(
            "Level 1 - Out of the Underworld", "maps/level1-greek.json", "nether", 7, 18),
        MainGameScreen.whereLocation(area, "maps/level1-greek.json"));
  }

  @Test
  void whereLocationReportsNoneOutsideSublevelsAndFloorsNegativeCoordinates() {
    LevelGameArea area = mock(LevelGameArea.class);
    Entity player = mock(Entity.class);
    LevelView level = mock(LevelView.class);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(-0.1f, 2.99f));
    when(level.tileSize()).thenReturn(1f);
    when(level.name()).thenReturn("Level 2 - Mount Olympus");

    assertEquals(
        new WhereCommand.Location("Level 2 - Mount Olympus", "maps/level2.json", "none", -1, 2),
        MainGameScreen.whereLocation(area, "maps/level2.json"));
  }
}
