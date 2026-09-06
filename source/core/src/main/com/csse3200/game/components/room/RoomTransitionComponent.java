package com.csse3200.game.components.room;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.areas.terrain.map.RoomTransition;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener2;
import com.csse3200.game.physics.BodyUserData;
import java.util.function.Consumer;

/** Detects the player entering a doorway and requests the doorway's room transition once. */
public class RoomTransitionComponent extends Component {
  private final RoomTransition transition;
  private final Entity player;
  private final Consumer<RoomTransition> transitionHandler;
  private boolean triggered;

  public RoomTransitionComponent(
      RoomTransition transition, Entity player, Consumer<RoomTransition> transitionHandler) {
    this.transition = transition;
    this.player = player;
    this.transitionHandler = transitionHandler;
  }

  @Override
  public void create() {
    entity
        .getEvents()
        .addListener(
            "collisionStart",
            (EventListener2<Fixture, Fixture>)
                (doorFixture, otherFixture) -> handleCollision(otherFixture));
  }

  private void handleCollision(Fixture otherFixture) {
    if (triggered || otherFixture == null) {
      return;
    }

    Object userData = otherFixture.getBody().getUserData();
    if (!(userData instanceof BodyUserData bodyUserData) || bodyUserData.entity != player) {
      return;
    }

    triggered = true;
    transitionHandler.accept(transition);
  }
}
