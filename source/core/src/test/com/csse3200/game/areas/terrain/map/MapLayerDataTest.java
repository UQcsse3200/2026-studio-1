package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.terrain.TileType;
import org.junit.jupiter.api.Test;

class MapLayerDataTest {
  private static final TileDefinition TILE = new TileDefinition(TileType.WALL, "wall.png");

  @Test
  void storesNameAndDimensions() {
    MapLayerData layer = new MapLayerData("terrain", 4, 3);
    assertEquals("terrain", layer.getName());
    assertEquals(4, layer.getWidth());
    assertEquals(3, layer.getHeight());
  }

  @Test
  void setAndGetTileWithinBounds() {
    MapLayerData layer = new MapLayerData("t", 2, 2);
    layer.set(1, 1, TILE);
    assertSame(TILE, layer.get(1, 1));
    assertNull(layer.get(0, 0)); // untouched cell
  }

  @Test
  void inBoundsChecksEdges() {
    MapLayerData layer = new MapLayerData("t", 2, 2);
    assertTrue(layer.inBounds(0, 0));
    assertTrue(layer.inBounds(1, 1));
    assertFalse(layer.inBounds(-1, 0));
    assertFalse(layer.inBounds(0, -1));
    assertFalse(layer.inBounds(2, 0));
    assertFalse(layer.inBounds(0, 2));
  }

  @Test
  void outOfBoundsGetReturnsNull() {
    MapLayerData layer = new MapLayerData("t", 2, 2);
    assertNull(layer.get(5, 5));
    assertNull(layer.get(-1, -1));
  }

  @Test
  void outOfBoundsSetIsIgnored() {
    MapLayerData layer = new MapLayerData("t", 2, 2);
    layer.set(9, 9, TILE); // must not throw
    assertNull(layer.get(9, 9));
  }
}
