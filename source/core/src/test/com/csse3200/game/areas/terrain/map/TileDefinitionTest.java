package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.csse3200.game.areas.terrain.TileType;
import org.junit.jupiter.api.Test;

class TileDefinitionTest {
  @Test
  void exposesTypeAndTexture() {
    TileDefinition def = new TileDefinition(TileType.WALL, "wall.png");
    assertEquals(TileType.WALL, def.type());
    assertEquals("wall.png", def.texture());
  }

  @Test
  void allowsNullTexture() {
    TileDefinition def = new TileDefinition(TileType.HAZARD, null);
    assertEquals(TileType.HAZARD, def.type());
    assertNull(def.texture());
  }

  @Test
  void valueEqualityHolds() {
    assertEquals(
        new TileDefinition(TileType.FLOOR, "f.png"), new TileDefinition(TileType.FLOOR, "f.png"));
  }
}
