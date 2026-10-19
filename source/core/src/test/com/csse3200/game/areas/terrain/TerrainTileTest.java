package com.csse3200.game.areas.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile.BlendMode;
import org.junit.jupiter.api.Test;

class TerrainTileTest {
  @Test
  void keepsTextureAndTileTypeFromConstructor() {
    TextureRegion region = mock(TextureRegion.class);
    TerrainTile tile = new TerrainTile(region, TileType.FLOOR);

    assertSame(region, tile.getTextureRegion());
    assertEquals(TileType.FLOOR, tile.getTileType());
    assertEquals(BlendMode.ALPHA, tile.getBlendMode());
    assertEquals(0, tile.getId());
    assertEquals(0f, tile.getOffsetX());
    assertEquals(0f, tile.getOffsetY());
  }

  @Test
  void settersUpdateValues() {
    TerrainTile tile = new TerrainTile(null, TileType.FLOOR);
    TextureRegion region = mock(TextureRegion.class);

    tile.setId(7);
    tile.setBlendMode(BlendMode.NONE);
    tile.setTextureRegion(region);
    tile.setOffsetX(1.5f);
    tile.setOffsetY(-2f);

    assertEquals(7, tile.getId());
    assertEquals(BlendMode.NONE, tile.getBlendMode());
    assertSame(region, tile.getTextureRegion());
    assertEquals(1.5f, tile.getOffsetX());
    assertEquals(-2f, tile.getOffsetY());
  }

  @Test
  void setTileTypeReturnsTheTileForChaining() {
    TerrainTile tile = new TerrainTile(null, TileType.FLOOR);

    assertSame(tile, tile.setTileType(TileType.LADDER));
    assertEquals(TileType.LADDER, tile.getTileType());
  }

  @Test
  void unusedPropertiesAndObjectsAreNull() {
    TerrainTile tile = new TerrainTile(null, TileType.FLOOR);

    assertNull(tile.getProperties());
    assertNull(tile.getObjects());
  }
}
