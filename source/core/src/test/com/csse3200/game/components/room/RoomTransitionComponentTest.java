package com.csse3200.game.components.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoomTransitionComponentTest {
  @Test
  void requestsTransitionOnceWhenPlayerEnters() {
    Entity player = new Entity();
    RoomTransition transition =
        new RoomTransition(
            "door", new GridPoint2(1, 2), 1, 2, null, "maps/room2.json", new GridPoint2(3, 4));
    AtomicInteger requestCount = new AtomicInteger();
    RoomTransitionComponent component =
        new RoomTransitionComponent(transition, player, ignored -> requestCount.incrementAndGet());
    Entity doorway = new Entity().addComponent(component);
    component.create();

    Fixture doorwayFixture = mock(Fixture.class);
    Fixture playerFixture = fixtureFor(player);
    doorway.getEvents().trigger("collisionStart", doorwayFixture, playerFixture);
    doorway.getEvents().trigger("collisionStart", doorwayFixture, playerFixture);

    assertEquals(1, requestCount.get());
  }

  @Test
  void ignoresOtherEntities() {
    Entity player = new Entity();
    AtomicInteger requestCount = new AtomicInteger();
    RoomTransition transition =
        new RoomTransition("door", new GridPoint2(), 1, 1, null, "maps/room2.json", null);
    RoomTransitionComponent component =
        new RoomTransitionComponent(transition, player, ignored -> requestCount.incrementAndGet());
    Entity doorway = new Entity().addComponent(component);
    component.create();

    doorway.getEvents().trigger("collisionStart", mock(Fixture.class), fixtureFor(new Entity()));

    assertEquals(0, requestCount.get());
  }

  private static Fixture fixtureFor(Entity entity) {
    Fixture fixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData userData = new BodyUserData();
    userData.entity = entity;
    when(fixture.getBody()).thenReturn(body);
    when(body.getUserData()).thenReturn(userData);
    return fixture;
  }
}
