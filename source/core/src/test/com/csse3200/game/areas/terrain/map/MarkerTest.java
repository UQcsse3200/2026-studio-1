package com.csse3200.game.areas.terrain.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarkerTest {
  @Test
  void exposesKindIdAndPosition() {
    Marker marker = new Marker("npc", "traveler", new GridPoint2(4, 13));

    assertEquals("npc", marker.kind());
    assertEquals("traveler", marker.id());
    assertEquals(new GridPoint2(4, 13), marker.position());
    assertTrue(marker.properties().isEmpty());
  }

  @Test
  void normalisesKindSoLookupIsCaseInsensitive() {
    assertEquals("checkpoint", new Marker("  CheckPoint ", null, new GridPoint2(0, 0)).kind());
  }

  @Test
  void allowsAMarkerWithNoId() {
    assertNull(new Marker("light", null, new GridPoint2(1, 1)).id());
  }

  @Test
  void carriesPropertiesWithTypedAccess() {
    Marker light =
        new Marker("light", null, new GridPoint2(2, 2), Map.of("radius", "6.5", "flicker", "true"));

    assertEquals(6.5f, light.getFloat("radius", 0f));
    assertTrue(light.flag("flicker"));
    assertEquals("none", light.get("colour", "none"));
    assertEquals(3, light.getInt("missing", 3));
    assertFalse(light.flag("missing"));
  }

  @Test
  void propertiesCannotBeModifiedThroughTheRecord() {
    Marker marker = new Marker("npc", "a", new GridPoint2(0, 0), Map.of("a", "b"));
    assertThrows(UnsupportedOperationException.class, () -> marker.properties().put("c", "d"));
  }
}
