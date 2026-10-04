package com.csse3200.game.components.lighting;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import java.util.Optional;

public record LightSpec(Color color, float radius, int rays, float flicker) {

  public static LightSpec of(Color color, float radius, float flicker) {
    float r = Math.max(0.5f, radius);
    return new LightSpec(
        color, r, Math.max(8, Math.round(r * 8)), Math.max(0f, Math.min(1f, flicker)));
  }

  public static Optional<LightSpec> fromTile(TileDefinition def) {
    if (!def.has("light")) return Optional.empty();
    if (!"point".equalsIgnoreCase(def.get("light").trim())) {
      // only point lights exist for now; extend here for cone
    }
    return Optional.of(
        of(
            LightColour.parse(def.get("lightColour"), Color.WHITE),
            def.getFloat("lightRadius", 2f),
            def.getFloat("flicker", 0f)));
  }
}
