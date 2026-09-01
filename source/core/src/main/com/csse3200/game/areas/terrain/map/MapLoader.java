package com.csse3200.game.areas.terrain.map;

/**
 * Loads a level map from a structured data file into a {@link LevelMapData}.
 *
 * <p>Defined as an interface so alternative formats and future map systems (TMX, overworld maps,
 * map transitions) can provide their own loading logic while presenting the same API to the rest of
 * the game. {@link JsonMapLoader} is the default JSON implementation.
 */
public interface MapLoader {
  /**
   * Load and parse a map file.
   *
   * @param path the asset path of the map file (resolved as an internal file)
   * @return the parsed map data
   * @throws MapLoadException if the file is missing, unreadable, or invalid
   */
  LevelMapData load(String path);
}
