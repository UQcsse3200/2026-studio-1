package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TerrainComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class GameAreaTest {
  @Test
  void shouldSpawnEntities() {
    GameArea gameArea = areaWith(null);

    ServiceLocator.registerEntityService(new EntityService());
    Entity entity = mock(Entity.class);

    gameArea.spawnEntity(entity);
    verify(entity).create();

    gameArea.dispose();
    verify(entity).dispose();
  }

  @Test
  void shouldSpawnEntityAtTileCornerOrCentre() {
    ServiceLocator.registerEntityService(new EntityService());
    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.getTileSize()).thenReturn(1f);
    when(terrain.tileToWorldPosition(new GridPoint2(2, 3)))
        .thenAnswer(invocation -> new Vector2(2f, 3f));
    GameArea gameArea = areaWith(terrain);

    Entity corner = new Entity();
    corner.setScale(0.5f, 0.5f);
    gameArea.spawnEntityAt(corner, new GridPoint2(2, 3), false, false);
    assertEquals(new Vector2(2f, 3f), corner.getPosition());

    Entity centred = new Entity();
    centred.setScale(0.5f, 0.5f);
    gameArea.spawnEntityAt(centred, new GridPoint2(2, 3), true, true);
    assertEquals(new Vector2(2.25f, 3.25f), centred.getPosition());
    assertEquals(2, gameArea.areaEntities.size());
  }

  @Test
  void shouldPositionWithoutRegistering() {
    TerrainComponent terrain = mock(TerrainComponent.class);
    when(terrain.getTileSize()).thenReturn(2f);
    when(terrain.tileToWorldPosition(new GridPoint2(1, 1)))
        .thenAnswer(invocation -> new Vector2(2f, 2f));
    GameArea gameArea = areaWith(terrain);
    Entity entity = new Entity();
    entity.setScale(1f, 1f);

    gameArea.positionEntityAt(entity, new GridPoint2(1, 1), true, false);

    assertEquals(new Vector2(2.5f, 2f), entity.getPosition());
    assertEquals(0, gameArea.areaEntities.size());
  }

  private static GameArea areaWith(TerrainComponent terrain) {
    GameArea gameArea =
        new GameArea() {
          @Override
          public void create() {
            // Nothing to build: these tests set the terrain directly.
          }
        };
    gameArea.terrain = terrain;
    return gameArea;
  }
}
