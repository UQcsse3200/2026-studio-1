package com.csse3200.game.components.lighting;

import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.components.lighting.LightingConfig.Ambient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightingConfigParser {
  private static final Logger logger = LoggerFactory.getLogger(LightingConfigParser.class);

  private LightingConfigParser() {}

  /**
   * @return the config, or null if the map has no lighting block
   */
  public static LightingConfig parse(JsonValue json, String mapName) {
    if (json == null) return null;

    LightingConfig.Ambient ambient =
        parseAmbient(json.get("ambient"), LightingConfig.DEFAULT_AMBIENT);

    Map<String, LightingConfig.Ambient> perSubLevel = new LinkedHashMap<>();
    JsonValue subs = json.get("subLevels");
    if (subs != null) {
      for (JsonValue s = subs.child; s != null; s = s.next) {
        JsonValue a = s.get("ambient");
        if (a != null) perSubLevel.put(s.name, parseAmbient(a, ambient));
      }
    }

    LightSpec player = parsePlayer(json.get("player"));
    logger.info("Map '{}' has lighting ({} sub-level overrides)", mapName, perSubLevel.size());
    return new LightingConfig(ambient, player, perSubLevel);
  }

  private static LightingConfig.Ambient parseAmbient(JsonValue a, Ambient fallback) {
    if (a == null) return fallback;
    String c = a.getString("colour", a.getString("color", null));
    float intensity = Math.max(0f, Math.min(1f, a.getFloat("intensity", fallback.intensity())));
    return new Ambient(LightColour.parse(c, fallback.color()), intensity);
  }

  private static LightSpec parsePlayer(JsonValue p) {
    if (p == null) {
      return LightSpec.of(LightColour.ELYSIAN_FADE.getColour(), 3f, 0f);
    }
    String c = p.getString("colour", p.getString("color", null));
    return LightSpec.of(
        LightColour.parse(c, LightColour.ELYSIAN_FADE.getColour()),
        p.getFloat("radius", 3f),
        p.getFloat("flicker", 0f));
  }
}
