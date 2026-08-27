package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final Vector2 MAX_SPEED = new Vector2(30f, 3f); // Metres per second

  private PhysicsComponent physicsComponent;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private float dashspeed = 5f;
  private boolean moving = false;
  private final String NORMAL_TEXTURE = "images/box_boy_leaf.png";
  private final String CROUCH_TEXTURE = "images/box_boy_crouch.png";
  private TextureRenderComponent textureRenderComponent;

  private PlatformerComponent platformerComponent;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("dash", this::dash);
    textureRenderComponent = entity.getComponent(TextureRenderComponent.class);
    entity.getEvents().addListener("ctrlChanged", this::ctrlChanged);
  }

  @Override
  public void update() {
    if (moving || platformerComponent.getJumpingBool()) {
      updateSpeed();
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 desiredVelocity = walkDirection.cpy().scl(MAX_SPEED);
    // impulse = desiredVel * mass
    Vector2 impulse = desiredVelocity.scl(body.getMass());
    body.applyForce(impulse, body.getWorldCenter(), true);

    // For the jump portion
    platformerComponent.updateJump(MAX_SPEED);
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

  /** Makes the player dash */
  void dash(Vector2 direction) {
    Body body = physicsComponent.getBody();
    Vector2 impulse = direction.cpy().scl(dashspeed);
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  private void ctrlChanged(boolean pressed) {
    if (pressed) {
      textureRenderComponent.setTexture(CROUCH_TEXTURE);
    } else {
      textureRenderComponent.setTexture(NORMAL_TEXTURE);
    }
  }
}
