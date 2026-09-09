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
    if (!isAtLadder()) {
      return false;
    }
    direction = Math.signum(newDirection);
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
    if (!isAtLadder()) {
      stopClimbing();
      return;
    }

    physics.getBody().setGravityScale(0f);
    float xVelocity = physics.getBody().getLinearVelocity().x;
    physics.getBody().setLinearVelocity(xVelocity, direction * CLIMB_SPEED);
  }

  private boolean isAtLadder() {
    Vector2 centre = entity.getCenterPosition();
    float tileSize = mapData.getTileSize();
    int x = (int) Math.floor(centre.x / tileSize);
    int y = (int) Math.floor(centre.y / tileSize);
    return isLadder(x, y) || isLadder(x, y - 1) || isLadder(x, y + 1);
  }

  private boolean isLadder(int x, int y) {
    return mapData.getTileType(x, y) == TileType.LADDER;
  }
}
