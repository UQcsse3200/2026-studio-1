package com.csse3200.game.areas.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class TerrainFactoryTest {
  private static final TileDefinition FLOOR = new TileDefinition(TileType.FLOOR, "floor.png");
  private static final TileDefinition LADDER = new TileDefinition(TileType.LADDER, "ladder.png");
  private static final TileDefinition UNLOADED = new TileDefinition(TileType.WALL, "unloaded.png");
  private static final TileDefinition INVISIBLE = new TileDefinition(TileType.WALL, null);

  private ResourceService resources;
  private CameraComponent cameraComponent;

  @BeforeEach
  void setUp() {
    resources = mock(ResourceService.class);
    ServiceLocator.registerResourceService(resources);
    cameraComponent = mock(CameraComponent.class);
    OrthographicCamera camera = mock(OrthographicCamera.class);
    when(cameraComponent.getCamera()).thenReturn(camera);
  }

  @Test
  void buildsOneTypedTileLayerPerMapLayer() {
    registerTexture("floor.png", 16, 16);
    registerTexture("ladder.png", 16, 16);
    LevelMapData map = map(FLOOR, LADDER, UNLOADED, INVISIBLE);

    try (MockedConstruction<OrthogonalTiledMapRenderer> renderers =
        mockConstruction(OrthogonalTiledMapRenderer.class)) {
      TerrainComponent terrain = new TerrainFactory(cameraComponent).createTerrainFromMap(map);

      assertEquals(1, renderers.constructed().size());
      assertEquals(0.5f, terrain.getTileSize());
      assertEquals(1, terrain.getMap().getLayers().getCount());
      TiledMapTileLayer layer = (TiledMapTileLayer) terrain.getMap().getLayers().get(0);
      assertEquals(TileType.FLOOR, tileAt(layer, 0, 0).getTileType());
      assertEquals(TileType.LADDER, tileAt(layer, 1, 0).getTileType());
      assertNull(layer.getCell(0, 1), "texture not loaded");
      assertNull(layer.getCell(1, 1), "no texture");
      assertEquals(new GridPoint2(2, 2), terrain.getMapBounds());
    }
  }

  @Test
  void usesSmallestTextureWhenSizesAreMixed() {
    registerTexture("floor.png", 16, 16);
    registerTexture("ladder.png", 32, 32);
    LevelMapData map = map(FLOOR, LADDER, null, null);

    try (MockedConstruction<OrthogonalTiledMapRenderer> ignored =
        mockConstruction(OrthogonalTiledMapRenderer.class)) {
      TerrainComponent terrain = new TerrainFactory(cameraComponent).createTerrainFromMap(map);

      TiledMapTileLayer layer = (TiledMapTileLayer) terrain.getMap().getLayers().get(0);
      assertEquals(16, layer.getTileWidth());
      assertEquals(16, layer.getTileHeight());
    }
  }

  @Test
  void fallsBackToDefaultTileSizeWithNoTextures() {
    LevelMapData map = map(INVISIBLE, null, null, null);

    try (MockedConstruction<OrthogonalTiledMapRenderer> ignored =
        mockConstruction(OrthogonalTiledMapRenderer.class)) {
      TerrainComponent terrain = new TerrainFactory(cameraComponent).createTerrainFromMap(map);

      TiledMapTileLayer layer = (TiledMapTileLayer) terrain.getMap().getLayers().get(0);
      assertEquals(16, layer.getTileWidth());
      assertNull(layer.getCell(0, 0));
    }
  }

  @Test
  void createsIsometricRenderer() {
    registerTexture("floor.png", 16, 16);

    try (MockedConstruction<IsometricTiledMapRenderer> renderers =
        mockConstruction(IsometricTiledMapRenderer.class)) {
      new TerrainFactory(cameraComponent, TerrainOrientation.ISOMETRIC)
          .createTerrainFromMap(map(FLOOR, null, null, null));

      assertEquals(1, renderers.constructed().size());
    }
  }

  @Test
  void createsHexagonalRenderer() {
    registerTexture("floor.png", 16, 16);

    try (MockedConstruction<HexagonalTiledMapRenderer> renderers =
        mockConstruction(HexagonalTiledMapRenderer.class)) {
      new TerrainFactory(cameraComponent, TerrainOrientation.HEXAGONAL)
          .createTerrainFromMap(map(FLOOR, null, null, null));

      assertEquals(1, renderers.constructed().size());
    }
  }

  private void registerTexture(String path, int width, int height) {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(width);
    when(texture.getHeight()).thenReturn(height);
    when(resources.getAsset(eq(path), any())).thenReturn(texture);
  }

  /** A 2x2 map with the given tiles at (0,0), (1,0), (0,1) and (1,1); null leaves a cell empty. */
  private static LevelMapData map(
      TileDefinition bottomLeft,
      TileDefinition bottomRight,
      TileDefinition topLeft,
      TileDefinition topRight) {
    TileDefinition[] defs = {bottomLeft, bottomRight, topLeft, topRight};
    MapLayerData layer = new MapLayerData("terrain", 2, 2);
    Map<String, TileDefinition> legend = new LinkedHashMap<>();
    for (int i = 0; i < defs.length; i++) {
      layer.set(i % 2, i / 2, defs[i]);
      if (defs[i] != null) {
        legend.put(String.valueOf(i), defs[i]);
      }
    }
    return LevelMapData.builder("test")
        .tileSize(0.5f)
        .size(2, 2)
        .legend(legend)
        .layers(List.of(layer))
        .build();
  }

  private static TerrainTile tileAt(TiledMapTileLayer layer, int x, int y) {
    return (TerrainTile) layer.getCell(x, y).getTile();
  }
}
