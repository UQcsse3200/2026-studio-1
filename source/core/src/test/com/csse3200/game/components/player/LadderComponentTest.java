package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapSpawns;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.components.PhysicsComponent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LadderComponentTest {
  private static final float TILE_SIZE = 0.5f;
  private static final int LADDER_X = 2;
  private static final int LADDER_Y = 2;

  @Mock PhysicsEngine engine;
  @Mock Body body;

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

  @Test
  void beginsDescendingWhenLadderIsImmediatelyBelowPlayer() {
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, LADDER_X, LADDER_Y + 2);

    assertTrue(ladder.beginClimb(-1f));
  }

  @Test
  void restoresGravityAfterMovingPastBottomOfLadder() {
    when(engine.createBody(any())).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(new PhysicsComponent(engine)).addComponent(ladder);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);
    player.create();

    assertTrue(ladder.beginClimb(-1f));
    ladder.update();
    verify(body).setGravityScale(0f);

    positionCentreAtTile(player, LADDER_X, LADDER_Y - 2);
    ladder.update();
    verify(body).setGravityScale(1f);
  }

  @Test
  void horizontalMovementStopsClimbingAndRestoresGravity() {
    when(engine.createBody(any())).thenReturn(body);
    when(body.getLinearVelocity()).thenReturn(new Vector2());
    LadderComponent ladder = new LadderComponent(createMap());
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(input);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);
    ladder.create();

    assertTrue(input.keyDown(Keys.S));
    ladder.update();
    verify(body).setGravityScale(0f);

    assertTrue(input.keyDown(Keys.A));
    verify(body).setGravityScale(1f);
  }

  @Test
  void releasingDownAfterClimbingDoesNotCreateUpwardWalking() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createMap());
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(input);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);
    ladder.create();
    AtomicInteger walkEvents = new AtomicInteger();
    player.getEvents().addListener("walk", direction -> walkEvents.incrementAndGet());

    assertTrue(input.keyDown(Keys.S));
    assertTrue(input.keyUp(Keys.S));

    assertEquals(0, walkEvents.get());
    verify(body).setGravityScale(1f);
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
