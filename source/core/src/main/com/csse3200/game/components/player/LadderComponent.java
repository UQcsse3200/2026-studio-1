package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.areas.terrain.map.SubLevel;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Enables vertical movement while the player overlaps a ladder tile. */
public class LadderComponent extends Component {
  private static final float CLIMB_SPEED = 2.5f;

  private LevelMapData mapData;
  private PhysicsComponent physics;
  private float direction;
  private boolean climbing;
  private boolean autoClimbing;
  private float autoElapsed;
  private float alignDuration;
  private float riseDuration;
  private float landingDuration;
  private Vector2 autoStart;
  private Vector2 shaftEntry;
  private Vector2 shaftExit;
  private Vector2 landing;

  public LadderComponent(LevelMapData mapData) {
    this.mapData = mapData;
  }

  /** Updates the tile map used for ladder detection after the player enters another room. */
  public void setMapData(LevelMapData mapData) {
    this.mapData = mapData;
    direction = 0f;
    climbing = false;
    autoClimbing = false;
    autoElapsed = 0f;
    autoStart = null;
    shaftEntry = null;
    shaftExit = null;
    landing = null;
    if (physics != null) {
      physics.getBody().setGravityScale(1f);
      physics.getBody().setLinearVelocity(0f, 0f);
    }
  }

  @Override
  public void create() {
    physics = entity.getComponent(PhysicsComponent.class);
  }

  /** Starts moving up ({@code 1}) or down ({@code -1}) when the player is at a ladder. */
  public boolean beginClimb(float newDirection) {
    if (autoClimbing) {
      return false;
    }
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
    if (autoClimbing) {
      return;
    }
    direction = 0f;
    climbing = false;
    physics.getBody().setGravityScale(1f);
  }

  /** Whether E can take the player up a regular Level 1 ladder. The sub-level lift is excluded. */
  public boolean canAutoClimb() {
    SubLevelTravelComponent travel = entity.getComponent(SubLevelTravelComponent.class);
    return !autoClimbing
        && (travel == null || !travel.isControlLocked())
        && findAutoClimbTarget() != null;
  }

  /** Starts a hands-free climb to the nearest supported landing above an ordinary ladder. */
  public boolean beginAutoClimb() {
    if (!canAutoClimb()) {
      return false;
    }
    AutoClimbTarget target = findAutoClimbTarget();
    stopClimbing();
    autoStart = entity.getCenterPosition();
    float tileSize = mapData.getTileSize();
    float ladderCentreX = (target.ladderX() + 0.5f) * tileSize;
    shaftEntry = new Vector2(ladderCentreX, autoStart.y);
    shaftExit = new Vector2(ladderCentreX, target.landingY());
    landing = new Vector2((target.landingX() + 0.5f) * tileSize, target.landingY());
    alignDuration = Math.abs(shaftEntry.x - autoStart.x) / CLIMB_SPEED;
    riseDuration = (shaftExit.y - shaftEntry.y) / CLIMB_SPEED;
    landingDuration = Math.abs(landing.x - shaftExit.x) / CLIMB_SPEED;
    autoElapsed = 0f;
    autoClimbing = true;
    physics.getBody().setGravityScale(0f);
    physics.getBody().setLinearVelocity(0f, 0f);
    return true;
  }

  public boolean isAutoClimbing() {
    return autoClimbing;
  }

