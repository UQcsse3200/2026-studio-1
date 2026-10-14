package com.csse3200.game.components.tasks;

import com.badlogic.gdx.math.Vector2;

/**
 * A {@link PlatformWanderTask} that also stays within a fixed radius of where it first started, so
 * an enemy patrols around its spawn point instead of drifting across the whole platform.
 */
public class BoundedPlatformWanderTask extends PlatformWanderTask {
  private boolean anchored;
  private final float radius;
  private float anchorX;

  /**
   * Creates a wander that never strays further than radius from where it first starts.
   *
   * @param radius how far either side of the spawn point the entity may go; greater than zero and
   *     finite (radius 2 means 4 units of total travel)
   * @param waitTime seconds to wait between wanders, passed to the parent unchanged
   * @param rayCastPositionScale 0 to 0.5, where the ledge raycasts sit, passed to the parent
   *     unchanged
   * @throws IllegalArgumentException if radius is zero, negative, not a number or infinite
   */
  public BoundedPlatformWanderTask(float radius, float waitTime, float rayCastPositionScale) {
    super(checkedWidth(radius), waitTime, rayCastPositionScale);
    this.radius = radius;
    anchored = false;
  }

  /** Captures the spawn anchor the first time the task starts, then starts as the parent does. */
  @Override
  public void start() {
    if (!isAnchored()) {
      anchorX = owner.getEntity().getPosition().x;
      anchored = true;
    }
    super.start();
  }

  /**
   * Picks a target the way the parent does, then limits its horizontal position to the anchor plus
   * or minus the radius. The vertical position is left as the parent chose it.
   *
   * @return a wander target no further than the radius from the anchor
   */
  @Override
  protected Vector2 getRandomPosInRange() {
    Vector2 candidate = super.getRandomPosInRange();
    if (!isAnchored()) {
      return candidate;
    }
    // lowest <- anchorX MINUS radius
    float lowestX = this.getAnchorX() - this.getRadius();
    // highest <- anchorX PLUS radius
    float highestX = this.getAnchorX() + this.getRadius();
    if (candidate.x < lowestX) {
      candidate.x = lowestX;
    } else if (candidate.x > highestX) {
      candidate.x = highestX;
    }
    return candidate;
  }

  /**
   * Gets how far the entity may stray.
   *
   * @return the maximum distance from the anchor, in world units
   */
  public float getRadius() {
    return radius;
  }

  /**
   * Gets the centre of the patrol.
   *
   * @return the horizontal position the radius is measured from (meaningful after the first start)
   */
  public float getAnchorX() {
    return this.anchorX;
  }

  /**
   * Whether the patrol centre has been captured yet.
   *
   * @return true once the task has started at least once and captured its anchor
   */
  public boolean isAnchored() {
    return anchored;
  }

  private static Vector2 checkedWidth(float radius) {
    if (radius <= 0) {
      throw new IllegalArgumentException("Radius must be greater than 0.");
    }
    if (radius == Float.NEGATIVE_INFINITY || radius == Float.POSITIVE_INFINITY) {
      throw new IllegalArgumentException("Radius must be a finite number.");
    }

    if (Float.isNaN(radius)) {
      throw new IllegalArgumentException("Radius must be a valid number.");
    }
    return new Vector2(radius * 2, 0);
  }
}
