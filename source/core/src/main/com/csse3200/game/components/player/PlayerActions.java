package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;

/**
 * Action component for interacting with the player.
 *
 * <p>Handles player movement and attacks, and prevents further player actions after the death event
 * is triggered.
 */
public class PlayerActions extends Component {
  private static final Vector2 MAX_SPEED = new Vector2(30f, 3f); // Metres per second
  private static final float SlideMaxTime = 0.5f; // slide will finifh in 0.5 second

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private PlatformerComponent platformerComponent;

  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 Speed = MAX_SPEED.cpy();
  private float CrouchSpeedRate = 0.2f; // Crouchspeed = MAX_SPEED * Crouchspeedrate
  private float dashspeed = 5f;
  private float slidespeed = 3f;
  private float SlideTimer = 0f; // slide will finifh in 0.5 second
  private boolean crouching = false;
  private boolean moving = false;
  private boolean sliding = false;
  private boolean dashing = false;
  private boolean sneaking = false;
  private boolean walkSoundPlaying = false;
  private boolean sneakSoundPlaying = false;
  private boolean slideSoundPlaying = false;

  // Death State
  private boolean dead = false;

  private final String NORMAL_TEXTURE = "images/box_boy_leaf.png";
  private final String CROUCH_TEXTURE = "images/box_boy_crouch.png";
  private final String SLIDE_TEXTURE = "images/box_boy_slide.png";
  private final String WALKING_SE = "sounds/walking1.mp3";
  private final String JUMP_SE = "sounds/jump.mp3";
  private final String DASH_SE = "sounds/dash.mp3";
  private final String SNEAK_SE = "sounds/sneaking1.mp3";
  private final String SLIDE_SE = "sounds/slide.mp3";
  private TextureRenderComponent textureRenderComponent;

