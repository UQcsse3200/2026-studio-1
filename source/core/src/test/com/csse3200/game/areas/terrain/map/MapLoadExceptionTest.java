package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class MapLoadExceptionTest {
  @Test
  void messageConstructor() {
    MapLoadException e = new MapLoadException("boom");
    assertEquals("boom", e.getMessage());
  }

  @Test
  void messageAndCauseConstructor() {
    Throwable cause = new IllegalStateException("root");
    MapLoadException e = new MapLoadException("boom", cause);
    assertEquals("boom", e.getMessage());
    assertSame(cause, e.getCause());
  }
}
