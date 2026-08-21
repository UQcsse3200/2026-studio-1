package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final Vector2 MAX_SPEED = new Vector2(30f, 3f); // Metres per second

  private PhysicsComponent physicsComponent;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 jumpDirection = Vector2.Zero.cpy();
  private boolean moving = false;
  private boolean jumping = false;
  private PlatformerComponent platformerComponent;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("jump", this::jump);
  }

  @Override
  public void update() {
    if (moving || jumping) {
      updateSpeed();
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 velocity = body.getLinearVelocity();
    Vector2 desiredVelocity = walkDirection.cpy().scl(MAX_SPEED);
    // impulse = (desiredVel - currentVel) * mass
    Vector2 impulse = desiredVelocity.scl(body.getMass());
    body.applyForce(impulse, body.getWorldCenter(), true);
    //The y velocity is being killed off for some reason.

    //For the jump portion
    if(jumping) {
      desiredVelocity = jumpDirection.cpy().scl(MAX_SPEED);
      Vector2 jumpImpulse = desiredVelocity.scl(body.getMass());
      body.applyLinearImpulse(jumpImpulse, body.getWorldCenter(), true);
      jumpDirection.y = 0;
      jumping = false;
    }
  }
  public boolean isGrounded(){
    /*
    //Raycast solution is not working for some reason
    RaycastHit hit = new RaycastHit();
    Body body = physicsComponent.getBody();

    //Raycast is not hitting anything at any range
    physicsEngine.raycast(entity.getPosition(), Vector2Utils.DOWN, PhysicsLayer.ALL, hit);
    System.out.print(entity.getPosition());
    //If the entity is too far from the ground (1 unit or greater) then they're
    //not grounded
    System.out.println(physicsEngine.raycast(entity.getPosition(), Vector2Utils.DOWN, PhysicsLayer.ALL, hit));
    if(hit.point == null){
      return false;
    }else{
        //If the entity is too far from the ground (1 unit or greater) then they're
        //not grounded
        if ((entity.getCenterPosition().y-(hit.point.y)) <1f){
          int a = 1/0;
          return true;
        }else{
          int a = 1/0;
          return false;
        }
    }
    */
    Body body = physicsComponent.getBody();
    //If there's no y velocity then that means the player must be on a platform
    //I tried implementing this with a raycast but it didn't work for me.
    return (body.getLinearVelocity().y ==0);
  }
  /**
   * Moves the player towards a given direction.
   *
   * @param direction direction to move in
   */
  void walk(Vector2 direction) {
    this.walkDirection = direction;
    moving = true;
  }
  void jump(Vector2 direction){
    if(isGrounded()){
      this.jumpDirection.y = direction.y;
      jumping = true;
    }
  }


  /** Stops the player from walking. */
  void stopWalking() {
    this.walkDirection = Vector2.Zero.cpy();
    updateSpeed();
    moving = false;
  }

  /** Makes the player attack. */
  void attack() {
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }
}
