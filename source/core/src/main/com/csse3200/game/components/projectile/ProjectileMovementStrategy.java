package com.csse3200.game.components.projectile;

import com.csse3200.game.entities.Entity;

/**
 * Defines how a projectile entity moves, decoupled from {@link ProjectileComponent} so the movement
 * type of a projectile (straight-line, a slower "follow" shot, a lobbed/bow-arc shot, ...) can be
 * swapped by passing a different implementation, without changing {@link ProjectileComponent} or
 * anything that spawns projectiles.
 */
public interface ProjectileMovementStrategy {

  /**
   * Called once, when the projectile entity is created, to set up its initial movement (e.g. give
   * its physics body a starting velocity).
   *
   * @param projectile the projectile entity being moved
   */
  void start(Entity projectile);

  /**
   * Called every frame to advance/update the projectile's movement. For a strategy whose motion is
   * fully determined at spawn time (e.g. a constant-velocity straight shot) this can be a no-op; it
   * exists for strategies that need to change direction/velocity over time (e.g. re-aiming at a
   * moving target, or curving a lobbed shot).
   *
   * @param projectile the projectile entity being moved
   * @param delta time elapsed since the last frame, in seconds
   */
  void update(Entity projectile, float delta);
}
