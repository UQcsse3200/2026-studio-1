package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final Vector2 MAX_SPEED = new Vector2(3f, 3f); // Metres per second

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private boolean moving = false;

  private final Set<Entity> enemiesInRange = new HashSet<>();

  // Active speed modifiers, keyed by whichever effect/component owns them.
  // Effective multiplier is the product of all active values.
  // 1 = normal, 0 = paused, <1 = slowed, >1 = sped up
  private final Map<Object, Float> speedModifiers = new HashMap<>();
  // jumping is covered by platformerComponent.getJumpingBool()
  private PlatformerComponent platformerComponent;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);

    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
  }

  @Override
  public void update() {
    if (moving) {
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

    for (Entity enemy : enemiesInRange) {
      CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
      if (enemyStats != null) {
        enemyStats.hit(combatStats);
      }
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
}
