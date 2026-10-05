package com.csse3200.game.components.lighting;

import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.components.lighting.LightingConfig.Ambient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LightingConfigParser {
  private static final Logger logger = LoggerFactory.getLogger(LightingConfigParser.class);

  /**
   * Parse the json and create a lighting config
   *
   * @param json the json to parse
   * @param mapName the name of the map
   * @return the lighting config
   */
  public static LightingConfig parse(JsonValue json, String mapName) {
    if (json == null) return null;

    // get the ambient light
    LightingConfig.Ambient ambient =
        parseAmbient(json.get("ambient"), LightingConfig.DEFAULT_AMBIENT);

    // get sublevel ambient lighting
    Map<String, LightingConfig.Ambient> perSubLevel = new LinkedHashMap<>();
    JsonValue subs = json.get("subLevels");
    if (subs != null) {
      for (JsonValue s = subs.child; s != null; s = s.next) {
        JsonValue a = s.get("ambient");
        if (a != null) perSubLevel.put(s.name, parseAmbient(a, ambient));
      }
    }

    // get the player lighting
    LightSpec player = parsePlayer(json.get("player"));
    logger.info("Map '{}' has lighting ({} sub-level overrides)", mapName, perSubLevel.size());

    return new LightingConfig(ambient, player, perSubLevel);
  }

  /**
   * Get the ambient lighting from the json file, setting a default otherwise
   *
   * @param a the json
   * @param fallback the fallback lighting
   * @return the ambient lighting
   */
  private static LightingConfig.Ambient parseAmbient(JsonValue a, Ambient fallback) {
    // fallback
    if (a == null) return fallback;

    // Get the colour and intensity of the ambient light
    String c = a.getString("colour", a.getString("color", null));
    float intensity = Math.max(0f, Math.min(1f, readFloat(a, "intensity", fallback.intensity())));
    return new Ambient(LightColour.parse(c, fallback.color()), intensity);
  }

  /**
   * Parses the players lighting from the json for a level
   *
   * @param p the json
   * @return the LightSpec of the players lighting
   */
  private static LightSpec parsePlayer(JsonValue p) {
    // fallback default
    if (p == null) {
      return LightSpec.of(LightColour.ELYSIAN_FADE.getColour(), 3f, 0f);
    }
    // get the colour of the light
    String c = p.getString("colour", p.getString("color", null));
    // return the light (with a fallback colour)
    return LightSpec.of(
        LightColour.parse(c, LightColour.ELYSIAN_FADE.getColour()),
        p.getFloat("radius", 3f),
        p.getFloat("flicker", 0f));
  }

  /**
   * read a float from the json without throwing
   *
   * @param json the json to read
   * @param key the key to read
   * @param fallback the fallback
   * @return the float read
   */
  private static float readFloat(JsonValue json, String key, float fallback) {
    try {
      return json.getFloat(key, fallback);
    } catch (RuntimeException e) {
      logger.warn("Lighting value '{}' is not a number, using {}", key, fallback);
      return fallback;
    }
  }
}
