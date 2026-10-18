package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LevelGameAreaSpawnNpcsTest {
  @Test
  void shouldNotCrashWhenNoLevelIsLoaded() throws Exception {
    LevelGameArea area = new LevelGameArea(mock(TerrainFactory.class), "maps/test.json");
    assertNull(area.getLevel(), "no map should be loaded before create()");

    Method spawnNpcs = LevelGameArea.class.getDeclaredMethod("spawnNpcs");
    spawnNpcs.setAccessible(true);

    assertDoesNotThrow(() -> spawnNpcs.invoke(area));
  }
}
