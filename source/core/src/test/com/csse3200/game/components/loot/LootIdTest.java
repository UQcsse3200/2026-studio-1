package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.GridPoint2;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

/** Tests for {@link LootId}, the stable id saved for each loot spawn point. */
class LootIdTest {
  private static final String LEVEL_ONE = "Level 1 - Out of the Underworld";

  @Test
  void idIsRoomNameThenPosition() {
    assertEquals(LEVEL_ONE + ":12,7", LootId.of(LEVEL_ONE, new GridPoint2(12, 7)));
  }

  @Test
  void samePlaceAlwaysGivesTheSameId() {
    assertEquals(
        LootId.of(LEVEL_ONE, new GridPoint2(4, 9)), LootId.of(LEVEL_ONE, new GridPoint2(4, 9)));
  }

  @Test
  void differentPositionsGiveDifferentIds() {
    assertNotEquals(
        LootId.of(LEVEL_ONE, new GridPoint2(4, 9)), LootId.of(LEVEL_ONE, new GridPoint2(9, 4)));
  }

  @Test
  void sameSpotInDifferentRoomsGivesDifferentIds() {
    GridPoint2 spot = new GridPoint2(4, 9);

    assertNotEquals(LootId.of(LEVEL_ONE, spot), LootId.of("Level 2 - Climb Mount Olympus", spot));
  }

  @Test
  void cannotBeInstantiated() throws NoSuchMethodException {
    Constructor<LootId> constructor = LootId.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
