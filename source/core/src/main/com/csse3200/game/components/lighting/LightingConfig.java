package com.csse3200.game.components.lighting;

import com.badlogic.gdx.graphics.Color;
import java.util.Map;

public record LightingConfig(Ambient ambient, LightSpec player, Map<String, Ambient> subLevels) {

  public record Ambient(Color color, float intensity) {}

  public static final Ambient DEFAULT_AMBIENT =
      new Ambient(LightColour.AMBIENT_DARK.getColour(), 0.3f);

  /** Ambient for a sub-level id, falling back to the map-wide ambient. */
  public Ambient ambientFor(String subLevelId) {
    Ambient a = subLevelId == null ? null : subLevels.get(subLevelId);
    return a != null ? a : ambient;
  }
}
