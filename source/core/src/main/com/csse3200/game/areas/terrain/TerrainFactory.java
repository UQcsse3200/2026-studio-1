package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.TerrainComponent.TerrainOrientation;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for creating game terrains. */
public class TerrainFactory {
  private static final Logger logger = LoggerFactory.getLogger(TerrainFactory.class);

  private static final int DEFAULT_TILE_PX = 16;

  private final OrthographicCamera camera;
  private final TerrainOrientation orientation;

  /**
   * Create a terrain factory with Orthogonal orientation
   *
   * @param cameraComponent Camera to render terrains to. Must be ortographic.
   */
  public TerrainFactory(CameraComponent cameraComponent) {
    this(cameraComponent, TerrainOrientation.ORTHOGONAL);
  }

  /**
   * Create a terrain factory
   *
   * @param cameraComponent Camera to render terrains to. Must be orthographic.
   * @param orientation orientation to render terrain at
   */
  public TerrainFactory(CameraComponent cameraComponent, TerrainOrientation orientation) {
    this.camera = (OrthographicCamera) cameraComponent.getCamera();
    this.orientation = orientation;
  }

  /**
   * Create a terrain component from parsed, file-loaded map data. Each {@link MapLayerData} becomes
   * a tile layer, drawn back-to-front, with tiles typed by their {@link TileType}.
   *
   * <p>The textures referenced by the map's legend must already be loaded into the {@link
   * ResourceService} before calling this.
   *
   * @param map the parsed level map data
   * @return a terrain component rendering the map
   */
  public TerrainComponent createTerrainFromMap(LevelMapData map) {
    ResourceService resourceService = ServiceLocator.getResourceService();
    float tileWorldSize = map.getTileSize();
    GridPoint2 tilePixelSize = resolveTilePixelSize(map, resourceService);

    TiledMap tiledMap = new TiledMap();
    for (MapLayerData layerData : map.getLayers()) {
      TiledMapTileLayer layer =
          new TiledMapTileLayer(map.getWidth(), map.getHeight(), tilePixelSize.x, tilePixelSize.y);
      for (int x = 0; x < map.getWidth(); x++) {
        for (int y = 0; y < map.getHeight(); y++) {
          TileDefinition def = layerData.get(x, y);
          if (def == null || def.texture() == null) {
            continue;
          }
          Texture texture = resourceService.getAsset(def.texture(), Texture.class);
          if (texture == null) {
            continue;
          }
          TerrainTile tile = new TerrainTile(new TextureRegion(texture), def.type());
          Cell cell = new Cell();
          cell.setTile(tile);
          layer.setCell(x, y, cell);
        }
      }
      tiledMap.getLayers().add(layer);
    }

    float tileScale = tileWorldSize / tilePixelSize.x;
    TiledMapRenderer renderer = createRenderer(tiledMap, tileScale);
    return new TerrainComponent(camera, tiledMap, renderer, orientation, tileWorldSize);
  }

  /**
   * Determines the pixel size of a tile from the map's legend textures.
   *
   * <p>A map is expected to use one tile size throughout. When it does not, the smallest texture
   * wins and everything larger is scaled down to match, which is almost never intended, so a map
   * mixing sizes is warned about. Falls back to {@link #DEFAULT_TILE_PX} for a map with no
   * textures, such as one drawn entirely from a composed background image.
   */
  private GridPoint2 resolveTilePixelSize(LevelMapData map, ResourceService resourceService) {
    List<GridPoint2> sizes = legendTextureSizes(map, resourceService);
    if (sizes.isEmpty()) {
      return new GridPoint2(DEFAULT_TILE_PX, DEFAULT_TILE_PX);
    }

    Comparator<GridPoint2> byArea = Comparator.comparingInt(size -> size.x * size.y);
    GridPoint2 smallest = Collections.min(sizes, byArea);
    GridPoint2 largest = Collections.max(sizes, byArea);
    if (!smallest.equals(largest)) {
      logger.warn(
          "Map '{}' mixes tile textures of {}x{} and {}x{}. The whole map is drawn at the smallest"
              + " of these, so the larger art is scaled down. Use one tile size per map.",
          map.getName(),
          smallest.x,
          smallest.y,
          largest.x,
          largest.y);
    }
    return smallest;
  }

  /** The pixel size of every loaded texture the map's legend uses. */
  private static List<GridPoint2> legendTextureSizes(
      LevelMapData map, ResourceService resourceService) {
    List<GridPoint2> sizes = new ArrayList<>();
    for (TileDefinition def : map.getLegend().values()) {
      Texture texture =
          def.texture() == null ? null : resourceService.getAsset(def.texture(), Texture.class);
      if (texture != null) {
        sizes.add(new GridPoint2(texture.getWidth(), texture.getHeight()));
      }
    }
    return sizes;
  }

  private TiledMapRenderer createRenderer(TiledMap tiledMap, float tileScale) {
    switch (orientation) {
      case ORTHOGONAL:
        return new OrthogonalTiledMapRenderer(tiledMap, tileScale);
      case ISOMETRIC:
        return new IsometricTiledMapRenderer(tiledMap, tileScale);
      case HEXAGONAL:
        return new HexagonalTiledMapRenderer(tiledMap, tileScale);
      default:
        return null;
    }
  }
}
