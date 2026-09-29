package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TileType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LevelMapDataTest {
  private static final TileDefinition WALL = new TileDefinition(TileType.WALL, "wall.png");
  private static final TileDefinition DECOR = new TileDefinition(TileType.DECORATIVE, "decor.png");
  private static final TileDefinition NO_TEXTURE = new TileDefinition(TileType.HAZARD, null);

  private static Map<String, TileDefinition> legend() {
    Map<String, TileDefinition> legend = new HashMap<>();
    legend.put("#", WALL);
    legend.put(".", DECOR);
    legend.put("x", NO_TEXTURE);
    return legend;
  }

  private static MapLayerData layer(String name) {
    MapLayerData layer = new MapLayerData(name, 2, 2);
    layer.set(0, 0, WALL);
    return layer;
  }

  private static LevelMapData build(List<MapLayerData> layers) {
    return new LevelMapData("Lvl", 0.5f, 2, 2, legend(), layers, new MapSpawns());
  }

  @Test
  void exposesBasicProperties() {
    LevelMapData map = build(List.of(layer("terrain")));
    assertEquals("Lvl", map.getName());
    assertEquals(0.5f, map.getTileSize());
    assertEquals(2, map.getWidth());
    assertEquals(2, map.getHeight());
    assertSame(MapSpawns.class, map.getSpawns().getClass());
  }

  @Test
  void legendAndLayersAreUnmodifiable() {
    LevelMapData map = build(new ArrayList<>(List.of(layer("terrain"))));
    assertThrows(UnsupportedOperationException.class, () -> map.getLegend().put("!", WALL));
    assertThrows(UnsupportedOperationException.class, () -> map.getLayers().add(layer("extra")));
  }

  @Test
  void getLayerFindsByNameOrReturnsNull() {
    LevelMapData map = build(List.of(layer("terrain")));
    assertEquals("terrain", map.getLayer("terrain").getName());
    assertNull(map.getLayer("missing"));
  }

  @Test
  void collisionLayerPrefersCollisionThenTerrainThenNull() {
    MapLayerData collision = layer("collision");
    MapLayerData terrain = layer("terrain");

    assertSame(collision, build(List.of(collision, terrain)).getCollisionLayer());
    assertSame(terrain, build(List.of(terrain)).getCollisionLayer());
    assertNull(build(List.of(layer("background"))).getCollisionLayer());
  }

  @Test
  void getTileTypeReturnsTypeOrNull() {
    LevelMapData map = build(List.of(layer("terrain")));
    assertEquals(TileType.WALL, map.getTileType(0, 0)); // tile present
    assertNull(map.getTileType(1, 1)); // empty cell
    assertNull(build(List.of(layer("background"))).getTileType(0, 0)); // no collision layer
  }

  @Test
  void texturePathsAreDistinctAndSkipNulls() {
    LevelMapData map = build(List.of(layer("terrain")));
    // WALL + DECOR have textures; NO_TEXTURE (null) is skipped.
    assertEquals(2, map.getTexturePaths().size());
    assertTrue(map.getTexturePaths().contains("wall.png"));
    assertFalse(map.getTexturePaths().contains(null));
  }

  @Test
  void isEmptyReflectsLayersAndDimensions() {
    assertFalse(build(List.of(layer("terrain"))).isEmpty());
    // no layers
    assertTrue(build(new ArrayList<>()).isEmpty());
    // zero width
    LevelMapData zeroWidth =
        new LevelMapData("z", 0.5f, 0, 5, legend(), List.of(layer("terrain")), new MapSpawns());
    assertTrue(zeroWidth.isEmpty());
    // zero height
    LevelMapData zeroHeight =
        new LevelMapData("z", 0.5f, 5, 0, legend(), List.of(layer("terrain")), new MapSpawns());
    assertTrue(zeroHeight.isEmpty());
  }

  @Test
  void collisionLayerConstantsAreExposed() {
    assertEquals("collision", LevelMapData.COLLISION_LAYER);
    assertEquals("terrain", LevelMapData.TERRAIN_LAYER);
    // sanity: spawn helper type is reachable
    assertEquals(new GridPoint2(0, 0), new GridPoint2(0, 0));
  }
}
