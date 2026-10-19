package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SubLevelTest {
  private static final SubLevel.Bounds BOUNDS = new SubLevel.Bounds(2, 10, 5, 4);

  @Test
  void boundsReportBottomAndTop() {
    assertEquals(10, BOUNDS.bottom());
    assertEquals(14, BOUNDS.top());
  }

  @Test
  void boundsContainTilesInsideOnly() {
    assertTrue(BOUNDS.contains(2, 10));
    assertTrue(BOUNDS.contains(6, 13));
    assertFalse(BOUNDS.contains(1, 11), "left of the rectangle");
    assertFalse(BOUNDS.contains(7, 11), "right edge is exclusive");
    assertFalse(BOUNDS.contains(3, 9), "below the rectangle");
    assertFalse(BOUNDS.contains(3, 14), "top edge is exclusive");
  }

  @Test
  void containsRowIgnoresX() {
    SubLevel subLevel = new SubLevel("dungeon", "DUNGEON", BOUNDS, null, null, null);

    assertTrue(subLevel.containsRow(10));
    assertTrue(subLevel.containsRow(13));
    assertFalse(subLevel.containsRow(9));
    assertFalse(subLevel.containsRow(14));
  }
}
