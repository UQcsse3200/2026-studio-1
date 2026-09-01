package com.csse3200.game.areas.terrain.map;

/**
 * A single named layer of a level map (e.g. "background", "terrain", "foreground", "collision").
 *
 * <p>Holds a grid of {@link TileDefinition}s indexed by tile coordinate, where {@code (x, y)} uses
 * the game's world convention: x increases rightwards, y increases upwards, origin bottom-left. A
 * {@code null} entry means the cell is empty in this layer.
 */
public class MapLayerData {
  private final String name;
  private final int width;
  private final int height;
  private final TileDefinition[][] tiles;

  /**
   * @param name the layer name
   * @param width layer width in tiles
   * @param height layer height in tiles
   */
  public MapLayerData(String name, int width, int height) {
    this.name = name;
    this.width = width;
    this.height = height;
    this.tiles = new TileDefinition[width][height];
  }

  public String getName() {
    return name;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /**
   * @return true if {@code (x, y)} lies within this layer's bounds
   */
  public boolean inBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  /**
   * Get the tile definition at a cell.
   *
   * @return the {@link TileDefinition}, or null if the cell is empty or out of bounds
   */
  public TileDefinition get(int x, int y) {
    if (!inBounds(x, y)) {
      return null;
    }
    return tiles[x][y];
  }

  /**
   * Set the tile definition at a cell. Out-of-bounds writes are ignored.
   *
   * @param x tile x coordinate
   * @param y tile y coordinate
   * @param definition the tile definition, or null to clear the cell
   */
  public void set(int x, int y, TileDefinition definition) {
    if (inBounds(x, y)) {
      tiles[x][y] = definition;
    }
  }
}
