package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapSpawns;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.entities.Entity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LadderComponentTest {
  private static final float TILE_SIZE = 0.5f;
  private static final int LADDER_X = 2;
  private static final int LADDER_Y = 2;

  @Test
  void beginsClimbingWhenPlayerCentreIsInLadderColumn() {
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);

    assertTrue(ladder.beginClimb(1f));
  }

  @Test
  void doesNotBeginClimbingFromAdjacentColumns() {
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(ladder);

    positionCentreAtTile(player, LADDER_X - 1, LADDER_Y);
    assertFalse(ladder.beginClimb(1f));

    positionCentreAtTile(player, LADDER_X + 1, LADDER_Y);
    assertFalse(ladder.beginClimb(1f));
  }

  private static LevelMapData createMap() {
    MapLayerData collision = new MapLayerData("collision", 5, 5);
    collision.set(LADDER_X, LADDER_Y, new TileDefinition(TileType.LADDER, null));
    return new LevelMapData(
        "ladder-test",
        TILE_SIZE,
        collision.getWidth(),
        collision.getHeight(),
        Map.of(),
        List.of(collision),
        new MapSpawns());
  }

  private static void positionCentreAtTile(Entity entity, int tileX, int tileY) {
    float centreX = (tileX + 0.5f) * TILE_SIZE;
    float centreY = (tileY + 0.5f) * TILE_SIZE;
    entity.setPosition(centreX - entity.getScale().x / 2f, centreY - entity.getScale().y / 2f);
  }
}
