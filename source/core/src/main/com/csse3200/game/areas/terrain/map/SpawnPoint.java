package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;

/**
 * A location in a map where an entity should be spawned, defined in tile coordinates (origin
 * bottom-left, y increasing upwards - matching the game's world coordinates).
 *
 * <p>Used for enemy and loot spawns. An optional {@code type} names what should be spawned (e.g.
 * "ghost", "ghostKing"), letting map files drive spawning without hard-coded positions.
 */
public class SpawnPoint {
  private final String type;
  private final int x;
  private final int y;

  /**
   * @param type identifier for what to spawn (may be null)
   * @param x tile x coordinate
   * @param y tile y coordinate
   */
  public SpawnPoint(String type, int x, int y) {
    this.type = type;
    this.x = x;
    this.y = y;
  }

  /**
   * @return the spawn identifier, or null if unspecified
   */
  public String getType() {
    return type;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  /**
   * @return this spawn's tile position as a {@link GridPoint2}
   */
  public GridPoint2 getPosition() {
    return new GridPoint2(x, y);
  }

  @Override
  public String toString() {
    return "SpawnPoint{type=" + type + ", x=" + x + ", y=" + y + "}";
  }
}
