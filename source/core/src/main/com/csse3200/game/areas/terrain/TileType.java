package com.csse3200.game.areas.terrain;

public enum TileType {
    FLOOR(CollisionType.SOLID),
    WALL(CollisionType.SOLID),
    PLATFORM(CollisionType.PLATFORM),
    HAZARD(CollisionType.HAZARD),
    DECORATIVE(CollisionType.NONE);

    private final CollisionType collisionType;

    TileType(CollisionType collisionType) {
        this.collisionType = collisionType;
    }

    public CollisionType getCollisionType() {
        return collisionType;
    }
}