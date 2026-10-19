package com.csse3200.game.areas.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class TerrainComponentTest {
  @Test
  void shouldConvertPositionOrthogonal() {
    TerrainComponent component = makeComponent(TerrainOrientation.ORTHOGONAL, 3f);
    assertEquals(new Vector2(0f, 0f), component.tileToWorldPosition(0, 0));
    assertEquals(new Vector2(6f, 12f), component.tileToWorldPosition(2, 4));
    assertEquals(new Vector2(-15f, -9f), component.tileToWorldPosition(-5, -3));
  }

  @Test
  void shouldConvertPositionIsometric() {
    TerrainComponent component = makeComponent(TerrainOrientation.ISOMETRIC, 3f);
    assertEquals(new Vector2(0f, 0f), component.tileToWorldPosition(0, 0));
    assertEquals(new Vector2(9f, 3f), component.tileToWorldPosition(2, 4));
    assertEquals(new Vector2(-12f, 3f), component.tileToWorldPosition(-5, -3));
  }

  @Test
  void shouldConvertPositionHexagonal() {
    TerrainComponent component = makeComponent(TerrainOrientation.HEXAGONAL, 3f);
    assertEquals(new Vector2(0f, 1.5f), component.tileToWorldPosition(0, 0));
    assertEquals(new Vector2(2.25f, 0f), component.tileToWorldPosition(1, 0));
    assertEquals(new Vector2(4.5f, 5.5f), component.tileToWorldPosition(2, 4));
  }

  @Test
  void shouldConvertGridPointPosition() {
    TerrainComponent component = makeComponent(TerrainOrientation.ORTHOGONAL, 0.5f);
    assertEquals(new Vector2(1f, 1.5f), component.tileToWorldPosition(new GridPoint2(2, 3)));
  }

  @Test
  void shouldReturnTileFromNamedLayer() {
    TiledMap map = new TiledMap();
    TiledMapTileLayer layer = new TiledMapTileLayer(4, 4, 16, 16);
    layer.setName("terrain");
    TerrainTile tile = new TerrainTile(null, TileType.FLOOR);
    TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
    cell.setTile(tile);
    layer.setCell(1, 2, cell);
    layer.setCell(3, 3, new TiledMapTileLayer.Cell());
    map.getLayers().add(layer);
    TerrainComponent component = makeComponent(map);

    assertSame(tile, component.getTile("terrain", 1, 2));
    assertNull(component.getTile("terrain", 0, 0), "empty cell");
    assertNull(component.getTile("terrain", 3, 3), "cell without a tile");
    assertNull(component.getTile("missing", 1, 2), "unknown layer");
  }

  @Test
  void shouldIgnoreNonTileLayersForTilesAndBounds() {
    TiledMap map = new TiledMap();
    MapLayer objects = new MapLayer();
    objects.setName("objects");
    map.getLayers().add(objects);
    map.getLayers().add(new TiledMapTileLayer(6, 3, 16, 16));
    TerrainComponent component = makeComponent(map);

    assertNull(component.getTile("objects", 0, 0));
    assertEquals(new GridPoint2(6, 3), component.getMapBounds());
  }

  @Test
  void shouldReportEmptyBoundsWithNoTileLayers() {
    TerrainComponent component = makeComponent(new TiledMap());
    assertEquals(new GridPoint2(0, 0), component.getMapBounds());
  }

  @Test
  void shouldExposeMapSizeAndLayering() {
    TiledMap map = new TiledMap();
    TerrainComponent component = makeComponent(map);

    assertSame(map, component.getMap());
    assertEquals(3f, component.getTileSize());
    assertEquals(0f, component.getZIndex());
    assertEquals(0, component.getLayer());
  }

  @Test
  void shouldDrawThroughRenderer() {
    OrthographicCamera camera = mock(OrthographicCamera.class);
    TiledMapRenderer renderer = mock(TiledMapRenderer.class);
    TerrainComponent component =
        new TerrainComponent(camera, new TiledMap(), renderer, TerrainOrientation.ORTHOGONAL, 1f);

    component.draw(mock(SpriteBatch.class));

    verify(renderer).setView(camera);
    verify(renderer).render();
  }

  @Test
  void shouldDisposeMap() {
    ServiceLocator.registerRenderService(mock(RenderService.class));
    TiledMap map = mock(TiledMap.class);
    TerrainComponent component = makeComponent(map);

    component.dispose();

    verify(map).dispose();
  }

  private static TerrainComponent makeComponent(TiledMap map) {
    return new TerrainComponent(
        mock(OrthographicCamera.class),
        map,
        mock(TiledMapRenderer.class),
        TerrainOrientation.ORTHOGONAL,
        3f);
  }

  private static TerrainComponent makeComponent(TerrainOrientation orientation, float tileSize) {
    OrthographicCamera camera = mock(OrthographicCamera.class);
    TiledMap map = mock(TiledMap.class);
    TiledMapRenderer mapRenderer = mock(TiledMapRenderer.class);
    return new TerrainComponent(camera, map, mapRenderer, orientation, tileSize);
  }
}
