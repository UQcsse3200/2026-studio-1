package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Enables vertical movement while the player overlaps a ladder tile. */
public class LadderComponent extends Component {
  private static final float CLIMB_SPEED = 2.5f;

  private final LevelMapData mapData;
  private PhysicsComponent physics;
  private float direction;
  private boolean climbing;

  public LadderComponent(LevelMapData mapData) {
    this.mapData = mapData;
  }

  @Override
  public void create() {
    physics = entity.getComponent(PhysicsComponent.class);
  }

  /** Starts moving up ({@code 1}) or down ({@code -1}) when the player is at a ladder. */
  public boolean beginClimb(float newDirection) {
    float requestedDirection = Math.signum(newDirection);
    if (requestedDirection == 0f || !isAtLadder(requestedDirection)) {
      return false;
    }
    direction = requestedDirection;
    climbing = true;
    return true;
  }

  /** Releases the ladder and restores ordinary gravity. */
  public void stopClimbing() {
    direction = 0f;
    climbing = false;
    physics.getBody().setGravityScale(1f);
  }

  @Override
  public void update() {
    if (!climbing) {
      return;
    }
    if (!isAtLadder(direction)) {
      stopClimbing();
      return;
    }

    physics.getBody().setGravityScale(0f);
    float xVelocity = physics.getBody().getLinearVelocity().x;
    physics.getBody().setLinearVelocity(xVelocity, direction * CLIMB_SPEED);
  }

  private boolean isAtLadder(float climbDirection) {
    Vector2 centre = entity.getCenterPosition();
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float tileSize = mapData.getTileSize();
    int x = (int) Math.floor(centre.x / tileSize);
    int bottomRow = (int) Math.floor(position.y / tileSize);
    int topRow = (int) Math.floor((position.y + scale.y - 0.001f) / tileSize);

    // Include one tile in the requested climb direction so the player can enter a ladder from a
    // landing. Do not check behind the player: doing so keeps climbing active past the end of a
    // ladder and leaves gravity disabled while the player moves away.
    int firstRow = climbDirection < 0f ? bottomRow - 1 : bottomRow;
    int lastRow = climbDirection > 0f ? topRow + 1 : topRow;
    for (int row = firstRow; row <= lastRow; row++) {
      if (isLadder(x, row)) {
        return true;
      }
    }
    return false;
  }

  private boolean isLadder(int x, int y) {
    return mapData.getTileType(x, y) == TileType.LADDER;
  }
}
