package com.csse3200.game.components.tasks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.services.ServiceLocator;

public class PlatformWanderTask extends WanderTask {
  private final PhysicsEngine physics;
  private final DebugRenderer debugRenderer;
  private final float rayCastPositionScale;
  private final RaycastHit leftFloorHit = new RaycastHit();
  private final RaycastHit rightFloorHit = new RaycastHit();

  /**
   * @param wanderRange Distance in X and Y the entity can move from its position when start() is
   *     called.
   * @param waitTime How long in seconds to wait between wandering.
   * @param rayCastPositionScale From 0f to 0.5f, with 0 being at the very edge of the entity box
   *     and 0.5f being in the very centre.
   */
  public PlatformWanderTask(Vector2 wanderRange, float waitTime, float rayCastPositionScale) {
    super(wanderRange, waitTime);
    physics = ServiceLocator.getPhysicsService().getPhysics();
    debugRenderer = ServiceLocator.getRenderService().getDebug();
    this.rayCastPositionScale = rayCastPositionScale;
  }

  @Override
  public void start() {
    super.start();
    // Allow owner to fall upon being spawned
    swapTask(waitTask);
  }

  /**
   * Raycasts towards the ground to find if owner is grounded.
   *
   * @param position float from 0 to 1 which is the ratio from leftmost to rightmost on the owner
   * @param hit the RaycastHit object to store the raycast results
   * @return true if raycast collides with the ground or another collision on PhysicsLayer.OBSTACLE
   */
  private boolean getGroundRaycast(float position, RaycastHit hit) {
    Vector2 ownerPosition =
        owner.getEntity().getPosition().add(owner.getEntity().getScale().x * position, 0);
    // Makes a raycast of length 0.15f
    Vector2 groundCheckPosition = new Vector2(ownerPosition.x, ownerPosition.y - 0.15f);
    boolean grounded =
        physics.raycast(ownerPosition, groundCheckPosition, PhysicsLayer.OBSTACLE, hit);
    if (grounded) {
      debugRenderer.drawLine(ownerPosition, hit.point);
    } else {
      debugRenderer.drawLine(ownerPosition, groundCheckPosition);
    }
    return grounded;
  }

  @Override
  public void update() {

    boolean leftGrounded = getGroundRaycast(0 + rayCastPositionScale, leftFloorHit);
    boolean rightGrounded = getGroundRaycast(1 - rayCastPositionScale, rightFloorHit);

    if (!leftGrounded && !rightGrounded) {
      updateStartPos();
    } else if (currentTask.getStatus() != Status.ACTIVE) {
      if (currentTask == movementTask) {
        startWaiting();
      } else {
        if (startPos.y > owner.getEntity().getPosition().y) {
          startPos.y = owner.getEntity().getPosition().y;
        }
        startMoving();
      }
    }
    currentTask.update();

    debugRenderer.drawRectangle(startPos, new Vector2(0.05f, 0.05f), Color.MAGENTA, 1f);
  }

  @Override
  protected Vector2 getRandomPosInRange() {
    Vector2 halfRange = wanderRange.cpy().scl(0.5f);
    Vector2 min = startPos.cpy().sub(halfRange);
    Vector2 max = startPos.cpy().add(halfRange);
    float randomXPosInRange = MathUtils.random(min.x, max.x);
    debugRenderer.drawLine(startPos, new Vector2(randomXPosInRange, startPos.y));

    return new Vector2(randomXPosInRange, startPos.y);
  }

  /** Changes startPos to current owner position for owner being on new platform. */
  private void updateStartPos() {
    if (leftFloorHit.point != null) {
      this.startPos = new Vector2(owner.getEntity().getPosition().x, leftFloorHit.point.y);
    } else {
      this.startPos = owner.getEntity().getPosition();
    }
  }
}
