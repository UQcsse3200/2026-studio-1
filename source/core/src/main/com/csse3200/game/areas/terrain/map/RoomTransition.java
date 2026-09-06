package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Describes a doorway in a level map and where it sends the player.
 *
 * <p>The doorway position and size are measured in tiles. The destination spawn is also a tile
 * coordinate, but belongs to the destination map. Keeping this data in the map file means final
 * doorway artwork and room layouts can be changed without changing transition code.
 */
public class RoomTransition {
  private final String id;
  private final GridPoint2 position;
  private final int width;
  private final int height;
  private final String texture;
  private final String destinationMap;
  private final GridPoint2 destinationSpawn;

  public RoomTransition(
      String id,
      GridPoint2 position,
      int width,
      int height,
      String texture,
      String destinationMap,
      GridPoint2 destinationSpawn) {
    this.id = id;
    this.position = new GridPoint2(position);
    this.width = width;
    this.height = height;
    this.texture = texture;
    this.destinationMap = destinationMap;
    this.destinationSpawn = destinationSpawn == null ? null : new GridPoint2(destinationSpawn);
  }

  public String getId() {
    return id;
  }

  public GridPoint2 getPosition() {
    return new GridPoint2(position);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getTexture() {
    return texture;
  }

  public String getDestinationMap() {
    return destinationMap;
  }

  /**
   * @return destination entrance tile, or {@code null} to use the destination map's player spawn
   */
  public GridPoint2 getDestinationSpawn() {
    return destinationSpawn == null ? null : new GridPoint2(destinationSpawn);
  }
}
