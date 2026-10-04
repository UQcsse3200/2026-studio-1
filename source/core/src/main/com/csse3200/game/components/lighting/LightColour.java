package com.csse3200.game.components.lighting;

import com.badlogic.gdx.graphics.Color;
import java.util.Locale;
import org.slf4j.LoggerFactory;

public enum LightColour {
  AMBIENT_DARK(new Color(0.15f, 0.15f, 0.25f, 1f)),
  RESINOUS_TORCH(new Color(0.85f, 0.58f, 0.38f, 1f)),
  ASHEN_TORCH(new Color(0.74f, 0.53f, 0.40f, 1f)),
  OIL_LAMP(new Color(0.82f, 0.51f, 0.28f, 1f)),
  ELYSIAN_FADE(new Color(0.88f, 0.66f, 0.44f, 1f)),
  LAVA_GLOW(new Color(0.55f, 0.16f, 0.11f, 1f)),
  AMBIENT_LIGHT(new Color(1.0f, 0.95f, 0.74f, 1.0f)),
  AMBIENT_ORANGE(new Color(0.85f, 0.25f, 0.0f, 1.0f));

  private final Color colour;

  LightColour(Color colour) {
    this.colour = colour;
  }

  public Color getColour() {
    return new Color(colour);
  }

  /** Accepts a theme name ("oil_lamp") or hex ("ffcc88"). Never throws. */
  public static Color parse(String value, Color fallback) {
    if (value == null || value.isBlank()) return new Color(fallback);
    String v = value.trim();
    try {
      return valueOf(v.toUpperCase(Locale.ROOT)).getColour();
    } catch (IllegalArgumentException ignored) {
      // not a theme name, try hex
    }
    try {
      return Color.valueOf(v);
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(LightColour.class)
          .warn("Unknown light colour '{}', using fallback", v);
      return new Color(fallback);
    }
  }
}
