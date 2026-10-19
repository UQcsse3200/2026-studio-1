package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
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

  @Test
  void autoClimbAlignsRisesThenStepsOntoTheLanding() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(new PhysicsComponent(engine)).addComponent(ladder);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 1, 2);
    ladder.create();
    AtomicInteger idleEvents = new AtomicInteger();
    AtomicInteger climbEvents = new AtomicInteger();
    player.getEvents().addListener("idle", direction -> idleEvents.incrementAndGet());
    player.getEvents().addListener("climb", climbEvents::incrementAndGet);

    assertTrue(ladder.beginAutoClimb());
    for (int i = 0; i < 200 && ladder.isAutoClimbing(); i++) {
      ladder.advanceAutoClimb(0.02f);
    }

    assertFalse(ladder.isAutoClimbing());
    assertEquals(1, climbEvents.get(), "the climb animation starts once, on the rise");
    assertEquals(2, idleEvents.get(), "idle before the rise and again on the landing");
    assertEquals(0.75f, player.getCenterPosition().x, 0.001f);
    assertEquals(2.895f, player.getCenterPosition().y, 0.001f);
  }

  @Test
  void manualClimbingIsIgnoredDuringAnAutoClimb() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(new PhysicsComponent(engine)).addComponent(ladder);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 2, 3);
    ladder.create();

    assertTrue(ladder.beginAutoClimb());
    clearInvocations(body);

    assertFalse(ladder.beginClimb(1f));
    ladder.stopClimbing();
    verify(body, never()).setGravityScale(1f);
    assertFalse(ladder.canAutoClimb());
    assertTrue(ladder.isAutoClimbing());
  }

  @Test
  void beginAutoClimbFailsWithNoLadderToClimb() {
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(ladder);
    player.setScale(0.75f, 0.75f);
    positionCentreAtTile(player, 7, 1);

    assertFalse(ladder.beginAutoClimb());
    assertFalse(ladder.isAutoClimbing());
  }

  @Test
  void advancingWithoutAnAutoClimbDoesNothing() {
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, 1, 2);
    Vector2 before = player.getPosition().cpy();

    ladder.advanceAutoClimb(1f);

    assertEquals(before, player.getPosition());
  }

  @Test
  void updateDoesNothingWhenNotClimbing() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(new PhysicsComponent(engine)).addComponent(ladder);
    ladder.create();

    ladder.update();

    verify(body, never()).setGravityScale(anyFloat());
  }

  @Test
  void zeroDirectionDoesNotStartClimbing() {
    LadderComponent ladder = new LadderComponent(createMap());
    Entity player = new Entity().addComponent(ladder);
    positionCentreAtTile(player, LADDER_X, LADDER_Y);

    assertFalse(ladder.beginClimb(0f));
  }

  @Test
  void changingMapRestoresGravityOnceCreated() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createMap());
    new Entity().addComponent(new PhysicsComponent(engine)).addComponent(ladder);
    ladder.create();

    ladder.setMapData(createLevelOneMap());

    verify(body).setGravityScale(1f);
    verify(body).setLinearVelocity(0f, 0f);
  }

  @Test
  void autoClimbIsBlockedWhileTheLiftHasControl() {
    when(engine.createBody(any())).thenReturn(body);
    LadderComponent ladder = new LadderComponent(createLevelOneMap());
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent(engine))
            .addComponent(ladder)
            .addComponent(travel);
    ladder.create();
    travel.create();
    // Stand at the real level's dungeon lift door, so the lift can take over.
    player.setPosition(13.25f - player.getScale().x / 2f, 11.9f - player.getScale().y / 2f);

    assertTrue(travel.beginTravel());

    assertFalse(ladder.canAutoClimb());
  }

  @Test
  void autoClimbSkipsLaddersWithNoWayOff() {
    LadderComponent ladder = new LadderComponent(createAwkwardLaddersMap());
    Entity player = new Entity().addComponent(ladder);
    player.setScale(0.75f, 0.75f);

    // Ladder at x = 8 has no floor beside its top.
    positionCentreAtTile(player, 8, 3);
    assertFalse(ladder.canAutoClimb());

    // Ladder at x = 5 runs from the dungeon into the Nether.
    positionCentreAtTile(player, 5, 7);
    assertFalse(ladder.canAutoClimb());

    // Already above the top of the climbable ladder at x = 2.
    positionCentreAtTile(player, 2, 6);
    assertFalse(ladder.canAutoClimb());

    // Too far below the bottom of it.
    positionCentreAtTile(player, 2, 0);
    assertFalse(ladder.canAutoClimb());

    // Standing on it, it can be climbed.
    positionCentreAtTile(player, 2, 3);
    assertTrue(ladder.canAutoClimb());
  }

  /**
   * A level-one map with one good ladder at x = 2 (landing on the floor at (1, 4)), one at x = 8
   * with nothing to step onto, and one at x = 5 crossing from the dungeon into the Nether. The lift
   * door is out of the way at (11, 6).
   */
  private static LevelMapData createAwkwardLaddersMap() {
    MapLayerData collision = new MapLayerData("collision", 12, 16);
    for (int row = 2; row <= 5; row++) {
      collision.set(2, row, new TileDefinition(TileType.LADDER, null));
      collision.set(8, row, new TileDefinition(TileType.LADDER, null));
    }
    collision.set(1, 4, new TileDefinition(TileType.FLOOR, null));
    for (int row = 6; row <= 9; row++) {
      collision.set(5, row, new TileDefinition(TileType.LADDER, null));
    }
    collision.set(4, 8, new TileDefinition(TileType.FLOOR, null));
    collision.set(6, 8, new TileDefinition(TileType.FLOOR, null));
    return LevelMapData.builder("awkward-ladders")
        .tileSize(TILE_SIZE)
        .size(12, 16)
        .layers(List.of(collision))
        .subLevels(
            List.of(
                new SubLevel(
                    "dungeon",
                    "DUNGEON",
                    new SubLevel.Bounds(0, 0, 12, 8),
                    new GridPoint2(11, 6),
                    "nether",
                    new GridPoint2(11, 9)),
                new SubLevel(
                    "nether",
                    "NETHER",
                    new SubLevel.Bounds(0, 8, 12, 8),
                    new GridPoint2(11, 9),
                    "dungeon",
                    new GridPoint2(11, 6))))
        .build();
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
