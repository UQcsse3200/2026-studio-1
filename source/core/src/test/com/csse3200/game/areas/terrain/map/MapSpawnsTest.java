package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import org.junit.jupiter.api.Test;

class MapSpawnsTest {
  @Test
  void defaultsAreEmpty() {
    MapSpawns spawns = new MapSpawns();
    assertNull(spawns.getPlayer());
    assertTrue(spawns.getEnemies().isEmpty());
    assertTrue(spawns.getLoot().isEmpty());
  }

  @Test
  void storesPlayerEnemiesAndLoot() {
    MapSpawns spawns = new MapSpawns();
    spawns.setPlayer(new GridPoint2(3, 4));
    spawns.addEnemy(new SpawnPoint("ghost", 1, 2));
    spawns.addLoot(new SpawnPoint(null, 5, 6));

    assertEquals(new GridPoint2(3, 4), spawns.getPlayer());
    assertEquals(1, spawns.getEnemies().size());
    assertEquals("ghost", spawns.getEnemies().get(0).getType());
    assertEquals(1, spawns.getLoot().size());
    assertEquals(5, spawns.getLoot().get(0).getX());
  }

  @Test
  void enemyAndLootListsAreUnmodifiable() {
    MapSpawns spawns = new MapSpawns();
    SpawnPoint sp = new SpawnPoint("ghost", 0, 0);
    assertThrows(UnsupportedOperationException.class, () -> spawns.getEnemies().add(sp));
    assertThrows(UnsupportedOperationException.class, () -> spawns.getLoot().add(sp));
  }
}
