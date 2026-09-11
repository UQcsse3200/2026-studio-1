package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Runs the locked, automatic lift sequence between the dungeon and Nether sub-levels. */
public class SubLevelTravelComponent extends Component {
  // Central lift endpoints in level1-greek.json.
  private static final Vector2 DUNGEON_DOOR = new Vector2(13.25f, 11.9f);
  // Arrive inside the hollow checkpoint cell, with feet just above the solid landing below.
  private static final Vector2 NETHER_DOOR = new Vector2(13.25f, 17.4f);
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
    if (canTravelToNether()) {
      startTravel(NETHER_DOOR);
      return true;
    }
    if (canTravelToDungeon()) {
      startTravel(DUNGEON_DOOR);
      return true;
    }
    return false;
  }

  /**
   * Whether the player is close enough to the dungeon lift to travel to the Nether.
   *
   * @return true when the Nether interaction prompt and {@code E} action should be available
   */
  public boolean canTravelToNether() {
    return !travelling && isNear(DUNGEON_DOOR);
  }

  private boolean canTravelToDungeon() {
    return !travelling && isNear(NETHER_DOOR);
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

  private boolean isNear(Vector2 door) {
    return entity.getCenterPosition().dst(door) < TILE_SIZE * 2f;
  }
}
