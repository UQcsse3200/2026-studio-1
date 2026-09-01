package com.csse3200.game.areas.terrain.map;

/**
 * Thrown when a map file cannot be found, read, or parsed into a valid {@link LevelMapData}.
 * Callers can catch this to handle invalid or missing maps gracefully (e.g. fall back to a default
 * level).
 */
public class MapLoadException extends RuntimeException {
  public MapLoadException(String message) {
    super(message);
  }

  public MapLoadException(String message, Throwable cause) {
    super(message, cause);
  }
}
