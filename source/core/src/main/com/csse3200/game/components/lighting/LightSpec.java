package com.csse3200.game.components.lighting;

import com.badlogic.gdx.graphics.Color;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import java.util.Optional;

public record LightSpec(Color color, float radius, int rays, float flicker) {

  public static Optional<LightSpec> fromTile(TileDefinition def) {
    if (!def.has("light")) return Optional.empty();
    Color color;
    try {
      color = Color.valueOf(def.get("lightColor", "ffffff"));
    } catch (RuntimeException e) {
      color = Color.WHITE;
    }
    float radius = Math.max(0.5f, def.getFloat("lightRadius", 2f));
    int rays = Math.max(8, Math.round(radius * 8));
    float flicker = Math.max(0f, Math.min(1f, def.getFloat("flicker", 0f)));
    return Optional.of(new LightSpec(color, radius, rays, flicker));
  }
}
