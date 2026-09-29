package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import java.util.Set;
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

  @Test
  void hasNoMarkersUntilSomeAreAdded() {
    MapSpawns spawns = new MapSpawns();

    assertTrue(spawns.getMarkers("npc").isEmpty());
    assertTrue(spawns.getAllMarkers().isEmpty());
    assertTrue(spawns.getMarkerKinds().isEmpty());
  }

  @Test
  void groupsMarkersByKind() {
    MapSpawns spawns = new MapSpawns();
    spawns.addMarker(new Marker("npc", "traveler", new GridPoint2(4, 13)));
    spawns.addMarker(new Marker("npc", "trader", new GridPoint2(9, 13)));
    spawns.addMarker(new Marker("light", null, new GridPoint2(2, 20)));

    assertEquals(2, spawns.getMarkers("npc").size());
    assertEquals(1, spawns.getMarkers("light").size());
    assertEquals(3, spawns.getAllMarkers().size());
    assertEquals(Set.of("npc", "light"), spawns.getMarkerKinds());
  }

  @Test
  void looksUpMarkersIgnoringCaseAndUnknownKinds() {
    MapSpawns spawns = new MapSpawns();
    spawns.addMarker(new Marker("checkpoint", "quarry", new GridPoint2(1, 1)));

    assertEquals(1, spawns.getMarkers("CHECKPOINT").size());
    assertTrue(spawns.getMarkers("pet").isEmpty());
    assertTrue(spawns.getMarkers(null).isEmpty());
  }
}
