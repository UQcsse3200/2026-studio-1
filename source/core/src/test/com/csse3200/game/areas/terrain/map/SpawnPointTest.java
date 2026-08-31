package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import org.junit.jupiter.api.Test;

class SpawnPointTest {
  @Test
  void exposesTypeAndCoordinates() {
    SpawnPoint sp = new SpawnPoint("ghost", 3, 7);
    assertEquals("ghost", sp.getType());
    assertEquals(3, sp.getX());
    assertEquals(7, sp.getY());
    assertEquals(new GridPoint2(3, 7), sp.getPosition());
  }

  @Test
  void allowsNullType() {
    SpawnPoint sp = new SpawnPoint(null, 0, 0);
    assertNull(sp.getType());
  }

  @Test
  void toStringIncludesFields() {
    String text = new SpawnPoint("ghost", 1, 2).toString();
    assertTrue(text.contains("ghost"));
    assertTrue(text.contains("1"));
    assertTrue(text.contains("2"));
  }
}
