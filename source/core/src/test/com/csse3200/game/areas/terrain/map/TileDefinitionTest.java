package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.terrain.TileType;
import java.util.Map;
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

  @Test
  void hasNoPropertiesByDefault() {
    TileDefinition def = new TileDefinition(TileType.WALL, "wall.png");
    assertTrue(def.properties().isEmpty());
    assertFalse(def.has("damage"));
    assertNull(def.get("damage"));
  }

  @Test
  void carriesAuthorSuppliedProperties() {
    TileDefinition def =
        new TileDefinition(
            TileType.HAZARD, "lava.png", Map.of("damage", "15", "hidden", "true", "light", "4.5"));

    assertTrue(def.has("damage"));
    assertEquals("15", def.get("damage"));
    assertEquals(15, def.getInt("damage", 1));
    assertTrue(def.flag("hidden"));
    assertEquals(4.5f, def.getFloat("light", 0f));
  }

  @Test
  void fallsBackWhenPropertyIsAbsentOrUnparseable() {
    TileDefinition def = new TileDefinition(TileType.WALL, null, Map.of("damage", "heaps"));

    assertEquals(7, def.getInt("damage", 7));
    assertEquals(2.5f, def.getFloat("damage", 2.5f));
    assertEquals(3, def.getInt("missing", 3));
    assertEquals("none", def.get("missing", "none"));
    assertFalse(def.flag("missing"));
  }

  @Test
  void propertiesCannotBeModifiedThroughTheRecord() {
    TileDefinition def = new TileDefinition(TileType.WALL, null, Map.of("a", "b"));
    assertThrows(UnsupportedOperationException.class, () -> def.properties().put("c", "d"));
  }

  @Test
  void nullPropertiesBecomeAnEmptyMap() {
    assertTrue(new TileDefinition(TileType.WALL, null, null).properties().isEmpty());
  }
}
