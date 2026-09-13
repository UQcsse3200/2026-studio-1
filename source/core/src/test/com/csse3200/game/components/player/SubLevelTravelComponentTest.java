package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class SubLevelTravelComponentTest {
  private static final float DUNGEON_DOOR_X = 13.25f;
  private static final float DUNGEON_DOOR_Y = 11.9f;

  @Test
  void enablesNetherTravelPromptAtDungeonDoor() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = new Entity().addComponent(travel);
    positionCentre(player, DUNGEON_DOOR_X, DUNGEON_DOOR_Y);

    assertTrue(travel.canTravelToNether());
  }

  @Test
  void disablesNetherTravelPromptAwayFromDungeonDoor() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = new Entity().addComponent(travel);
    positionCentre(player, DUNGEON_DOOR_X + 1f, DUNGEON_DOOR_Y);

    assertFalse(travel.canTravelToNether());
  }

  private static void positionCentre(Entity player, float x, float y) {
    player.setPosition(x - player.getScale().x / 2f, y - player.getScale().y / 2f);
  }
}