  private final Set<Entity> enemiesInRange = new HashSet<>();

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    textureRenderComponent = entity.getComponent(TextureRenderComponent.class);

    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);

    // Existing movement features
    entity.getEvents().addListener("dash", this::dash);
    entity.getEvents().addListener("slide", this::slide);
    textureRenderComponent = entity.getComponent(TextureRenderComponent.class);
    entity.getEvents().addListener("ctrlChanged", this::ctrlChanged);

    // Existing combat features from main
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);

    // Death State
    entity.getEvents().addListener("death", this::onDeath);
  }

  @Override
  public void update() {
    if (!dead && (moving || platformerComponent.getJumpingBool())) {
      updateSpeed();
    }
    timerforslide();
  }

  public void playMovementSound() {
    Sound walkSound = ServiceLocator.getResourceService().getAsset(WALKING_SE, Sound.class);
    Sound sneakSound = ServiceLocator.getResourceService().getAsset(SNEAK_SE, Sound.class);
    if (dashing) {
      Sound dashSound = ServiceLocator.getResourceService().getAsset(DASH_SE, Sound.class);
      dashSound.play();
      dashing = false;
    } else if (platformerComponent.getJumpingBool()) {
      Sound jumpSound = ServiceLocator.getResourceService().getAsset(JUMP_SE, Sound.class);
      jumpSound.play();
    } else if (sliding) {
      Sound slideSound = ServiceLocator.getResourceService().getAsset(SLIDE_SE, Sound.class);
      if (!slideSoundPlaying) {
        slideSound.play();
        slideSoundPlaying = true;
      }
    } else if (moving && platformerComponent.isGrounded()) {
      if (sneaking) {
        if (!sneakSoundPlaying) {
          sneakSound.loop();
          sneakSoundPlaying = true;
        }
        walkSound.stop();
        walkSoundPlaying = false;
      } else {
        if (!walkSoundPlaying) {
          walkSound.loop();
          walkSoundPlaying = true;
        }
        sneakSound.stop();
        sneakSoundPlaying = false;
      }
    }
    if (!moving) {
      walkSound.stop();
      sneakSound.stop();
      walkSoundPlaying = false;
      sneakSoundPlaying = false;
    }
    if (!sliding) {
      slideSoundPlaying = false;
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    if (crouching == true) {
      Speed.x = MAX_SPEED.cpy().x * CrouchSpeedRate;
    } else {
      Speed = MAX_SPEED.cpy();
    }
    Vector2 desiredVelocity = walkDirection.cpy().scl(Speed);
    // impulse = desiredVel * mass
    Vector2 impulse = desiredVelocity.scl(body.getMass());
    body.applyForce(impulse, body.getWorldCenter(), true);

    // Existing jump functionality
    platformerComponent.updateJump(MAX_SPEED);
  }

  /**
   * Moves the player towards a given direction.
   *
   * @param direction direction to move in
   */
  void walk(Vector2 direction) {
    if (dead) {
      return;
    }

    this.walkDirection = direction;
    moving = true;
  }

  /** Stops the player from walking. */
  void stopWalking() {
    this.walkDirection = Vector2.Zero.cpy();

    if (!dead) {
      updateSpeed();
    }

    moving = false;
  }

  /** Makes the player attack. */
  void attack() {
    if (dead) {
      return;
    }

    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();

    // Existing melee combat from main
    for (Entity enemy : enemiesInRange) {
      CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
      if (enemyStats != null) {
        enemyStats.hit(combatStats);
      }
    }

    // Existing weapon functionality
    entity.getEvents().trigger("weaponAttack");
  }

  /** Makes the player dash. */
  void dash(Vector2 direction) {
    if (dead) {
      return;
    }

    Body body = physicsComponent.getBody();
    Vector2 impulse = direction.cpy().scl(dashspeed);
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    dashing = true;
  }

  private void ctrlChanged(boolean pressed) {
    if (dead) {
      return;
    }

    if (pressed) {
      textureRenderComponent.setTexture(CROUCH_TEXTURE);
      sneaking = true;
      crouching = true;
      updateSpeed();
    } else {
      textureRenderComponent.setTexture(NORMAL_TEXTURE);
      sneaking = false;
      crouching = false;
      updateSpeed();
    }
  }

  private void slide(boolean pressed) {
    if (pressed) {
      sliding = true;
      SlideTimer = 0;
      textureRenderComponent.setTexture(SLIDE_TEXTURE);
      slidingAction(walkDirection.cpy());

    } else {
      sliding = false;
      textureRenderComponent.setTexture(NORMAL_TEXTURE);
    }
  }

  private void slidingAction(Vector2 direction) {
    Body body = physicsComponent.getBody();
    Vector2 impulse = direction.cpy().scl(slidespeed);
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  private void timerforslide() {
    if (sliding != true) return;

    SlideTimer += Gdx.graphics.getDeltaTime();
    if (SlideTimer >= SlideMaxTime) { // finish slide
      sliding = false;
      textureRenderComponent.setTexture(NORMAL_TEXTURE);
    }
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (!PhysicsLayer.contains(PhysicsLayer.NPC, other.getFilterData().categoryBits)) {
      return;
    }

    BodyUserData userData = (BodyUserData) other.getBody().getUserData();
    if (userData != null && userData.entity != null) {
      enemiesInRange.add(userData.entity);
    }
  }

  private void onCollisionEnd(Fixture me, Fixture other) {
    if (hitboxComponent.getFixture() != me) {
      return;
    }

    if (!PhysicsLayer.contains(PhysicsLayer.NPC, other.getFilterData().categoryBits)) {
      return;
    }

    BodyUserData userData = (BodyUserData) other.getBody().getUserData();
    if (userData != null && userData.entity != null) {
      enemiesInRange.remove(userData.entity);
    }
  }

  /** Stops all player actions when the player dies. */
  private void onDeath() {
    dead = true;
    moving = false;
    walkDirection = Vector2.Zero.cpy();

    Body body = physicsComponent.getBody();
    body.setLinearVelocity(Vector2.Zero);
  }

  public boolean getDashing() {
    return dashing;
  }
}
