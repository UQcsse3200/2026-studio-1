package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Generates a stable, unique identifier for a loot spawn point, combining the room it belongs to
 * with its tile position. Used to track which specific loot spots have already been collected,
 * independent of session state.
 */
public class LootId {

  /**
   * Builds a stable ID for a loot spawn point.
   *
   * @param roomName the name of the room/map this spawn point belongs to
   * @param position the tile position of the spawn point
   * @return a stable identifier, e.g. "BoxForest:14,7"
   */
  public static String of(String roomName, GridPoint2 position) {
    return roomName + ":" + position.x + "," + position.y;
  }

  private LootId() {
    throw new IllegalStateException("Utility class");
  }
}
