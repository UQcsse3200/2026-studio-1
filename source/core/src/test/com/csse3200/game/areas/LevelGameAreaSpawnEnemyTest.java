package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.components.CameraComponent;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LevelGameAreaSpawnEnemyTest {

  @Test
  void spawnNullEnemyAtNullPosition() {
    try (MockedStatic<EntitySpawnRegistry> mockSpawnRegistry =
        mockStatic(EntitySpawnRegistry.class)) {
      when(EntitySpawnRegistry.create(eq(null), any())).thenReturn(null);
      LevelGameArea levelGameArea =
          new LevelGameArea(new TerrainFactory(new CameraComponent()), "");
      boolean spawned = levelGameArea.spawnEnemy(null, null);
      assertFalse(spawned);
      mockSpawnRegistry.verify(() -> EntitySpawnRegistry.create(eq(null), any()));
    }
  }
}
