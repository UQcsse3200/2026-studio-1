package com.csse3200.game.components.player;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Action component for interacting with the player.
 *
 * <p>Handles player movement and attacks, and prevents further player actions after the death event
 * is triggered.
 */
public class PlayerActions extends Component {
  // Thank you Lachlan, you beautiful, beautiful man
  private static final Vector2 MAX_SPEED = new Vector2(30f, 3f); // Metres per second

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;

  private Vector2 walkDirection = Vector2.Zero.cpy();
  private float dashspeed = 5f;
  private boolean moving = false;

  // Death State
  private boolean dead = false;

  private final String NORMAL_TEXTURE = "images/box_boy_leaf.png";
  private final String CROUCH_TEXTURE = "images/box_boy_crouch.png";
  private TextureRenderComponent textureRenderComponent;

  private final Set<Entity> enemiesInRange = new HashSet<>();

  // Active speed modifiers, keyed by whichever effect/component owns them.
  // Effective multiplier is the product of all active values.
  // 1 = normal, 0 = paused, <1 = slowed, >1 = sped up
  private final Map<Object, Float> speedModifiers = new HashMap<>();

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    textureRenderComponent = entity.getComponent(TextureRenderComponent.class);

    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);

    // Existing movement features
    entity.getEvents().addListener("dash", this::dash);
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
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 desiredVelocity = walkDirection.cpy().scl(MAX_SPEED).scl(getEffectiveSpeedMultiplier());
    // impulse = desiredVel * mass
    Vector2 impulse = desiredVelocity.scl(body.getMass());
    body.applyForce(impulse, body.getWorldCenter(), true);

    // For the jump portion
    platformerComponent.updateJump(MAX_SPEED);
  }

  /**
   * Adds or updates a speed modifier owned by the given key. The effective speed multiplier is the
   * product of all currently active modifiers.
   *
   * @param key identifies the owner of this modifier (e.g. the effect component itself), so it can
   *     be removed later without affecting other active effects.
   * @param multiplier the modifier's contribution (1 = no change, 0 = pause, 0.5 = half speed).
   */
  public void addSpeedModifier(Object key, float multiplier) {
    speedModifiers.put(key, multiplier);
  }

  /**
   * Removes a previously-added speed modifier.
   *
   * @param key the same key passed to {@link #addSpeedModifier(Object, float)}.
   */
  public void removeSpeedModifier(Object key) {
    speedModifiers.remove(key);
  }

  /**
   * Returns the combined effect of all active speed modifiers (their product). 1 if none active.
   */
  public float getEffectiveSpeedMultiplier() {
    float result = 1f;
    for (float value : speedModifiers.values()) {
      result *= value;
    }
    return result;
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
  }

  private void ctrlChanged(boolean pressed) {
    if (dead) {
      return;
    }

    if (pressed) {
      textureRenderComponent.setTexture(CROUCH_TEXTURE);
    } else {
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
}
