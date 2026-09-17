package com.csse3200.game.areas.terrain;

public enum TileType {
  FLOOR(CollisionType.SOLID),
  WALL(CollisionType.SOLID),
  PLATFORM(CollisionType.PLATFORM),
  ONE_WAY_PLATFORM(CollisionType.ONE_WAY_PLATFORM),
  HAZARD(CollisionType.HAZARD),
  LADDER(CollisionType.NONE),
  DECORATIVE(CollisionType.NONE);

  private final CollisionType collisionType;

  TileType(CollisionType collisionType) {
    this.collisionType = collisionType;
  }

  public CollisionType getCollisionType() {
    return collisionType;
  }
}
