package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Runs the locked, automatic lift sequence between the dungeon and Nether sub-levels. */
public class SubLevelTravelComponent extends Component {
  // Central lift endpoints in level1-greek.json.
  private static final Vector2 DUNGEON_DOOR = new Vector2(13.25f, 11.9f);
  // The lift shaft remains the return interaction point.
  private static final Vector2 NETHER_DOOR = new Vector2(13.25f, 17.4f);
  private static final float TILE_SIZE = 0.5f;
  // The platform one tile right of the shaft supports the player after the ascent.
  private static final Vector2 NETHER_LANDING = new Vector2(13.75f, 17.4f);
  private static final float ASCENT_FRACTION = 0.8f;
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
      startTravel(NETHER_LANDING);
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
    Vector2 position =
        destination == NETHER_LANDING
            ? netherAscentPosition(start, progress)
            : dungeonDescentPosition(start, progress);
    entity.setPosition(
        position.x - entity.getScale().x / 2f, position.y - entity.getScale().y / 2f);
    physics.getBody().setLinearVelocity(0f, 0f);
    if (progress >= 1f) {
      physics.getBody().setGravityScale(1f);
      travelling = false;
      entity
          .getEvents()
          .trigger("subLevelEntered", destination == NETHER_LANDING ? "NETHER" : "DUNGEON");
    }
  }

  /** Rise through the shaft before moving sideways onto the Nether landing platform. */
  static Vector2 netherAscentPosition(Vector2 start, float progress) {
    if (progress <= ASCENT_FRACTION) {
      return start.cpy().lerp(NETHER_DOOR, progress / ASCENT_FRACTION);
    }
    return NETHER_DOOR
        .cpy()
        .lerp(NETHER_LANDING, (progress - ASCENT_FRACTION) / (1f - ASCENT_FRACTION));
  }

  /** Return to the shaft before descending, avoiding the platform beside the Nether lift. */
  static Vector2 dungeonDescentPosition(Vector2 start, float progress) {
    float sideStepFraction = 1f - ASCENT_FRACTION;
    Vector2 shaftEntry = new Vector2(NETHER_DOOR.x, start.y);
    if (progress <= sideStepFraction) {
      return start.cpy().lerp(shaftEntry, progress / sideStepFraction);
    }
    return shaftEntry.lerp(DUNGEON_DOOR, (progress - sideStepFraction) / ASCENT_FRACTION);
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
