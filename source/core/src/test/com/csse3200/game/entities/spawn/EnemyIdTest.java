package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.spawn.enemies.EnemyId;
import org.junit.jupiter.api.Test;

/** Tests {@link EnemyId}, which names an enemy spawn point by its room and tile position. */
class EnemyIdTest {

  @Test
  void shouldCombineRoomNameAndTilePosition() {
    assertEquals("forest:3,4", EnemyId.of("forest", new GridPoint2(3, 4)));
  }

  @Test
  void shouldBeStableForTheSameRoomAndPosition() {
    assertEquals(
        EnemyId.of("forest", new GridPoint2(3, 4)), EnemyId.of("forest", new GridPoint2(3, 4)));
  }

  @Test
  void shouldDifferBetweenRooms() {
    assertNotEquals(
        EnemyId.of("forest", new GridPoint2(3, 4)), EnemyId.of("cave", new GridPoint2(3, 4)));
  }

  @Test
  void shouldDifferBetweenPositions() {
    assertNotEquals(
        EnemyId.of("forest", new GridPoint2(3, 4)), EnemyId.of("forest", new GridPoint2(4, 3)));
  }

  @Test
  void shouldKeepNegativeCoordinates() {
    assertEquals("forest:-1,-2", EnemyId.of("forest", new GridPoint2(-1, -2)));
  }
}
