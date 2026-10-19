package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.components.gamearea.SubLevelEvents;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.components.PhysicsComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubLevelTravelComponentTest {
  private static final float NETHER_DOOR_X = 13.25f;
  private static final float NETHER_DOOR_Y = 17.4f;

  private Graphics realGraphics;
  private Graphics graphics;
  private Body body;

  @BeforeEach
  void setUp() {
    realGraphics = Gdx.graphics;
    graphics = mock(Graphics.class);
    Gdx.graphics = graphics;
    body = mock(Body.class);
  }

  @AfterEach
  void tearDown() {
    Gdx.graphics = realGraphics;
  }

  @Test
  void liftCarriesThePlayerUpToTheNetherAndHandsBackControl() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent(levelOneMap());
    LadderComponent ladder = new LadderComponent(levelOneMap());
    Entity player = playerWith(travel, ladder);
    positionCentre(player, DUNGEON_DOOR_X, DUNGEON_DOOR_Y);
    List<String> entered = new ArrayList<>();
    player.getEvents().addListener(SubLevelEvents.SUB_LEVEL_ENTERED, recordInto(entered));

    assertTrue(travel.beginTravel());
    assertTrue(travel.isControlLocked());
    assertTrue(travel.beginTravel(), "a second request while moving is accepted");
    assertFalse(travel.canTravelToNether());
    verify(body).setGravityScale(0f);

    when(graphics.getDeltaTime()).thenReturn(0.7f);
    travel.update();
    assertTrue(travel.isControlLocked());
    assertTrue(entered.isEmpty());
    clearInvocations(body);

    travel.update();
    assertFalse(travel.isControlLocked());
    assertEquals(List.of("NETHER"), entered);
    assertEquals(13.75f, player.getCenterPosition().x, 0.001f);
    assertEquals(NETHER_DOOR_Y, player.getCenterPosition().y, 0.001f);
    verify(body).setGravityScale(1f);
  }

  @Test
  void liftCarriesThePlayerBackDownToTheDungeon() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = playerWith(travel);
    positionCentre(player, NETHER_DOOR_X, NETHER_DOOR_Y);
    List<String> entered = new ArrayList<>();
    player.getEvents().addListener(SubLevelEvents.SUB_LEVEL_ENTERED, recordInto(entered));

    assertFalse(travel.canTravelToNether());
    assertTrue(travel.beginTravel());

    when(graphics.getDeltaTime()).thenReturn(5f);
    travel.update();

    assertEquals(List.of("DUNGEON"), entered);
    assertEquals(DUNGEON_DOOR_X, player.getCenterPosition().x, 0.001f);
    assertEquals(DUNGEON_DOOR_Y, player.getCenterPosition().y, 0.001f);
  }

  @Test
  void liftDoesNothingAwayFromEitherDoor() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = playerWith(travel);
    positionCentre(player, 0f, 0f);

    assertFalse(travel.beginTravel());
    travel.update();
    assertFalse(travel.isControlLocked());
  }

  @Test
  void leavingLevelOneMidTripStopsTheLift() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = playerWith(travel);
    positionCentre(player, DUNGEON_DOOR_X, DUNGEON_DOOR_Y);
    assertTrue(travel.beginTravel());
    clearInvocations(body);

    travel.setMapData(null);

    assertFalse(travel.isControlLocked());
    verify(body).setGravityScale(1f);
    verify(body).setLinearVelocity(0f, 0f);
  }

  @Test
  void mapWithOnlyOneOfTheSubLevelsIsNotLevelOne() {
    SubLevelTravelComponent travel =
        new SubLevelTravelComponent(
            LevelMapData.builder("dungeon-only")
                .size(1, 1)
                .subLevels(List.of(subLevel("dungeon", 0)))
                .build());
    Entity player = new Entity().addComponent(travel);
    positionCentre(player, DUNGEON_DOOR_X, DUNGEON_DOOR_Y);

    assertFalse(travel.canTravelToNether());
  }

  /** Collects each sub-level title announced, in order. */
  private static EventListener1<String> recordInto(List<String> titles) {
    return titles::add;
  }

  private Entity playerWith(SubLevelTravelComponent travel) {
    return playerWith(travel, null);
  }

  private Entity playerWith(SubLevelTravelComponent travel, LadderComponent ladder) {
    PhysicsEngine engine = mock(PhysicsEngine.class);
    when(engine.createBody(any())).thenReturn(body);
    Entity player = new Entity().addComponent(new PhysicsComponent(engine)).addComponent(travel);
    if (ladder != null) {
      player.addComponent(ladder);
      ladder.create();
    }
    travel.create();
    return player;
  }

  private static LevelMapData levelOneMap() {
    return LevelMapData.builder("level-one")
        .size(1, 1)
        .subLevels(List.of(subLevel("dungeon", 0), subLevel("nether", 40)))
        .build();
  }

  private static SubLevel subLevel(String id, int bottom) {
    return new SubLevel(id, null, new SubLevel.Bounds(0, bottom, 60, 40), null, null, null);
  }

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

  @Test
  void disablesLevelOneTravelAfterEnteringAnotherRoom() {
    SubLevelTravelComponent travel = new SubLevelTravelComponent();
    Entity player = new Entity().addComponent(travel);
    positionCentre(player, DUNGEON_DOOR_X, DUNGEON_DOOR_Y);

    assertTrue(travel.canTravelToNether());

    LevelMapData levelTwo = LevelMapData.builder("level-two-test").size(1, 1).build();
    travel.setMapData(levelTwo);

    assertFalse(travel.canTravelToNether());
    assertFalse(travel.beginTravel());
  }

  @Test
  void ascentEndsOneTileRightOfNetherLiftOnThePlatform() {
    Vector2 start = new Vector2(DUNGEON_DOOR_X, DUNGEON_DOOR_Y);

    Vector2 shaftExit = SubLevelTravelComponent.netherAscentPosition(start, 0.8f);
    Vector2 landing = SubLevelTravelComponent.netherAscentPosition(start, 1f);

    assertEquals(13.25f, shaftExit.x, 0.0001f);
    assertEquals(17.4f, shaftExit.y, 0.0001f);
    assertEquals(shaftExit.x + 0.5f, landing.x, 0.0001f);
    assertEquals(shaftExit.y, landing.y, 0.0001f);
  }

  @Test
  void returnTravelMovesBackIntoLiftShaftBeforeDescending() {
    Vector2 landing = new Vector2(13.75f, 17.4f);

    Vector2 shaftEntry = SubLevelTravelComponent.dungeonDescentPosition(landing, 0.2f);
    Vector2 dungeonArrival = SubLevelTravelComponent.dungeonDescentPosition(landing, 1f);

    assertEquals(13.25f, shaftEntry.x, 0.0001f);
    assertEquals(17.4f, shaftEntry.y, 0.0001f);
    assertEquals(DUNGEON_DOOR_X, dungeonArrival.x, 0.0001f);
    assertEquals(DUNGEON_DOOR_Y, dungeonArrival.y, 0.0001f);
  }

  private static void positionCentre(Entity player, float x, float y) {
    player.setPosition(x - player.getScale().x / 2f, y - player.getScale().y / 2f);
  }
}
