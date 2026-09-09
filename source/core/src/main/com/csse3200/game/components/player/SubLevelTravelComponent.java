package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Runs the locked, automatic lift sequence between the dungeon and Nether sub-levels. */
public class SubLevelTravelComponent extends Component {
  private static final Vector2 DUNGEON_DOOR = new Vector2(11.75f, 12.25f);
  private static final Vector2 NETHER_DOOR = new Vector2(11.75f, 19.25f);
  private static final float TILE_SIZE = 0.5f;
  private static final float TRAVEL_DURATION = 1.4f;

  private PhysicsComponent physics;
  private boolean travelling;
  private float elapsed;
  private Vector2 start;
  private Vector2 destination;

  @Override
  public void create() {
    physics = entity.getComponent(PhysicsComponent.class);
  }

  /** Begins travel when the player is standing at either lift door. */
  public boolean beginTravel() {
    if (travelling) {
      return true;
    }
    Vector2 position = entity.getCenterPosition();
    if (position.dst(DUNGEON_DOOR) < TILE_SIZE * 2f) {
      startTravel(NETHER_DOOR);
      return true;
    }
    if (position.dst(NETHER_DOOR) < TILE_SIZE * 2f) {
      startTravel(DUNGEON_DOOR);
      return true;
    }
    return false;
  }

  public boolean isControlLocked() {
    return travelling;
  }

  @Override
  public void update() {
    if (!travelling) {
      return;
    }
    elapsed += com.badlogic.gdx.Gdx.graphics.getDeltaTime();
    float progress = Math.min(1f, elapsed / TRAVEL_DURATION);
    Vector2 position = start.cpy().lerp(destination, progress);
    entity.setPosition(
        position.x - entity.getScale().x / 2f, position.y - entity.getScale().y / 2f);
    physics.getBody().setLinearVelocity(0f, 0f);
    if (progress >= 1f) {
      physics.getBody().setGravityScale(1f);
      travelling = false;
      entity
          .getEvents()
          .trigger("subLevelEntered", destination == NETHER_DOOR ? "NETHER" : "DUNGEON");
    }
  }

  private void startTravel(Vector2 target) {
    start = entity.getCenterPosition();
    destination = target;
    elapsed = 0f;
    travelling = true;
    physics.getBody().setGravityScale(0f);
    physics.getBody().setLinearVelocity(0f, 0f);
  }
}
