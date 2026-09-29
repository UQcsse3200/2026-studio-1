package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A named point placed on a map by its author, for a system outside the map package to act on.
 *
 * <p>Markers are how a team puts something on a map without the map system knowing what it is. The
 * loader parses a marker, checks its position is inside the map, and serves it on request; it never
 * interprets the {@code kind}, the {@code id}, or the properties. A team declares one in the entity
 * legend and places its symbol in the entities layer:
 *
 * <pre>{@code
 * "entityLegend": {
 *   "N": { "type": "MARKER", "kind": "npc",        "id": "traveler" },
 *   "T": { "type": "MARKER", "kind": "light",      "radius": "6" },
 *   "C": { "type": "MARKER", "kind": "checkpoint", "id": "quarry" }
 * }
 * }</pre>
 *
 * then reads them back with {@link MapSpawns#getMarkers(String)}:
 *
 * <pre>{@code
 * for (Marker marker : mapData.getSpawns().getMarkers("npc")) {
 *   spawnNpc(marker.id(), marker.position());
 * }
 * }</pre>
 *
 * @param kind the group this marker belongs to, lower-cased, used to look it up (e.g. "npc")
 * @param id which one it is within that kind, or null if the kind alone is enough
 * @param position the marker's tile, in world tile coordinates (origin bottom-left, y up)
 * @param properties extra author-supplied values, keyed by name (never null, never modifiable)
 */
public record Marker(String kind, String id, GridPoint2 position, Map<String, String> properties) {

  /** Normalises the kind and properties so callers do not have to. */
  public Marker {
    kind = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    properties =
        properties == null || properties.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  /**
   * Creates a marker with no extra properties.
   *
   * @param kind the group this marker belongs to
   * @param id which one it is within that kind, or null
   * @param position the marker's tile
   */
  public Marker(String kind, String id, GridPoint2 position) {
    this(kind, id, position, Collections.emptyMap());
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
   * @param key the property name
   * @return true if the property is present and reads as true
   */
  public boolean flag(String key) {
    String value = properties.get(key);
    return value != null && Boolean.parseBoolean(value.trim().toLowerCase(Locale.ROOT));
  }

  /**
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
