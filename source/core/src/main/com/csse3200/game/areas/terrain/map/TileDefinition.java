package com.csse3200.game.areas.terrain.map;

import com.csse3200.game.areas.terrain.TileType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An entry in a map's legend: the pairing of a tile {@link TileType} with the texture used to draw
 * it, plus any extra properties the map author attached to that symbol. A single symbol in a map
 * file resolves to one {@code TileDefinition}, shared by every cell that uses the symbol.
 *
 * <p>Properties are how a feature adds per-tile data without changing this record, the loader, or
 * {@link TileType}. Any key in a legend entry other than {@code type} and {@code texture} is
 * carried here unchanged, and the map system never interprets it. For example:
 *
 * <pre>{@code
 * "~": { "type": "HAZARD", "texture": "river-styx.png", "damage": "15" },
 * "W": { "type": "WALL",   "texture": "wall.png",       "hidden": "true" },
 * "o": { "type": "DECORATIVE", "texture": "lamp.png",   "light": "4.5" }
 * }</pre>
 *
 * <p>which the owning feature reads back as {@code getInt("damage", 10)}, {@code flag("hidden")}
 * and {@code getFloat("light", 0f)}.
 *
 * @param type the gameplay category of the tile
 * @param texture the asset path of the tile's texture (may be null for non-visual tiles)
 * @param properties extra author-supplied values, keyed by name (never null, never modifiable)
 */
public record TileDefinition(TileType type, String texture, Map<String, String> properties) {

  /** Normalises {@code properties} so callers never have to null-check it. */
  public TileDefinition {
    properties =
        properties == null || properties.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  /**
   * Creates a definition with no extra properties.
   *
   * @param type the gameplay category of the tile
   * @param texture the asset path of the tile's texture, or null
   */
  public TileDefinition(TileType type, String texture) {
    this(type, texture, Collections.emptyMap());
  }

  /**
   * @param key the property name
   * @return true if the map author set this property at all
   */
  public boolean has(String key) {
    return properties.containsKey(key);
  }

  /**
   * @param key the property name
   * @return the raw value, or null if the author did not set it
   */
  public String get(String key) {
    return properties.get(key);
  }

  /**
   * @param key the property name
   * @param fallback the value to use when the property is absent
   * @return the value, or {@code fallback} if absent
   */
  public String get(String key, String fallback) {
    String value = properties.get(key);
    return value == null ? fallback : value;
  }

  /**
   * Reads a property as a boolean flag. Anything other than {@code "true"} (ignoring case) is
   * false, including an absent property, so a flag is safe to read on any tile.
   *
   * @param key the property name
   * @return true if the property is present and reads as true
   */
  public boolean flag(String key) {
    String value = properties.get(key);
    return value != null && Boolean.parseBoolean(value.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Reads a property as a whole number.
   *
   * @param key the property name
   * @param fallback the value to use when the property is absent or not a number
   * @return the parsed value, or {@code fallback}
   */
  public int getInt(String key, int fallback) {
    String value = properties.get(key);
    if (value == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  /**
   * Reads a property as a decimal number.
   *
   * @param key the property name
   * @param fallback the value to use when the property is absent or not a number
   * @return the parsed value, or {@code fallback}
   */
  public float getFloat(String key, float fallback) {
    String value = properties.get(key);
    if (value == null) {
      return fallback;
    }
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
