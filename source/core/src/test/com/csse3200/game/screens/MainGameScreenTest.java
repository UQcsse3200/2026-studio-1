package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MainGameScreenTest {
  @Test
  void usesMountainTitlesForRoomTwo() {
    assertEquals("BASE", MainGameScreen.subLevelTitle("maps/level2.json", false));
    assertEquals("SKIES", MainGameScreen.subLevelTitle("maps/level2.json", true));
  }

  @Test
  void preservesUnderworldTitlesForRoomOne() {
    assertEquals("DUNGEON", MainGameScreen.subLevelTitle("maps/level1-greek.json", false));
    assertEquals("NETHER", MainGameScreen.subLevelTitle("maps/level1-greek.json", true));
  }

  @Test
  void doesNotUseUnderworldTitlesForOtherRooms() {
    assertNull(MainGameScreen.subLevelTitle("maps/level3.json", false));
    assertNull(MainGameScreen.subLevelTitle("maps/level3.json", true));
  }
}
