package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.MapLayerData;
import com.csse3200.game.areas.terrain.map.MapSpawns;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
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
  @Mock GameTime timeSource;

  @AfterEach
  void clearServices() {
    ServiceLocator.clear();
  }

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

  @Test
  void eAutomaticallyClimbsAnOrdinaryLadderAndLandsOnSupportedFloor() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(input);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 1, 2);
    ladder.create();
    AtomicInteger walkEvents = new AtomicInteger();
    player.getEvents().addListener("walk", direction -> walkEvents.incrementAndGet());

    assertTrue(ladder.canAutoClimb());
    assertTrue(input.keyDown(Keys.E));
    assertTrue(ladder.isAutoClimbing());
    verify(body).setGravityScale(0f);
    assertTrue(input.keyDown(Keys.A));
    assertTrue(input.keyUp(Keys.A));
    assertEquals(0, walkEvents.get());
    clearInvocations(body);

    ladder.advanceAutoClimb(10f);

    assertFalse(ladder.isAutoClimbing());
    assertEquals(0.75f, player.getCenterPosition().x, 0.001f);
    assertEquals(2.895f, player.getCenterPosition().y, 0.001f);
    verify(body).setGravityScale(1f);
    assertFalse(ladder.canAutoClimb());
  }

  @Test
  void releasingControlDuringAutoClimbStopsCrouching() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(input);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 1, 2);
    ladder.create();
    AtomicBoolean crouching = new AtomicBoolean();
    player.getEvents().addListener("ctrlChanged", crouching::set);

    assertTrue(input.keyDown(Keys.CONTROL_LEFT));
    assertTrue(crouching.get());
    assertTrue(input.keyDown(Keys.E));
    assertTrue(ladder.isAutoClimbing());

    assertTrue(input.keyUp(Keys.CONTROL_LEFT));

    assertFalse(crouching.get());
  }

  @Test
  void autoClimbUsesScaledGameTime() {
    when(engine.createBody(any())).thenReturn(body);
    when(timeSource.getDeltaTime()).thenReturn(0f, 10f);
    ServiceLocator.registerTimeSource(timeSource);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(input);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 1, 2);
    ladder.create();
    Vector2 startingPosition = player.getPosition().cpy();

    assertTrue(input.keyDown(Keys.E));
    ladder.update();

    assertEquals(startingPosition, player.getPosition());
    assertTrue(ladder.isAutoClimbing());

    ladder.update();

    assertFalse(ladder.isAutoClimbing());
    assertEquals(0.75f, player.getCenterPosition().x, 0.001f);
    assertEquals(2.895f, player.getCenterPosition().y, 0.001f);
  }

  @Test
  void eDoesNotAutoClimbTheDungeonToNetherLift() {
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, 5, 6);

    assertFalse(ladder.canAutoClimb());
  }

  @Test
  void eAutoClimbIsLimitedToLevelOne() {
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);

    assertFalse(ladder.canAutoClimb());
  }

  @Test
  void mapUpdateStopsUsingLaddersFromThePreviousRoom() {
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, 1, 2);

    assertTrue(ladder.canAutoClimb());

    ladder.setMapData(createMap());

    assertFalse(ladder.canAutoClimb());
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

  private static LevelMapData createLevelOneMap() {
    MapLayerData collision = new MapLayerData("collision", 8, 16);
    for (int row = 2; row <= 5; row++) {
      collision.set(2, row, new TileDefinition(TileType.LADDER, null));
    }
    collision.set(1, 4, new TileDefinition(TileType.FLOOR, null));
    for (int row = 6; row <= 9; row++) {
      collision.set(5, row, new TileDefinition(TileType.LADDER, null));
    }
    return LevelMapData.builder("level-one-test")
        .tileSize(TILE_SIZE)
        .size(8, 16)
        .layers(List.of(collision))
        .subLevels(
            List.of(
                new SubLevel(
                    "dungeon",
                    "DUNGEON",
                    new SubLevel.Bounds(0, 0, 8, 8),
                    new GridPoint2(5, 6),
                    "nether",
                    new GridPoint2(5, 9)),
                new SubLevel(
                    "nether",
                    "NETHER",
                    new SubLevel.Bounds(0, 8, 8, 8),
                    new GridPoint2(5, 9),
                    "dungeon",
                    new GridPoint2(5, 6))))
        .build();
  }

  private static void positionCentreAtTile(Entity entity, int tileX, int tileY) {
    float centreX = (tileX + 0.5f) * TILE_SIZE;
    float centreY = (tileY + 0.5f) * TILE_SIZE;
    entity.setPosition(centreX - entity.getScale().x / 2f, centreY - entity.getScale().y / 2f);
  }
}
