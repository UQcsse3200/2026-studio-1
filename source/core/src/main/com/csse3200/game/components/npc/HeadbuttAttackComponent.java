package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Winds up, charges and headbutts the player when triggered, knocking them back. */
public class HeadbuttAttackComponent extends Component {
  private static final String IMPACT_ATLAS = "images/npcs/satyr_impact.atlas";
  private static final float WINDUP_SECONDS = 0.5f;
  private static final float CHARGE_SPEED = 5f;
  private static final float CHARGE_MAX_SECONDS = 1.2f;
  private static final float CHARGE_MAX_DISTANCE = 4f;
  private static final float HEADBUTT_SECONDS = 0.25f;
  private static final float RECOVER_SECONDS = 0.4f;
  private static final int HEADBUTT_DAMAGE = 10;
  private static final float KNOCKBACK_SPEED = 6f;
  private static final float KNOCKBACK_LIFT = 0.4f;
  private static final float IMPACT_SIZE = 0.75f;
  private static final float IMPACT_FRAME_SECONDS = 0.08f;
  private static final float EDGE_INSET = 0.05f;
  private static final float GROUND_CHECK_DEPTH = 0.15f;

  private enum State {
    IDLE,
    WINDUP,
    CHARGE,
    HEADBUTT,
    RECOVER
  }

  private final Entity player;
  private final RaycastHit groundHit = new RaycastHit();
  private PhysicsEngine physics;
  private PhysicsComponent physicsComponent;
  private AnimationRenderComponent animator;
  private State state = State.IDLE;
  private float stateTime = 0f;
  private boolean facingRight = true;
  private float chargeStartX;
  private int playerContacts = 0;
  private boolean bumped = false;

  /**
   * @param player the only entity this headbutt can hit.
   */
  public HeadbuttAttackComponent(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    physics = ServiceLocator.getPhysicsService().getPhysics();
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    animator = entity.getComponent(AnimationRenderComponent.class);
    entity.getEvents().addListener("headbutt", this::startWindup);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
    entity.getEvents().addListener("collisionEnd", this::onCollisionEnd);
  }

