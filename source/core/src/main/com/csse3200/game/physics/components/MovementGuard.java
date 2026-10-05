package com.csse3200.game.physics.components;

import com.badlogic.gdx.math.Vector2;

/**
 * A veto over where an entity may walk. {@link PhysicsMovementComponent} asks its guard each frame,
 * with the direction it is about to walk, and stops the walk for that frame if the guard says it is
 * blocked.
 *
 * <p>It is a functional interface so a test or a one-off rule can pass a lambda. The first real
 * implementation is {@code HazardAvoidanceComponent}.
 *
 * <p><b>Contract:</b> the guard must not change its argument, must be quick (it runs every frame
 * for every guarded entity) and must not move the entity itself.
 */
@FunctionalInterface
public interface MovementGuard {

  /**
   * Says whether walking in the given direction right now is blocked.
   *
   * @param direction the unit direction the entity is about to walk, never zero length; the guard
   *     must treat it as read only
   * @return true to stop the walk for this frame, false to let it go ahead
   */
  boolean blocks(Vector2 direction);
}
