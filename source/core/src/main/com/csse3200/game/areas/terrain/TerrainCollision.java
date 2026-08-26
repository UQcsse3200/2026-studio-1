package com.csse3200.game.areas.terrain;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ObstacleFactory;
import java.util.ArrayList;
import java.util.List;

public class TerrainCollision {
  private final TerrainComponent terrain;

  public TerrainCollision(TerrainComponent terrain) {
    this.terrain = terrain;
  }

  public List<Entity> createColliders() {
    List<Entity> colliders = new ArrayList<>();

    TiledMapTileLayer layer = (TiledMapTileLayer) terrain.getMap().getLayers().get(0);

    float tileSize = terrain.getTileSize();

    for (int x = 0; x < layer.getWidth(); x++) {
      for (int y = 0; y < layer.getHeight(); y++) {
        TiledMapTileLayer.Cell cell = layer.getCell(x, y);

        if (cell == null || cell.getTile() == null) {
          continue;
        }

        TerrainTile tile = (TerrainTile) cell.getTile();

        if (tile.getTileType().getCollisionType() == CollisionType.SOLID) {
          Entity collider = ObstacleFactory.createWall(tileSize, tileSize);

          Vector2 position = terrain.tileToWorldPosition(x, y);
          collider.setPosition(position);

          colliders.add(collider);
        }
      }
    }

    return colliders;
  }
}
