package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.AudioSettings;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.PlayerRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action component for interacting with the player.
 *
 * <p>Handles player movement and attacks, and prevents further player actions after the death event
 * is triggered.
 */
public class PlayerActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerActions.class);
  // Thank you Lachlan, you beautiful, beautiful man
  private static final Vector2 MAX_SPEED = new Vector2(30f, 10f); // Metres per second
  private static final float SlideMaxTime = 0.5f; // slide will finifh in 0.5 second
  private static final float BASE_ATTACK_COOLDOWN = 0.5f;
  private float attackCooldownRemaining = 0f;
  private float attackCooldownMultiplier = 1f;

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private PlatformerComponent platformerComponent;
  private StaminaComponent staminaComponent;

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

  private final String WALKING_SE = "sounds/walking1.mp3";
  private final String JUMP_SE = "sounds/jump.mp3";
  private final String DASH_SE = "sounds/dash.mp3";
  private final String SNEAK_SE = "sounds/sneaking1.mp3";
  private final String SLIDE_SE = "sounds/slide.mp3";

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
    staminaComponent = entity.getComponent(StaminaComponent.class);

    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);

    // Existing movement features
    entity.getEvents().addListener("dash", this::dash);
    entity.getEvents().addListener("slide", this::slide);
    entity.getEvents().addListener("ctrlChanged", this::ctrlChanged);

    // Existing combat features from main
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);

    // Death State for player
    entity.getEvents().addListener("death", this::onDeath);
  }

  @Override
  public void update() {

    StaminaComponent staminaComponent = entity.getComponent(StaminaComponent.class);

    if (staminaComponent != null) {
      staminaComponent.regenerate(
          staminaComponent.getRegenRate() * ServiceLocator.getTimeSource().getDeltaTime());
    }

    if (attackCooldownRemaining > 0f) {
      attackCooldownRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      attackCooldownRemaining = Math.max(0f, attackCooldownRemaining);
    }

    playMovementSound();
    if (!dead && (moving || platformerComponent.getJumpingBool())) {
      updateSpeed();
    }
    timerforslide();
    String direction = entity.getComponent(KeyboardPlayerInputComponent.class).getDirection();
    animationtimer(direction);
  }

  private void animationtimer(String direction) {
    PlayerRenderComponent animator = entity.getComponent(PlayerRenderComponent.class);
    if (animator.isFinished()
        && !animator.getCurrentAnimation().equals("crouchidle")
        && !animator.getCurrentAnimation().equals("Leftcrouchidle")
        && !animator.getCurrentAnimation().equals("Run")
        && !animator.getCurrentAnimation().equals("LeftRun")) {

      if (animator.getCurrentAnimation().equals("Jump")
          || animator.getCurrentAnimation().equals("LeftJump")) {
        if (!walkDirection.isZero()) {
          entity.getEvents().trigger("run", direction);
        } else {
          entity.getEvents().trigger("idle", direction);
        }
      } else {
        entity.getEvents().trigger("idle", direction);
      }
    }
  }

  public void playMovementSound() {
    Sound walkSound = ServiceLocator.getResourceService().getAsset(WALKING_SE, Sound.class);
    Sound sneakSound = ServiceLocator.getResourceService().getAsset(SNEAK_SE, Sound.class);
    if (dashing) {
      Sound dashSound = ServiceLocator.getResourceService().getAsset(DASH_SE, Sound.class);
      dashSound.play(AudioSettings.getEffectiveEffectsVolume());
      dashing = false;
    } else if (platformerComponent.getJumpingBool()) {
      Sound jumpSound = ServiceLocator.getResourceService().getAsset(JUMP_SE, Sound.class);
      jumpSound.play(AudioSettings.getEffectiveEffectsVolume());
    } else if (sliding) {
      Sound slideSound = ServiceLocator.getResourceService().getAsset(SLIDE_SE, Sound.class);
      if (!slideSoundPlaying) {
        slideSound.play(AudioSettings.getEffectiveEffectsVolume());
        slideSoundPlaying = true;
      }
    } else if (moving && platformerComponent.isGrounded()) {
      if (sneaking) {
        if (!sneakSoundPlaying) {
          sneakSound.loop(AudioSettings.getEffectiveEffectsVolume());
          sneakSoundPlaying = true;
        }
        walkSound.stop();
        walkSoundPlaying = false;
      } else {
        if (!walkSoundPlaying) {
          walkSound.loop(AudioSettings.getEffectiveEffectsVolume());
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
    Vector2 desiredVelocity = walkDirection.cpy().scl(Speed).scl(getEffectiveSpeedMultiplier());
    // impulse = desiredVel * mass
    Vector2 impulse = desiredVelocity.scl(body.getMass());
    body.applyForce(impulse, body.getWorldCenter(), true);

    // To track player global stats
    if (platformerComponent.getJumpingBool()) {
      Quest.incrementGlobalJumps();
    }
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
  public void setAttackSpeedMultiplier(float multiplier) {
    attackCooldownMultiplier = multiplier;
  }

  public float getAttackSpeedMultiplier() {
    return attackCooldownMultiplier;
  }

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
    if (dead || attackCooldownRemaining > 0f) return;

    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);

    // Existing melee combat from main
    for (Entity enemy : enemiesInRange) {
      CombatStatsComponent enemyStats = enemy.getComponent(CombatStatsComponent.class);
      if (enemyStats != null) {
        enemyStats.hit(combatStats);
        logger.info("Enemy health decreased; health = {}", enemyStats.getHealth());
        attackSound.play(AudioSettings.getEffectiveEffectsVolume());
      }
    }

    // Existing weapon functionality
    entity.getEvents().trigger("weaponAttack");
    attackCooldownRemaining = BASE_ATTACK_COOLDOWN * attackCooldownMultiplier;
  }

  /** Makes the player dash. */
  void dash(Vector2 direction) {
    if (dead) {
      return;
    }

    if (!staminaComponent.hasEnoughStamina(staminaComponent.getDashCost())) {
      return;
    }

    staminaComponent.useStamina(staminaComponent.getDashCost());

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
      sneaking = true;
      crouching = true;
      updateSpeed();
    } else {
      sneaking = false;
      crouching = false;
      updateSpeed();
    }
  }

  private void slide(boolean pressed) {
    if (pressed) {
      sliding = true;
      SlideTimer = 0;
      slidingAction(walkDirection.cpy());
    } else {
      sliding = false;
    }
  }

  private void slidingAction(Vector2 direction) {
    Body body = physicsComponent.getBody();
    Vector2 impulse = direction.cpy().scl(slidespeed);
    if (impulse.x != 0f) {
      entity
          .getEvents()
          .trigger(
              "sliding", entity.getComponent(KeyboardPlayerInputComponent.class).getDirection());
    }
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  private void timerforslide() {
    if (sliding != true) return;

    SlideTimer += Gdx.graphics.getDeltaTime();
    if (SlideTimer >= SlideMaxTime) { // finish slide
      sliding = false;
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