  @Override
  public void update() {
    if (state == State.IDLE) {
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    stateTime += delta;

    switch (state) {
      case WINDUP:
        holdStill();
        if (stateTime >= WINDUP_SECONDS) {
          startCharge();
        }
        break;
      case CHARGE:
        updateCharge(delta);
        break;
      case HEADBUTT:
        holdStill();
        if (stateTime >= HEADBUTT_SECONDS) {
          setState(State.RECOVER);
        }
        break;
      case RECOVER:
        holdStill();
        if (stateTime >= RECOVER_SECONDS) {
          finish();
        }
        break;
      default:
        break;
    }

    if (state != State.IDLE) {
      playStateAnimation();
    }
  }

  /** Starts the wind-up towards the target, unless an attack is already running. */
  private void startWindup(Entity target) {
    if (state != State.IDLE || target == null) {
      return;
    }
    ProvokedComponent provoked = entity.getComponent(ProvokedComponent.class);
    if (provoked != null) {
      provoked.setAttacking(true);
    }
    PhysicsMovementComponent movement = entity.getComponent(PhysicsMovementComponent.class);
    if (movement != null) {
      movement.setMoving(false);
    }
    facingRight = target.getCenterPosition().x >= entity.getCenterPosition().x;
    setState(State.WINDUP);
  }

  /** Starts running at the player in the direction faced during the wind-up. */
  private void startCharge() {
    chargeStartX = entity.getPosition().x;
    bumped = false;
    setState(State.CHARGE);
  }

  /** Moves the charge on, hitting the player on contact or giving up when blocked or too far. */
  private void updateCharge(float delta) {
    if (playerContacts > 0) {
      hitPlayer();
      return;
    }
    float travelled = Math.abs(entity.getPosition().x - chargeStartX);
    if (stateTime >= CHARGE_MAX_SECONDS
        || travelled >= CHARGE_MAX_DISTANCE
        || bumped
        || !isGroundAhead(delta)) {
      holdStill();
      setState(State.RECOVER);
      return;
    }
    Body body = physicsComponent.getBody();
    body.setLinearVelocity(facingRight ? CHARGE_SPEED : -CHARGE_SPEED, body.getLinearVelocity().y);
  }

  /** Damages and knocks back the player unless a shield blocks it, then plays the impact. */
  private void hitPlayer() {
    holdStill();
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    if (stats != null && !isBlockedByShield(stats)) {
      stats.addHealth(-HEADBUTT_DAMAGE);
      knockBack();
    }
    spawnImpact();
    setState(State.HEADBUTT);
  }

  /** Checks the player's shields, using up an upgrade shield charge if one blocks the hit. */
  private boolean isBlockedByShield(CombatStatsComponent stats) {
    if (stats.getShieldHits() > 0) {
      stats.setShieldHits(stats.getShieldHits() - 1);
      return true;
    }
    ShieldComponent shield = player.getComponent(ShieldComponent.class);
    return shield != null && shield.isActive();
  }

  /** Pushes the player away from this entity with a small upward lift. */
  private void knockBack() {
    PhysicsComponent playerPhysics = player.getComponent(PhysicsComponent.class);
    if (playerPhysics == null) {
      return;
    }
    Body body = playerPhysics.getBody();
    Vector2 direction = new Vector2(facingRight ? 1f : -1f, KNOCKBACK_LIFT).nor();
    body.applyLinearImpulse(
        direction.scl(KNOCKBACK_SPEED * body.getMass()), body.getWorldCenter(), true);
  }

  /** Spawns a one-shot impact burst at this entity's leading edge. */
  private void spawnImpact() {
    AnimationRenderComponent impactAnimator =
        new AnimationRenderComponent(new TextureAtlas(Gdx.files.internal(IMPACT_ATLAS)), true);
    impactAnimator.addAnimation("impact", IMPACT_FRAME_SECONDS, Animation.PlayMode.NORMAL);

    float contactX =
        entity.getCenterPosition().x + (facingRight ? 1f : -1f) * entity.getScale().x / 2f;
    float contactY = entity.getCenterPosition().y;
    Entity impact =
        new Entity().addComponent(impactAnimator).addComponent(new SplashEffectComponent("impact"));
    impact.setScale(IMPACT_SIZE, IMPACT_SIZE);
    impact.setPosition(contactX - IMPACT_SIZE / 2f, contactY - IMPACT_SIZE / 2f);
    ServiceLocator.getEntityService().register(impact);
  }

  /** Checks for floor just ahead of the leading foot, so the charge stops at platform edges. */
  private boolean isGroundAhead(float delta) {
    float lookAhead = CHARGE_SPEED * delta;
    Vector2 position = entity.getPosition();
    float x =
        facingRight
            ? position.x + entity.getScale().x * (1f - EDGE_INSET) + lookAhead
            : position.x + entity.getScale().x * EDGE_INSET - lookAhead;
    Vector2 from = new Vector2(x, position.y);
    Vector2 to = new Vector2(x, position.y - GROUND_CHECK_DEPTH);
    return physics.raycast(from, to, PhysicsLayer.OBSTACLE, groundHit);
  }

  /** Stops sideways movement while keeping gravity. */
  private void holdStill() {
    Body body = physicsComponent.getBody();
    body.setLinearVelocity(0f, body.getLinearVelocity().y);
  }

  /** Ends the attack and hands control back to the normal behaviour. */
  private void finish() {
    state = State.IDLE;
    ProvokedComponent provoked = entity.getComponent(ProvokedComponent.class);
    if (provoked != null) {
      provoked.setAttacking(false);
    }
  }

  /** Switches to a new stage of the attack and restarts its timer. */
  private void setState(State newState) {
    state = newState;
    stateTime = 0f;
  }

  /** Shows this stage's animation, restoring it if the walk/idle controller replaced it. */
  private void playStateAnimation() {
    if (animator == null) {
      return;
    }
    String name = animationFor(state) + (facingRight ? "r" : "l");
    if (!name.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(name);
    }
  }

  /** Returns the animation name, without its direction, for a stage of the attack. */
  private static String animationFor(State attackState) {
    switch (attackState) {
      case WINDUP:
        return "windup";
      case CHARGE:
        return "charge";
      case HEADBUTT:
        return "headbutt";
      default:
        return "idle";
    }
  }

  /** Counts contacts with the player and notes bumps into other bodies during a charge. */
  private void onCollisionStart(Fixture me, Fixture other) {
    short layer = other.getFilterData().categoryBits;
    if (PhysicsLayer.contains(PhysicsLayer.PLAYER, layer)) {
      playerContacts++;
      return;
    }
    Object userData = other.getBody().getUserData();
    Entity otherEntity = userData instanceof BodyUserData data ? data.entity : null;
    if (state == State.CHARGE
        && !other.isSensor()
        && otherEntity != null
        && otherEntity != player
        && otherEntity != entity
        && !PhysicsLayer.contains(PhysicsLayer.OBSTACLE, layer)) {
      bumped = true;
    }
  }

  /** Stops counting a contact with the player once it ends. */
  private void onCollisionEnd(Fixture me, Fixture other) {
    if (PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      playerContacts = Math.max(0, playerContacts - 1);
    }
  }
}
