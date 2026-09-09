package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.utils.math.Vector2Utils;
import java.util.Objects;

public class PlatformerComponent extends Component {
  private Vector2 jumpDirection = Vector2.Zero.cpy();

  private boolean jumping = false;

  private boolean doubleJumpPowerup = false;
  private int doubleJumpRemaining = 0;
  private int maxDoubleJump = 0;

  private boolean superJumpPowerup = false;
  private int superJumpScaler = 1;

  private int baseJumpScaler = 3;

  private PhysicsComponent physicsComponent;
  private PhysicsEngine physics;
  ColliderComponent collider;
  DebugRenderer debug = new DebugRenderer();

  public PlatformerComponent(int baseJumpScaler) {
    this.baseJumpScaler = baseJumpScaler;
  }

  public PlatformerComponent(
      int baseJumpScaler,
      boolean doubleJumpPowerup,
      int maxDoubleJump,
      boolean superJumpPowerup,
      int superJumpScaler) {
    this.baseJumpScaler = baseJumpScaler;
    this.doubleJumpPowerup = doubleJumpPowerup;
    this.maxDoubleJump = maxDoubleJump;
    this.doubleJumpRemaining = maxDoubleJump;
    this.superJumpPowerup = superJumpPowerup;
    this.superJumpScaler = superJumpScaler;
  }

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    physics = ServiceLocator.getPhysicsService().getPhysics();
    entity.getEvents().addListener("jump", this::jump);
    collider = entity.getComponent(ColliderComponent.class);
  }

  /*
  *****************************************************************************
  Please note, I've opted for get/set functions instead of making the variables
  public in case a developer puts negative or otherwise "wrong" values accidentally
  for maxDoubleJump/superJumpScaler/baseJumpScaler so that we can catch these
  mistakes early and correct them.
  ******************************************************************************
  */
  // To change doubleJump parameters you need to use this function
  public void setDoubleJump(boolean doubleJumpPowerup, int maxDoubleJump) {
    if (maxDoubleJump < 0)
      throw new IllegalArgumentException("maxDoubleJump should not be less than zero");
    this.maxDoubleJump = maxDoubleJump;
    this.doubleJumpPowerup = doubleJumpPowerup;
  }

  // To change super jump parameters you need to use this function
  public void setSuperJump(boolean superJumpPowerup, int superJumpScaler) {
    if (superJumpScaler <= 0)
      throw new IllegalArgumentException(
          "superJumpScaler should not be zero or less or else" + "it cancels out the jump");
    this.superJumpPowerup = superJumpPowerup;
    this.superJumpScaler = superJumpScaler;
  }

  // To change base jump parameters you need to use this function
  public void setBaseJumpScaler(int baseJumpScaler) {
    if (baseJumpScaler <= 0)
      throw new IllegalArgumentException(
          "baseJumpScaler should not be zero or less since"
              + "that gets rid of jump functionality");
    this.baseJumpScaler = baseJumpScaler;
  }

  public boolean getDoubleJumpBool() {
    return doubleJumpPowerup;
  }

  public int getMaxDoubleJump() {
    return maxDoubleJump;
  }

  public boolean getSuperJumpBool() {
    return superJumpPowerup;
  }

  public int getSuperJumpScaler() {
    return superJumpScaler;
  }

  public int getBaseJumpScaler() {
    return baseJumpScaler;
  }

  // No JumpingBool set since we shouldn't be able to set jumping from
  // outside the function
  public boolean getJumpingBool() {
    return jumping;
  }

  public void updateJump(Vector2 MAX_SPEED) {
    if (jumping) {
      Body body = physicsComponent.getBody();
      Vector2 desiredVelocity = jumpDirection.cpy().scl(MAX_SPEED);
      Vector2 jumpImpulse = desiredVelocity.scl(body.getMass());
      body.applyLinearImpulse(jumpImpulse, body.getWorldCenter(), true);
      jumpDirection = Vector2.Zero.cpy();
      jumping = false;
    }
  }

  public boolean isGrounded() {
    Body body = physicsComponent.getBody();
    // If there's no y velocity then that means the player must not falling or jumping
    // i.e. they're on a platform
    // You could alternatively implement a raycast to determine ifGrounded
    return (body.getLinearVelocity().y == 0);
  }

  // Will return LEFT if can wall jump from the left, return RIGHT if can wall
  // jump from the right. Will return NONE if can not wall jump
  public String canWallJump() {
    RaycastHit leftHit = new RaycastHit();
    RaycastHit rightHit = new RaycastHit();
    int rightOffset = 3; // The rightHit raycast isn't able to hit the left part of the wall
    // as well as the right part of the wall, so we give a boost to the distanceThreshold
    float distanceThreshold = 15f; // The wall jump applies if the collider is X units away
    Vector2 from = entity.getPosition();
    from.y+=5;
    Vector2 fromRight = from;
    fromRight.x+=5;
    Vector2 fromLeft = from;//It's already on the left

    Vector2 vectorToRight = new Vector2(fromRight.x + (distanceThreshold), fromRight.y);
    Vector2 vectorToLeft = new Vector2(fromLeft.x-distanceThreshold, fromLeft.y);
    float left = 100000;
    float right = 100000;
    physics.raycast(fromLeft, vectorToLeft, PhysicsLayer.OBSTACLE, leftHit);
    if (leftHit.point != null) {
      left = Math.abs(from.x - leftHit.point.x);
      System.out.println(left + "LEFT" + leftHit.point.x);
    }else{
      System.out.println("LEFT DOESNT HIT");
    }
    physics.raycast(fromRight, vectorToRight, PhysicsLayer.OBSTACLE, rightHit);
    debug.drawLine(from, rightHit.point);
    if (rightHit.point != null) {
      right = Math.abs(from.x - rightHit.point.x);
      System.out.println(right + "R" + rightHit.point.x);
    }else{
      System.out.println("RIGHT DOESNT HIT");
    }


    if(left<right) {
      if (left <= distanceThreshold && leftHit.point != null) {
        return "LEFT";
      }
    } else {
      if (right <= distanceThreshold && rightHit.point != null) {
        return "RIGHT";
      }
    }
    return "NONE"; // If neither then return NONE
  }

  private void jump(Vector2 direction) {
    // Wall jump takes priority over a normal jump (given the player is not grounded),
    // you cannot normal jump and wall jump at the same time. Wall jump doesn't
    // replenish double jumps
    this.jumpDirection.y = direction.y;

    // Wall jump code
    if ((Objects.equals(canWallJump(), "RIGHT") || Objects.equals(canWallJump(), "LEFT"))
        && !isGrounded()) {
      if (Objects.equals(canWallJump(), "RIGHT")) {
        this.jumpDirection.x = -1;
      } else if (Objects.equals(canWallJump(), "LEFT")) {
        this.jumpDirection.x = 1;
      }
      this.jumpDirection.x *= direction.y;
      // Making sure that we are jumping off the wall with equal x and y forces
      this.jumpDirection.scl(baseJumpScaler);
      if (superJumpPowerup) this.jumpDirection.scl(superJumpScaler);
      jumping = true;
    } // Normal jump code
    else if (isGrounded() || (doubleJumpPowerup && doubleJumpRemaining > 0)) {
      this.jumpDirection.y *= baseJumpScaler;
      if (!isGrounded()) doubleJumpRemaining--;
      if (superJumpPowerup) this.jumpDirection.y *= superJumpScaler;
      jumping = true;
    }
    if (doubleJumpPowerup && isGrounded()) {
      doubleJumpRemaining = maxDoubleJump;
    }
  }
}
