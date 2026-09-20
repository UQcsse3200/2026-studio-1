package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;

/**
 * Render a tiled terrain for a given tiled map and orientation. A terrain is a map of tiles that
 * shows the 'ground' in the game. Enabling/disabling this component will show/hide the terrain.
 */
public class TerrainComponent extends RenderComponent {
  private static final int TERRAIN_LAYER = 0;

  private final TiledMap tiledMap;
  private final TiledMapRenderer tiledMapRenderer;
  private final OrthographicCamera camera;
  private final TerrainOrientation orientation;
  private final float tileSize;

  public TerrainComponent(
      OrthographicCamera camera,
      TiledMap map,
      TiledMapRenderer renderer,
      TerrainOrientation orientation,
      float tileSize) {
    this.camera = camera;
    this.tiledMap = map;
    this.orientation = orientation;
    this.tileSize = tileSize;
    this.tiledMapRenderer = renderer;
  }

  public Vector2 tileToWorldPosition(GridPoint2 tilePos) {
    return tileToWorldPosition(tilePos.x, tilePos.y);
  }

  public Vector2 tileToWorldPosition(int x, int y) {
    switch (orientation) {
      case HEXAGONAL:
        float hexLength = tileSize / 2;
        float yOffset = (x % 2 == 0) ? 0.5f * tileSize : 0f;
        return new Vector2(x * (tileSize + hexLength) / 2, y + yOffset);
      case ISOMETRIC:
        return new Vector2((x + y) * tileSize / 2, (y - x) * tileSize / 2);
      case ORTHOGONAL:
        return new Vector2(x * tileSize, y * tileSize);
      default:
        return null;
    }
  }

  /**
   * The tile drawn at a position in a named layer.
   *
   * <p>Rendered tiles are not the same thing as gameplay tiles: this returns whatever the renderer
   * holds, which for a decorative layer is decoration. To ask what the level is like at a position,
   * use {@code LevelView} instead, which reads the collision layer and answers in terms of
   * behaviour.
   *
   * @param layerName the layer to read, such as "terrain" or "background"
   * @param x tile x
   * @param y tile y
   * @return the tile, or null if that layer or cell is empty
   */
  public TerrainTile getTile(String layerName, int x, int y) {
    if (!(tiledMap.getLayers().get(layerName) instanceof TiledMapTileLayer layer)) {
      return null;
    }

    TiledMapTileLayer.Cell cell = layer.getCell(x, y);

    if (cell == null || cell.getTile() == null) {
      return null;
    }

    return (TerrainTile) cell.getTile();
  }

  public float getTileSize() {
    return tileSize;
  }

  /**
   * The size of the rendered map in tiles.
   *
   * <p>Every layer of a map shares its dimensions, so this reads the first one.
   *
   * @return width and height in tiles, or (0, 0) for a map with no tile layers
   */
  public GridPoint2 getMapBounds() {
    for (int i = 0; i < tiledMap.getLayers().getCount(); i++) {
      if (tiledMap.getLayers().get(i) instanceof TiledMapTileLayer layer) {
        return new GridPoint2(layer.getWidth(), layer.getHeight());
      }
    }
    return new GridPoint2();
  }

  public TiledMap getMap() {
    return tiledMap;
  }

  @Override
  public void draw(SpriteBatch batch) {
    tiledMapRenderer.setView(camera);
    tiledMapRenderer.render();
  }

  @Override
  public void dispose() {
    tiledMap.dispose();
    super.dispose();
  }

  @Override
  public float getZIndex() {
    return 0f;
  }

  @Override
  public int getLayer() {
    return TERRAIN_LAYER;
  }

  public enum TerrainOrientation {
    ORTHOGONAL,
    ISOMETRIC,
    HEXAGONAL
  }
}
