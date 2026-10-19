package com.csse3200.game.components.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LightSpecTest {
  @Test
  void ofKeepsValidValues() {
    LightSpec spec = LightSpec.of(Color.RED, 3f, 0.4f);

    assertEquals(Color.RED, spec.color());
    assertEquals(3f, spec.radius());
    assertEquals(24, spec.rays());
    assertEquals(0.4f, spec.flicker());
  }

  @Test
  void ofClampsRadiusRaysAndFlicker() {
    LightSpec small = LightSpec.of(Color.WHITE, 0.1f, -1f);
    assertEquals(0.5f, small.radius());
    assertEquals(8, small.rays());
    assertEquals(0f, small.flicker());

    assertEquals(1f, LightSpec.of(Color.WHITE, 2f, 5f).flicker());
  }

  @Test
  void fromTileIsEmptyWithoutLightProperty() {
    TileDefinition def = new TileDefinition(TileType.DECORATIVE, "torch.png");

    assertTrue(LightSpec.fromTile(def).isEmpty());
  }

  @Test
  void fromTileReadsLightProperties() {
    TileDefinition def =
        new TileDefinition(
            TileType.DECORATIVE,
            "torch.png",
            Map.of("light", "point", "lightRadius", "4", "flicker", "0.25"));

    Optional<LightSpec> spec = LightSpec.fromTile(def);

    assertTrue(spec.isPresent());
    assertEquals(4f, spec.get().radius());
    assertEquals(0.25f, spec.get().flicker());
    assertEquals(Color.WHITE, spec.get().color());
  }

  @Test
  void fromTileAcceptsOtherLightKinds() {
    TileDefinition def =
        new TileDefinition(TileType.DECORATIVE, "torch.png", Map.of("light", "cone"));

    Optional<LightSpec> spec = LightSpec.fromTile(def);

    assertTrue(spec.isPresent());
    assertEquals(2f, spec.get().radius());
    assertEquals(0f, spec.get().flicker());
  }
}
