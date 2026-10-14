package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
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
 * <p>Handles player movement and attacks, prevents further player actions after death, and handles
 * the player Bribe ability.
 */
public class PlayerActions extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PlayerActions.class);
  // Thank you Lachlan, you beautiful, beautiful man
  private static final Vector2 MAX_SPEED = new Vector2(30f, 10f); // Metres per second
  private static final Vector2 MAX_JUMP_SPEED = new Vector2(10f, 10f); // Metres per second
  private static final float SLIDE_MAX_TIME = 0.5f; // slide will finifh in 0.5 second
  private static final float BASE_ATTACK_COOLDOWN = 0.5f;
  private static final float SPECIAL_ATTACK_COOLDOWN = 3f;
  private static final int SPECIAL_ATTACK_DAMAGE_MULTIPLIER = 3;
  private static final float AREA_ATTACK_COOLDOWN = 5f;
  private static final float AREA_ATTACK_RADIUS = 2f;
  private static final int AREA_ATTACK_DAMAGE_MULTIPLIER = 2;

  private static final int BRIBE_COST = 50;

  private float attackCooldownRemaining = 0f;
  private float specialAttackCooldownRemaining = 0f;
  private float areaAttackCooldownRemaining = 0f;
  private float attackCooldownMultiplier = 1f;

  private PhysicsComponent physicsComponent;
  private CombatStatsComponent combatStats;
  private HitboxComponent hitboxComponent;
  private PlatformerComponent platformerComponent;
  private PlayerAnimationController playerAnimationControllerComponent;
  private StaminaComponent staminaComponent;

  private Vector2 walkDirection = Vector2.Zero.cpy();
  private Vector2 Speed = MAX_SPEED.cpy();
  private float CrouchSpeedRate = 0.2f;
  private float dashspeed = 5f;
  private float slidespeed = 3f;
  private float SlideTimer = 0f;
  private boolean crouching = false;
  private boolean moving = false;
  private boolean sliding = false;
  private boolean dashing = false;
  private boolean sneaking = false;
  private boolean walkSoundPlaying = false;
  private boolean sneakSoundPlaying = false;
  private boolean slideSoundPlaying = false;
  private boolean frozen = false;

  // Death State
  private boolean dead = false;

  /*
   * The enemy that most recently successfully damaged the player.
   * This is the only enemy that can currently be bribed.
   */
  private Entity lastDamageDealer;

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
    System.out.println("PLAYER ACTIONS CREATED entity=" + entity.getId());

    physicsComponent = entity.getComponent(PhysicsComponent.class);
    platformerComponent = entity.getComponent(PlatformerComponent.class);
    combatStats = entity.getComponent(CombatStatsComponent.class);
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    playerAnimationControllerComponent = entity.getComponent(PlayerAnimationController.class);
    staminaComponent = entity.getComponent(StaminaComponent.class);

    System.out.println(
        "PLAYER ACTIONS COMPONENTS entity="
            + entity.getId()
            + " physics="
            + (physicsComponent != null)
            + " platformer="
            + (platformerComponent != null)
            + " stamina="
            + (staminaComponent != null));

    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);
    entity.getEvents().addListener("specialAttack", this::specialAttack);
    entity.getEvents().addListener("areaAttack", this::areaAttack);

    // Bribe support.
    entity.getEvents().addListener("damagedBy", this::onDamagedBy);

    // Existing movement features.
    entity.getEvents().addListener("dash", this::dash);
    entity.getEvents().addListener("slide", this::slide);
    entity.getEvents().addListener("ctrlChanged", this::ctrlChanged);

    // Existing combat features from main.
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);

    // Death State for player.
    entity.getEvents().addListener("death", this::onDeath);
  }

  @Override
  public void update() {
    StaminaComponent staminaComponent = entity.getComponent(StaminaComponent.class);

    if (staminaComponent != null) {
      staminaComponent.regenerate(ServiceLocator.getTimeSource().getDeltaTime());
    }

    if (attackCooldownRemaining > 0f) {
      attackCooldownRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      attackCooldownRemaining = Math.max(0f, attackCooldownRemaining);
    }

    if (specialAttackCooldownRemaining > 0f) {
      specialAttackCooldownRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      specialAttackCooldownRemaining = Math.max(0f, specialAttackCooldownRemaining);
    }

    if (areaAttackCooldownRemaining > 0f) {
      areaAttackCooldownRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      areaAttackCooldownRemaining = Math.max(0f, areaAttackCooldownRemaining);
    }

    frozen = getEffectiveSpeedMultiplier() == 0;

    playMovementSound();

    if (!dead && (moving || platformerComponent.getJumpingBool())) {
      updateSpeed();
    }

    timerforslide();

    KeyboardPlayerInputComponent keyboardInput =
        entity.getComponent(KeyboardPlayerInputComponent.class);

    if (keyboardInput != null) {
      animationtimer(keyboardInput.getDirection());
    }
  }

  private void animationtimer(String direction) {
    PlayerRenderComponent animator = entity.getComponent(PlayerRenderComponent.class);
    boolean hurtPlaying = playerAnimationControllerComponent.hurtPlaying;
    if (hurtPlaying && animator.isFinished()) {
      playerAnimationControllerComponent.hurtPlaying = false;
    }
    if (animator.isFinished()
        && !animator.getCurrentAnimation().equals("crouchidle")
        && !animator.getCurrentAnimation().equals("Leftcrouchidle")
        && !animator.getCurrentAnimation().equals("Run")
        && !animator.getCurrentAnimation().equals("LeftRun")
        && !animator.getCurrentAnimation().equals("climb")) {

      if (animator.getCurrentAnimation().equals("Jump")
          || animator.getCurrentAnimation().equals("LeftJump")
          || animator.getCurrentAnimation().equals("climb")) {
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
    if (frozen) {
      return;
    }
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
    if (physicsComponent == null) {
      System.out.println("MOVEMENT ERROR entity=" + entity.getId() + " physicsComponent is NULL");
      return;
    }

    Body body = physicsComponent.getBody();

    if (body == null) {
      System.out.println("MOVEMENT ERROR entity=" + entity.getId() + " physics body is NULL");
      return;
    }

    if (crouching) {
      Speed.x = MAX_SPEED.cpy().x * CrouchSpeedRate;
    } else {
      Speed = MAX_SPEED.cpy();
    }

    Vector2 desiredVelocity = walkDirection.cpy().scl(Speed).scl(getEffectiveSpeedMultiplier());

    Vector2 impulse = desiredVelocity.scl(body.getMass());

    body.applyForce(impulse, body.getWorldCenter(), true);

    if (platformerComponent.getJumpingBool()) {
      Quest.incrementGlobalJumps();
    }

    // For the jump portion
    Vector2 jump = frozen ? new Vector2(0f, 0f) : MAX_SPEED;
    platformerComponent.updateJump(jump);
  }

  /**
   * Adds or updates a speed modifier owned by the given key.
   *
   * @param key identifies the owner of this modifier
   * @param multiplier the modifier's contribution
   */
  public void addSpeedModifier(Object key, float multiplier) {
    speedModifiers.put(key, multiplier);
  }

  /**
   * Removes a previously-added speed modifier.
   *
   * @param key the same key passed to addSpeedModifier
   */
  public void removeSpeedModifier(Object key) {
    speedModifiers.remove(key);
  }

  /** Returns the combined effect of all active speed modifiers. */
  public float getEffectiveSpeedMultiplier() {
    float result = 1f;

    for (float value : speedModifiers.values()) {
      result *= value;
    }

    return result;
  }

  public void setAttackSpeedMultiplier(float multiplier) {
    attackCooldownMultiplier = multiplier;
  }

  public float getAttackSpeedMultiplier() {
    return attackCooldownMultiplier;
  }

  /**
   * @return remaining cooldown in seconds for the F-key special attack
   */
  public float getSpecialAttackCooldownRemaining() {
    return specialAttackCooldownRemaining;
  }

  /**
   * @return remaining cooldown in seconds for the G-key area attack
   */
  public float getAreaAttackCooldownRemaining() {
    return areaAttackCooldownRemaining;
  }

  /**
   * Moves the player towards a given direction.
   *
   * @param direction direction to move in
   */
  void walk(Vector2 direction) {
    if (dead || frozen) {
      return;
    }

    this.walkDirection = direction;
    moving = true;
  }

  void stopWalking() {
    this.walkDirection = Vector2.Zero.cpy();

    if (!dead) {
      updateSpeed();
    }

    moving = false;
    walkSoundPlaying = false;
  }

  /** Makes the player attack. */
  void attack() {
    if (dead || attackCooldownRemaining > 0f) {
      return;
    }

    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);

    // Existing melee combat from main
    for (Entity enemy : enemiesInRange) {
      if (hitEnemy(enemy, combatStats.getBaseAttack())) {
        logger.info(
            "Enemy health decreased; health = {}",
            enemy.getComponent(CombatStatsComponent.class).getHealth());

        attackSound.play(AudioSettings.getEffectiveEffectsVolume());
      }
    }

    // Existing weapon functionality
    entity.getEvents().trigger("weaponAttack");

    attackCooldownRemaining = BASE_ATTACK_COOLDOWN * attackCooldownMultiplier;
  }

  /** Hits the nearest enemy in melee range for three times the player's base attack. */
  void specialAttack() {
    if (dead || specialAttackCooldownRemaining > 0f || combatStats == null) {
      return;
    }

    Entity target = getNearestEnemyInRange();

    if (target == null) {
      return;
    }

    int damage =
        (int)
            Math.min(
                (long) combatStats.getBaseAttack() * SPECIAL_ATTACK_DAMAGE_MULTIPLIER,
                Integer.MAX_VALUE);
    if (hitEnemy(target, damage)) {
      entity.getEvents().trigger("specialAttackHit", target);
    }
    specialAttackCooldownRemaining = SPECIAL_ATTACK_COOLDOWN;
  }

  private Entity getNearestEnemyInRange() {
    Entity nearestEnemy = null;
    float nearestDistanceSquared = Float.MAX_VALUE;
    Vector2 playerPosition = entity.getPosition();

    for (Entity enemy : enemiesInRange) {
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (enemy.isDisposed() || stats == null || stats.isDead()) {
        continue;
      }

      float distanceSquared = playerPosition.dst2(enemy.getPosition());

      if (distanceSquared < nearestDistanceSquared) {
        nearestEnemy = enemy;
        nearestDistanceSquared = distanceSquared;
      }
    }

    return nearestEnemy;
  }

  /** Hits every enemy within the player's area-attack radius for twice the base attack. */
  void areaAttack() {
    if (dead || areaAttackCooldownRemaining > 0f || combatStats == null) {
      return;
    }

    Set<Entity> targets = getEnemiesInAreaAttackRange();

    if (targets.isEmpty()) {
      return;
    }

    entity.getEvents().trigger("areaAttackStarted");

    int damage =
        (int)
            Math.min(
                (long) combatStats.getBaseAttack() * AREA_ATTACK_DAMAGE_MULTIPLIER,
                Integer.MAX_VALUE);

    for (Entity target : targets) {
      if (hitEnemy(target, damage)) {
        entity.getEvents().trigger("areaAttackHit", target);
      }
    }

    areaAttackCooldownRemaining = AREA_ATTACK_COOLDOWN;
  }

  /** Resolves a player hit before notifying listeners of the enemy that was struck. */
  private boolean hitEnemy(Entity target, int damage) {
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (target.isDisposed() || targetStats == null || targetStats.isDead()) {
      return false;
    }

    targetStats.hit(combatStats, damage);
    entity.getEvents().trigger("playerAttackHit", target);
    return true;
  }

  private Set<Entity> getEnemiesInAreaAttackRange() {
    World world = ServiceLocator.getPhysicsService().getPhysics().getWorld();
    Vector2 center = entity.getCenterPosition();
    float radiusSquared = AREA_ATTACK_RADIUS * AREA_ATTACK_RADIUS;
    Set<Entity> targets = new HashSet<>();

    world.QueryAABB(
        fixture -> {
          if (!PhysicsLayer.contains(PhysicsLayer.NPC, fixture.getFilterData().categoryBits)) {
            return true;
          }

          Object userData = fixture.getBody().getUserData();

          if (!(userData instanceof BodyUserData bodyUserData)
              || bodyUserData.entity == null
              || bodyUserData.entity.getComponent(CombatStatsComponent.class) == null) {
            return true;
          }

          Entity target = bodyUserData.entity;
          if (!target.isDisposed()
              && !target.getComponent(CombatStatsComponent.class).isDead()
              && center.dst2(target.getCenterPosition()) <= radiusSquared) {
            targets.add(target);
          }

          return true;
        },
        center.x - AREA_ATTACK_RADIUS,
        center.y - AREA_ATTACK_RADIUS,
        center.x + AREA_ATTACK_RADIUS,
        center.y + AREA_ATTACK_RADIUS);

    return targets;
  }

  /** Makes the player dash. */
  void dash(Vector2 direction) {
    if (dead || frozen) {
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
    if (dead || frozen) {
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
      if (staminaComponent == null
          || !staminaComponent.hasEnoughStamina(staminaComponent.getSlideCost())) {
        return;
      }

      staminaComponent.useStamina(staminaComponent.getSlideCost());

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
    if (!sliding) {
      return;
    }

    SlideTimer += Gdx.graphics.getDeltaTime();

    if (SlideTimer >= SLIDE_MAX_TIME) {
      sliding = false;
    }
  }

  /**
   * Remembers the enemy that most recently successfully damaged the player.
   *
   * @param attacker entity that caused the damage
   */
  private void onDamagedBy(Entity attacker) {
    if (dead || attacker == null || attacker == entity) {
      return;
    }

    CombatStatsComponent attackerStats = attacker.getComponent(CombatStatsComponent.class);

    if (attackerStats == null || attackerStats.isDead()) {
      return;
    }

    lastDamageDealer = attacker;

    logger.info("Bribe target set to enemy {}", attacker.getId());
  }

  /**
   * Attempts to bribe the enemy that most recently damaged the player.
   *
   * @return true if the bribe was successfully performed
   */
  public boolean bribeLastAttacker() {
    if (dead || lastDamageDealer == null) {
      if (!dead) {
        entity.getEvents().trigger("bribeNoTarget");
      }
      return false;
    }

    CombatStatsComponent attackerStats = lastDamageDealer.getComponent(CombatStatsComponent.class);

    if (attackerStats == null || attackerStats.isDead()) {
      lastDamageDealer = null;
      entity.getEvents().trigger("bribeNoTarget");
      return false;
    }

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null || !inventory.hasGold(BRIBE_COST)) {
      logger.info("Bribe failed: player does not have {} gold", BRIBE_COST);
      entity.getEvents().trigger("bribeNoGold");
      return false;
    }

    boolean bribed = false;

    MeleeAttackComponent meleeAttack = lastDamageDealer.getComponent(MeleeAttackComponent.class);

    if (meleeAttack != null) {
      bribed = meleeAttack.bribe();
    }

    TouchAttackComponent touchAttack = lastDamageDealer.getComponent(TouchAttackComponent.class);

    if (touchAttack != null) {
      bribed = touchAttack.bribe() || bribed;
    }

    if (!bribed) {
      logger.info("Bribe failed: target has no supported attack component");
      entity.getEvents().trigger("bribeNoTarget");
      return false;
    }

    inventory.addGold(-BRIBE_COST);

    logger.info("Enemy {} bribed successfully for {} gold", lastDamageDealer.getId(), BRIBE_COST);

    entity.getEvents().trigger("enemyBribed", lastDamageDealer);
    entity.getEvents().trigger("bribeSuccess");

    lastDamageDealer = null;

    return true;
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
    System.out.println("PLAYER ACTIONS DEATH entity=" + entity.getId());

    dead = true;
    moving = false;
    walkDirection = Vector2.Zero.cpy();
    lastDamageDealer = null;

    Body body = physicsComponent.getBody();

    body.setLinearVelocity(Vector2.Zero);
    KeyboardPlayerInputComponent input = entity.getComponent(KeyboardPlayerInputComponent.class);
    String direction = input == null ? "Right" : input.getDirection();
    entity.getEvents().trigger("dead", direction);
  }

  public boolean getDashing() {
    return dashing;
  }
}
