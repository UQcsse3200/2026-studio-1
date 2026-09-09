package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.LevelGameArea.SolidRectangle;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class LevelGameAreaCollisionTest {
  private static final TileDefinition WALL = new TileDefinition(TileType.WALL, null);
  private static final TileDefinition PLATFORM = new TileDefinition(TileType.PLATFORM, null);

  @Test
  void mergesStraightWallIntoOneRectangle() {
    MapLayerData collisionLayer = new MapLayerData("collision", 4, 5);
    fill(collisionLayer, 1, 0, 2, 5, WALL);

    List<SolidRectangle> rectangles = LevelGameArea.findSolidRectangles(collisionLayer);

    assertEquals(List.of(new SolidRectangle(1, 0, 2, 5)), rectangles);
  }

  @Test
  void doesNotMergeAcrossEmptySpace() {
    MapLayerData collisionLayer = new MapLayerData("collision", 1, 5);
    fill(collisionLayer, 0, 0, 1, 2, WALL);
    fill(collisionLayer, 0, 3, 1, 2, WALL);

    List<SolidRectangle> rectangles = LevelGameArea.findSolidRectangles(collisionLayer);

    assertEquals(
        List.of(new SolidRectangle(0, 0, 1, 2), new SolidRectangle(0, 3, 1, 2)), rectangles);
  }

  @Test
  void keepsFloorAndSideWallsAsContinuousRectangles() {
    MapLayerData collisionLayer = new MapLayerData("collision", 5, 4);
    fill(collisionLayer, 0, 0, 5, 1, WALL);
    fill(collisionLayer, 0, 1, 1, 3, WALL);
    fill(collisionLayer, 4, 1, 1, 3, WALL);

    List<SolidRectangle> rectangles = LevelGameArea.findSolidRectangles(collisionLayer);

    assertEquals(3, rectangles.size());
    assertTrue(rectangles.contains(new SolidRectangle(0, 0, 5, 1)));
    assertTrue(rectangles.contains(new SolidRectangle(0, 1, 1, 3)));
    assertTrue(rectangles.contains(new SolidRectangle(4, 1, 1, 3)));
  }

  @Test
  void leavesPlatformsForOneWayPlatformCollisionGeneration() {
    MapLayerData collisionLayer = new MapLayerData("collision", 3, 1);
    fill(collisionLayer, 0, 0, 3, 1, PLATFORM);

    assertTrue(LevelGameArea.findSolidRectangles(collisionLayer).isEmpty());
  }

  private static void fill(
      MapLayerData layer,
      int startX,
      int startY,
      int width,
      int height,
      TileDefinition definition) {
    for (int x = startX; x < startX + width; x++) {
      for (int y = startY; y < startY + height; y++) {
        layer.set(x, y, definition);
      }
    }
  }
}