  @Override
  public void update() {
    if (autoClimbing) {
      advanceAutoClimb(Gdx.graphics.getDeltaTime());
      return;
    }
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

  /** Progresses the automatic climb; kept separate so the route can be tested without a frame. */
  void advanceAutoClimb(float delta) {
    if (!autoClimbing) {
      return;
    }
    autoElapsed += Math.max(0f, delta);
    float ascentEnd = alignDuration + riseDuration;
    float routeEnd = ascentEnd + landingDuration;
    Vector2 next;
    if (autoElapsed < alignDuration) {
      next = autoStart.cpy().lerp(shaftEntry, autoElapsed / alignDuration);
    } else if (autoElapsed < ascentEnd) {
      next = shaftEntry.cpy().lerp(shaftExit, (autoElapsed - alignDuration) / riseDuration);
    } else if (autoElapsed < routeEnd) {
      next = shaftExit.cpy().lerp(landing, (autoElapsed - ascentEnd) / landingDuration);
    } else {
      next = landing;
    }

    Vector2 scale = entity.getScale();
    entity.setPosition(next.x - scale.x / 2f, next.y - scale.y / 2f);
    physics.getBody().setLinearVelocity(0f, 0f);
    if (autoElapsed >= routeEnd) {
      autoClimbing = false;
      physics.getBody().setGravityScale(1f);
    }
  }

  private AutoClimbTarget findAutoClimbTarget() {
    if (!isLevelOne()) {
      return null;
    }
    Vector2 centre = entity.getCenterPosition();
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float tileSize = mapData.getTileSize();
    int centreColumn = (int) Math.floor(centre.x / tileSize);
    int firstRow = (int) Math.floor(position.y / tileSize) - 1;
    int lastRow = (int) Math.floor((position.y + scale.y) / tileSize) + 1;

    for (int x = centreColumn - 1; x <= centreColumn + 1; x++) {
      if (isLiftColumn(x) || Math.abs(centre.x - (x + 0.5f) * tileSize) > tileSize * 1.5f) {
        continue;
      }
      for (int row = firstRow; row <= lastRow; row++) {
        if (!isLadder(x, row)) {
          continue;
        }
        int bottom = row;
        while (isLadder(x, bottom - 1)) {
          bottom--;
        }
        int top = row;
        while (isLadder(x, top + 1)) {
          top++;
        }
        if (mapData.getSubLevelAt(bottom) != mapData.getSubLevelAt(top)) {
          continue;
        }
        float landingY = top * tileSize + scale.y / 2f + 0.02f;
        if (centre.y < (bottom - 1) * tileSize || centre.y >= landingY - 0.1f) {
          continue;
        }
        int landingX = findLandingColumn(x, top);
        if (landingX >= 0) {
          return new AutoClimbTarget(x, landingX, landingY);
        }
      }
    }
    return null;
  }

  private boolean isLevelOne() {
    boolean dungeon = false;
    boolean nether = false;
    for (SubLevel section : mapData.getSubLevels()) {
      dungeon |= "dungeon".equals(section.id());
      nether |= "nether".equals(section.id());
    }
    return dungeon && nether;
  }

  private boolean isLiftColumn(int x) {
    for (SubLevel section : mapData.getSubLevels()) {
      if (section.door() != null && section.door().x == x) {
        return true;
      }
    }
    return false;
  }

  private int findLandingColumn(int ladderX, int top) {
    for (int x : new int[] {ladderX - 1, ladderX + 1}) {
      TileType support = mapData.getTileType(x, top - 1);
      if ((support == TileType.FLOOR || support == TileType.PLATFORM)
          && isOpen(x, top)
          && isOpen(x, top + 1)) {
        return x;
      }
    }
    return -1;
  }

  private boolean isOpen(int x, int y) {
    TileType tile = mapData.getTileType(x, y);
    return tile == null || tile == TileType.DECORATIVE || tile == TileType.LADDER;
  }

  private record AutoClimbTarget(int ladderX, int landingX, float landingY) {}

  private boolean isAtLadder(float climbDirection) {
    Vector2 centre = entity.getCenterPosition();
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float tileSize = mapData.getTileSize();
    int x = (int) Math.floor(centre.x / tileSize);
    int bottomRow = (int) Math.floor(position.y / tileSize);
    int topRow = (int) Math.floor((position.y + scale.y - 0.001f) / tileSize);

    // Check the player's ladder column and one tile ahead to allow entry from a landing.
    // Exclude tiles behind the player so gravity returns after leaving the ladder.
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
