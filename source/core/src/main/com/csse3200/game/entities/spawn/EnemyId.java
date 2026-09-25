package com.csse3200.game.entities.spawn;

import com.badlogic.gdx.math.GridPoint2;

// Generates a stable, unique identifier for an enemy spawn point, combining the room
// it belongs to with its tile position - used to track which spawns have already been
// killed, independent of session state.
public class EnemyId {

  public static String of(String roomName, GridPoint2 position) {
    return roomName + ":" + position.x + "," + position.y;
  }

  private EnemyId() {
    throw new IllegalStateException("Utility class");
  }
}
