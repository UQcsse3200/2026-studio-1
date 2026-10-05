package com.csse3200.game.components.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.enemies.ZeusConfig;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the Zeus boss fight: his three phases, his movement, and every attack he makes.
 *
 * <ul>
 *   <li><b>Floor</b> (full health): floats along the floor, swinging at a player in reach and
 *       throwing bolts at one who is not.
 *   <li><b>Throne</b>: rises to the balcony over his starting spot and calls ground strikes under
 *       the player, with the odd thrown bolt.
 *   <li><b>Enraged</b>: slams back to the floor behind a shockwave, then uses every attack, faster.
 * </ul>
 *
 * <p>Zeus floats, so he is moved directly rather than by the physics engine, and his attacks are
 * resolved by overlap with the player rather than by colliders: a bolt, a strike column and a
 * shockwave are plain data here, drawn by {@link ZeusEffectsRenderComponent}. Each attack shows its
 * warning for a fixed time before it can hurt, and hurts once.
 *
 * <p>The arena is read from the level given to {@link #setLevel(LevelView)}: the floor beneath his
 * spawn, the walls either side, and the highest platform above him, which is his throne balcony.
 *
 * <p>Requires a {@link CombatStatsComponent} on this entity. Fires {@link #DEFEATED_EVENT} on this
 * entity once his death has played out.
 */
public class ZeusBossComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ZeusBossComponent.class);

  /** Fired on Zeus's entity when he has been defeated and is about to be removed. */
  public static final String DEFEATED_EVENT = "bossDefeated";

  /** The stages of the fight, in the order they happen. */
  public enum Phase {
    FLOOR,
    THRONE,
    ENRAGED
  }

  /** What Zeus is doing at this moment. */
  enum Action {
    IDLE,
    TRAVEL,
    MELEE,
    BOLT,
    STRIKE,
    SHOCKWAVE,
    DYING
  }

  static final float MELEE_WINDUP = 0.25f;
  static final float MELEE_DURATION = 0.5f;
  static final float BOLT_WINDUP = 0.6f;
  static final float BOLT_WINDUP_ENRAGED = 0.4f;
  static final float BOLT_SPEED = 3.9f;
  static final float STRIKE_WARNING = 0.9f;
  static final float STRIKE_WARNING_ENRAGED = 0.7f;
  static final float STRIKE_DURATION = 0.4f;
  static final float STRIKE_HEIGHT = 3f;
  static final float SHOCKWAVE_WARNING = 0.5f;
  static final float SHOCKWAVE_SPEED = 3f;
  static final float SHOCKWAVE_WIDTH = 1f;
  static final float SHOCKWAVE_HEIGHT = 0.5f;

  /** How far ahead of a shockwave the floor lights up, in world units. */
  static final float SHOCKWAVE_LEAD = 2.5f;

  /** The wind-up of the cast animation is its first four frames; the bolt leaves on the fifth. */
  private static final int CAST_WINDUP_FRAMES = 4;

  private static final int CAST_RECOVERY_FRAMES = 3;
  private static final float STRIKE_DAMAGE_WINDOW = 0.2f;
  private static final float STRIKE_HALF_WIDTH = 0.3f;
  private static final float STRIKE_CAST_TIME = 0.5f;
  private static final float SHOCKWAVE_RECOVERY = 0.4f;
  private static final float BOLT_LIFETIME = 8f;
  private static final float BOLT_HALF_WIDTH = 0.25f;
  private static final float BOLT_HALF_HEIGHT = 0.12f;
  private static final float BOLT_FLOOR_HEIGHT = 0.4f;
  private static final float FLAT_SHOT_TOLERANCE = 0.75f;
  private static final float OPENING_DELAY = 1.5f;
  private static final float TRAVEL_SPEED = 6f;
  private static final float SLAM_SPEED = 14f;
  private static final float ENRAGED_SPEED_BOOST = 1.35f;
  private static final float HOVER_HEIGHT = 0.1f;
  private static final float BOB_HEIGHT = 0.06f;
  private static final float BOB_SPEED = 3f;
  private static final float HURT_TIME = 0.25f;
  private static final float DEATH_TIME = 1f;
  private static final float FALLBACK_HALF_ARENA = 10f;
  private static final float FALLBACK_PERCH_HEIGHT = 6f;

  private final ZeusConfig config;
  private final float damageMultiplier;
  private final float cooldownMultiplier;

  private Entity target;
  private CombatStatsComponent targetStats;
  private CombatStatsComponent stats;
  private AnimationRenderComponent animator;
  private LevelView level;

  private int maxHealth;
  private int lastHealth;
  private float homeX;
  private float floorY;
  private float perchY;
  private float arenaLeft;
  private float arenaRight;

  private Phase phase = Phase.FLOOR;
  private Action action = Action.IDLE;
  private float actionTime;
  private float cooldown = OPENING_DELAY;
  private boolean released;
  private boolean facingRight;
  private boolean moving;
  private boolean slamPending;
  private boolean defeated;
  private int attackCount;
  private float hurtTime;
  private float elapsed;
  private final Vector2 travelTarget = new Vector2();
  private float travelSpeed;

  private final List<Bolt> bolts = new ArrayList<>();
  private final List<Strike> strikes = new ArrayList<>();
  private final List<Wave> waves = new ArrayList<>();
  private final Rectangle targetBox = new Rectangle();
  private final Rectangle hitBox = new Rectangle();

  /**
   * @param target the player Zeus fights
   * @param config his stats
   * @param damageMultiplier scales every attack's damage, for difficulty
   * @param cooldownMultiplier scales the pause between his attacks, for difficulty
   */
  public ZeusBossComponent(
      Entity target, ZeusConfig config, float damageMultiplier, float cooldownMultiplier) {
    this.target = target;
    this.config = config;
    this.damageMultiplier = damageMultiplier;
    this.cooldownMultiplier = cooldownMultiplier;
  }

  /**
   * Gives Zeus the level he fights in. Set before the entity is created; without one he treats his
   * starting height as the floor of an open arena.
   *
   * @param level the level to read the arena from
   */
  public void setLevel(LevelView level) {
    this.level = level;
  }

  @Override
  public void create() {
    stats = entity.getComponent(CombatStatsComponent.class);
    animator = entity.getComponent(AnimationRenderComponent.class);
    targetStats = target == null ? null : target.getComponent(CombatStatsComponent.class);
    maxHealth = stats.getHealth();
    lastHealth = maxHealth;

    readArena();
    facingRight = target != null && target.getCenterPosition().x >= homeX;
    placeAt(homeX, floorY + HOVER_HEIGHT);

    entity.getEvents().addListener("updateHealth", this::onHealthChanged);
    entity.getEvents().addListener("death", this::onDeath);
    animate();
  }

  /**
   * Starts the fight over against a new player: full health, first phase, back on his spot, with
   * every attack in flight cleared. Does nothing once Zeus has been defeated.
   *
   * @param newTarget the player to fight from now on
   */
  public void reset(Entity newTarget) {
    if (action == Action.DYING) {
      return;
    }
    target = newTarget;
    targetStats = newTarget == null ? null : newTarget.getComponent(CombatStatsComponent.class);
    clearAttacks();
    phase = Phase.FLOOR;
    attackCount = 0;
    hurtTime = 0f;
    slamPending = false;
    lastHealth = maxHealth;
    stats.setHealth(maxHealth);
    setCastWindup(BOLT_WINDUP);
    placeAt(homeX, floorY + HOVER_HEIGHT);
    idle(OPENING_DELAY);
    animate();
  }

  @Override
  public void update() {
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    if (delta <= 0f) {
      return;
    }
    elapsed += delta;
    actionTime += delta;

    if (action == Action.DYING) {
      if (actionTime >= DEATH_TIME) {
        finishDeath();
      }
      return;
    }

    hurtTime = Math.max(0f, hurtTime - delta);
    updateAttacks(delta);

    if (targetStats == null || Boolean.TRUE.equals(targetStats.isDead())) {
      // Nobody to fight: finish nothing new, and wait where he is.
      if (action != Action.TRAVEL) {
        action = Action.IDLE;
        moving = false;
      }
    }

    switch (action) {
      case TRAVEL -> updateTravel(delta);
      case MELEE -> updateMelee();
      case BOLT -> updateBolt();
      case STRIKE -> updateStrike();
      case SHOCKWAVE -> updateShockwave();
      default -> updateIdle(delta);
    }
    animate();
  }

  // ---------------------------------------------------------------------------------------------
  // Phases and movement
  // ---------------------------------------------------------------------------------------------

  /**
   * @param healthFraction remaining health from 0 to 1
   * @param config the thresholds
   * @return the phase the fight is in at that health
   */
  static Phase phaseFor(float healthFraction, ZeusConfig config) {
    if (healthFraction <= config.enragedThreshold) {
      return Phase.ENRAGED;
    }
    return healthFraction <= config.throneThreshold ? Phase.THRONE : Phase.FLOOR;
  }

  private void updateIdle(float delta) {
    Phase wanted = phaseFor(getHealthFraction(), config);
    if (wanted.ordinal() > phase.ordinal()) {
      enterPhase(wanted);
      return;
    }

    boolean fighting = targetStats != null && !Boolean.TRUE.equals(targetStats.isDead());
    float centreX = centreX();
    float bob = BOB_HEIGHT * MathUtils.sin(elapsed * BOB_SPEED);
    moving = false;

    if (phase == Phase.THRONE) {
      placeAt(centreX, perchY + HOVER_HEIGHT + bob);
    } else {
      float x = centreX;
      if (fighting) {
        float gap = targetCentreX() - centreX;
        if (Math.abs(gap) > config.meleeRange * 0.6f) {
          float speed = config.moveSpeed * (phase == Phase.ENRAGED ? ENRAGED_SPEED_BOOST : 1f);
          x += Math.signum(gap) * Math.min(speed * delta, Math.abs(gap));
          moving = true;
        }
      }
      float halfWidth = entity.getScale().x / 2f;
      x = MathUtils.clamp(x, arenaLeft + halfWidth, Math.max(arenaLeft, arenaRight - halfWidth));
      placeAt(x, floorY + HOVER_HEIGHT + bob);
    }

    if (!fighting) {
      return;
    }
    facingRight = targetCentreX() >= centreX();
    cooldown -= delta;
    if (cooldown <= 0f) {
      chooseAttack();
    }
  }

  private void enterPhase(Phase next) {
    logger.info("Zeus enters phase {}", next);
    phase = next;
    if (next == Phase.THRONE) {
      travelTo(homeX, perchY + HOVER_HEIGHT, TRAVEL_SPEED);
    } else {
      // The slam: straight down to the floor, and a shockwave where he lands.
      setCastWindup(BOLT_WINDUP_ENRAGED);
      slamPending = true;
      travelTo(centreX(), floorY + HOVER_HEIGHT, SLAM_SPEED);
    }
  }

  private void travelTo(float centreX, float y, float speed) {
    travelTarget.set(centreX, y);
    travelSpeed = speed;
    begin(Action.TRAVEL);
  }

  private void updateTravel(float delta) {
    Vector2 here = new Vector2(centreX(), entity.getPosition().y);
    Vector2 step = travelTarget.cpy().sub(here);
    float distance = step.len();
    float reach = travelSpeed * delta;
    moving = true;

    if (distance > reach) {
      here.mulAdd(step.scl(1f / distance), reach);
      placeAt(here.x, here.y);
      return;
    }

    placeAt(travelTarget.x, travelTarget.y);
    moving = false;
    if (slamPending) {
      slamPending = false;
      begin(Action.SHOCKWAVE);
    } else {
      idle(attackGap());
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Attacks
  // ---------------------------------------------------------------------------------------------

  private void chooseAttack() {
    if (phase != Phase.THRONE && targetInReach(true)) {
      begin(Action.MELEE);
      return;
    }

    int turn = attackCount++;
    switch (phase) {
      case FLOOR -> begin(Action.BOLT);
      case THRONE -> begin(turn % 3 == 2 ? Action.BOLT : Action.STRIKE);
      default ->
          begin(turn % 3 == 0 ? Action.STRIKE : turn % 3 == 1 ? Action.BOLT : Action.SHOCKWAVE);
    }
    if (action == Action.STRIKE) {
      callStrike();
    }
  }

  private void updateMelee() {
    if (!released && actionTime >= MELEE_WINDUP) {
      released = true;
      if (targetInReach(false)) {
        hurtTarget(config.baseAttack);
        knockTargetBack();
      }
    }
    if (actionTime >= MELEE_DURATION) {
      idle(attackGap());
    }
  }

  private void updateBolt() {
    float windup = boltWindup();
    if (!released && actionTime >= windup) {
      released = true;
      throwBolt();
    }
    if (actionTime >= windup + castFrameDuration() * CAST_RECOVERY_FRAMES) {
      idle(attackGap());
    }
  }

  private void updateStrike() {
    if (actionTime >= STRIKE_CAST_TIME) {
      idle(attackGap());
    }
  }

  private void updateShockwave() {
    if (!released && actionTime >= SHOCKWAVE_WARNING) {
      released = true;
      float centreX = centreX();
      waves.add(new Wave(centreX, 1f));
      waves.add(new Wave(centreX, -1f));
    }
    if (actionTime >= SHOCKWAVE_WARNING + SHOCKWAVE_RECOVERY) {
      idle(attackGap());
    }
  }

  /** Throws a bolt: flat along the floor at a player level with him, otherwise straight at them. */
  private void throwBolt() {
    Vector2 centre = entity.getCenterPosition();
    float side = facingRight ? 1f : -1f;
    Rectangle box = targetBox();
    Vector2 aim = new Vector2(box.x + box.width / 2f, box.y + box.height / 2f);

    Vector2 origin = new Vector2(centre.x + side * 0.6f, floorY + BOLT_FLOOR_HEIGHT);
    Vector2 direction = new Vector2(side, 0f);
    if (phase == Phase.THRONE || Math.abs(aim.y - origin.y) > FLAT_SHOT_TOLERANCE) {
      origin.y = centre.y + 0.3f;
      direction.set(aim).sub(origin);
      if (direction.isZero(0.001f)) {
        direction.set(side, 0f);
      }
    }
    bolts.add(new Bolt(origin, direction.nor().scl(BOLT_SPEED)));
  }

  /** Marks the ground under the player; the strike lands there after its warning. */
  private void callStrike() {
    Rectangle box = targetBox();
    float x = box.x + box.width / 2f;
    float warning = phase == Phase.ENRAGED ? STRIKE_WARNING_ENRAGED : STRIKE_WARNING;
    strikes.add(new Strike(x, groundBelow(x, box.y), warning));
  }

  private void updateAttacks(float delta) {
    boolean fighting = targetStats != null && !Boolean.TRUE.equals(targetStats.isDead());
    Rectangle box = fighting ? targetBox() : null;

    for (Iterator<Bolt> it = bolts.iterator(); it.hasNext(); ) {
      Bolt bolt = it.next();
      bolt.age += delta;
      bolt.position.mulAdd(bolt.velocity, delta);
      hitBox.set(
          bolt.position.x - BOLT_HALF_WIDTH,
          bolt.position.y - BOLT_HALF_HEIGHT,
          BOLT_HALF_WIDTH * 2f,
          BOLT_HALF_HEIGHT * 2f);
      if (box != null && hitBox.overlaps(box)) {
        hurtTarget(config.boltDamage);
        it.remove();
      } else if (bolt.age >= BOLT_LIFETIME || isBlocked(bolt.position)) {
        it.remove();
      }
    }

    for (Iterator<Strike> it = strikes.iterator(); it.hasNext(); ) {
      Strike strike = it.next();
      strike.age += delta;
      float sinceLanding = strike.age - strike.warning;
      if (!strike.spent && sinceLanding >= 0f && sinceLanding <= STRIKE_DAMAGE_WINDOW) {
        hitBox.set(
            strike.x - STRIKE_HALF_WIDTH, strike.groundY, STRIKE_HALF_WIDTH * 2f, STRIKE_HEIGHT);
        if (box != null && hitBox.overlaps(box)) {
          strike.spent = true;
          hurtTarget(config.strikeDamage);
        }
      }
      if (sinceLanding >= STRIKE_DURATION) {
        it.remove();
      }
    }

    for (Iterator<Wave> it = waves.iterator(); it.hasNext(); ) {
      Wave wave = it.next();
      wave.x += wave.direction * SHOCKWAVE_SPEED * delta;
      hitBox.set(wave.x - SHOCKWAVE_WIDTH / 2f, floorY, SHOCKWAVE_WIDTH, SHOCKWAVE_HEIGHT);
      if (!wave.spent && box != null && hitBox.overlaps(box)) {
        wave.spent = true;
        hurtTarget(config.shockwaveDamage);
      }
      if (wave.x < arenaLeft || wave.x > arenaRight) {
        it.remove();
      }
    }
  }

  private void hurtTarget(int damage) {
    targetStats.hit(stats, Math.max(1, Math.round(damage * damageMultiplier)));
  }

  private void knockTargetBack() {
    PhysicsComponent physics = target.getComponent(PhysicsComponent.class);
    if (physics == null || config.meleeKnockback <= 0f) {
      return;
    }
    Body body = physics.getBody();
    Vector2 impulse = new Vector2(facingRight ? 1f : -1f, 0.4f).setLength(config.meleeKnockback);
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  /**
   * @param eitherSide true to look both ways, false for only the side Zeus faces
   * @return true if the player is close enough, and level enough, for his swing to land
   */
  private boolean targetInReach(boolean eitherSide) {
    float centreX = centreX();
    float behind = eitherSide ? config.meleeRange : 0.2f;
    float left = facingRight ? centreX - behind : centreX - config.meleeRange;
    hitBox.set(left, entity.getPosition().y, config.meleeRange + behind, entity.getScale().y);
    return hitBox.overlaps(targetBox());
  }

  // ---------------------------------------------------------------------------------------------
  // The arena
  // ---------------------------------------------------------------------------------------------

  /** Reads the floor, walls and throne balcony around Zeus's starting spot from the level. */
  private void readArena() {
    Vector2 centre = entity.getCenterPosition();
    homeX = centre.x;
    if (level == null) {
      floorY = entity.getPosition().y;
      perchY = floorY + FALLBACK_PERCH_HEIGHT;
      arenaLeft = homeX - FALLBACK_HALF_ARENA;
      arenaRight = homeX + FALLBACK_HALF_ARENA;
      return;
    }

    float tileSize = level.tileSize();
    int column = MathUtils.floor(centre.x / tileSize);
    int floorRow = Math.min(level.height() - 1, MathUtils.floor(centre.y / tileSize));
    while (floorRow > 0 && !level.isSolid(column, floorRow)) {
      floorRow--;
    }
    floorY = (floorRow + 1) * tileSize;

    int left = 0;
    while (left < level.width() && level.isSolid(left, floorRow + 1)) {
      left++;
    }
    int right = level.width() - 1;
    while (right > left && level.isSolid(right, floorRow + 1)) {
      right--;
    }
    arenaLeft = left * tileSize;
    arenaRight = (right + 1) * tileSize;

    // The highest platform over his spot, below the ceiling, is the throne balcony.
    perchY = floorY;
    for (int row = floorRow + 1; row < level.height() && !level.isSolid(column, row); row++) {
      if (level.isSupporting(column, row)) {
        perchY = (row + 1) * tileSize;
      }
    }
    if (perchY <= floorY) {
      perchY = floorY + FALLBACK_PERCH_HEIGHT;
    }
  }

  /**
   * @param x a world x
   * @param y a world y
   * @return the top of the nearest ground or platform at or below that point
   */
  float groundBelow(float x, float y) {
    if (level == null) {
      return floorY;
    }
    float tileSize = level.tileSize();
    int column = MathUtils.floor(x / tileSize);
    for (int row = MathUtils.floor((y - 0.1f) / tileSize); row >= 0; row--) {
      if (level.isSupporting(column, row)) {
        return (row + 1) * tileSize;
      }
    }
    return floorY;
  }

  /** A bolt stops at a wall, the floor or a stone block, but passes through thin platforms. */
  private boolean isBlocked(Vector2 point) {
    if (point.x < arenaLeft || point.x > arenaRight || point.y < floorY) {
      return true;
    }
    if (level == null) {
      return false;
    }
    float tileSize = level.tileSize();
    return level.isSolid(MathUtils.floor(point.x / tileSize), MathUtils.floor(point.y / tileSize));
  }

  // ---------------------------------------------------------------------------------------------
  // Health and death
  // ---------------------------------------------------------------------------------------------

  private void onHealthChanged(int health) {
    if (health < lastHealth) {
      hurtTime = HURT_TIME;
    }
    lastHealth = health;
  }

  private void onDeath() {
    if (action == Action.DYING) {
      return;
    }
    logger.info("Zeus is defeated");
    clearAttacks();
    moving = false;
    begin(Action.DYING);
    animate();
  }

  private void finishDeath() {
    if (defeated) {
      return;
    }
    defeated = true;
    Quest.incrementGlobalEnemiesKilled();
    entity.getEvents().trigger(DEFEATED_EVENT);
    // Never dispose inside the entity update loop; see EnemyDeathComponent.
    Gdx.app.postRunnable(entity::dispose);
  }

  private void clearAttacks() {
    bolts.clear();
    strikes.clear();
    waves.clear();
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private void begin(Action next) {
    action = next;
    actionTime = 0f;
    released = false;
  }

  private void idle(float pause) {
    begin(Action.IDLE);
    cooldown = pause;
  }

  private float attackGap() {
    float gap =
        switch (phase) {
          case FLOOR -> 1.4f;
          case THRONE -> 1.3f;
          default -> 0.8f;
        };
    return gap * cooldownMultiplier;
  }

  private float boltWindup() {
    return phase == Phase.ENRAGED ? BOLT_WINDUP_ENRAGED : BOLT_WINDUP;
  }

  private float castFrameDuration() {
    return boltWindup() / CAST_WINDUP_FRAMES;
  }

  /** Paces the cast animation so the bolt leaves his hand exactly as the wind-up ends. */
  private void setCastWindup(float windup) {
    if (animator != null) {
      animator.setFrameDuration("zeus-cast-right", windup / CAST_WINDUP_FRAMES);
      animator.setFrameDuration("zeus-cast-left", windup / CAST_WINDUP_FRAMES);
    }
  }

  private void placeAt(float centreX, float y) {
    entity.setPosition(centreX - entity.getScale().x / 2f, y);
  }

  private float centreX() {
    return entity.getPosition().x + entity.getScale().x / 2f;
  }

  private float targetCentreX() {
    return target.getPosition().x + target.getScale().x / 2f;
  }

  /** The player's body, a little narrower and shorter than their sprite. */
  private Rectangle targetBox() {
    Vector2 position = target.getPosition();
    Vector2 scale = target.getScale();
    return targetBox.set(position.x + scale.x * 0.25f, position.y, scale.x * 0.5f, scale.y * 0.9f);
  }

  private void animate() {
    if (animator == null) {
      return;
    }
    String name =
        switch (action) {
          case DYING -> "death";
          case MELEE, SHOCKWAVE -> "melee";
          case BOLT, STRIKE -> "cast";
          case TRAVEL -> hurtTime > 0f ? "hurt" : "float";
          default -> idleAnimation();
        };
    String animation = "zeus-" + name + (facingRight ? "-right" : "-left");
    if (!animation.equals(animator.getCurrentAnimation())) {
      animator.startAnimation(animation);
    }
  }

  private String idleAnimation() {
    if (hurtTime > 0f) {
      return "hurt";
    }
    if (phase == Phase.ENRAGED) {
      return "enraged-idle";
    }
    return moving ? "float" : "idle";
  }

  // ---------------------------------------------------------------------------------------------
  // State for the HUD, the effects renderer and tests
  // ---------------------------------------------------------------------------------------------

  /**
   * @return the phase of the fight
   */
  public Phase getPhase() {
    return phase;
  }

  /**
   * @return remaining health from 0 to 1
   */
  public float getHealthFraction() {
    return maxHealth <= 0 ? 0f : (float) stats.getHealth() / maxHealth;
  }

  /**
   * @return true while Zeus is winding up an attack that comes from where he stands, which is when
   *     a player who cannot see him needs pointing at him
   */
  public boolean isTelegraphing() {
    return (action == Action.BOLT || action == Action.SHOCKWAVE) && !released;
  }

  /**
   * @return how far along its warning the coming shockwave is, from 0 to 1, or -1 if none is coming
   */
  public float getShockwaveWarning() {
    return action == Action.SHOCKWAVE && !released ? actionTime / SHOCKWAVE_WARNING : -1f;
  }

  /**
   * @return the world y of the arena floor's surface
   */
  public float getFloorY() {
    return floorY;
  }

  /**
   * @return the world y of the throne balcony's surface
   */
  public float getPerchY() {
    return perchY;
  }

  /**
   * @return the world x of the arena's left wall face
   */
  public float getArenaLeft() {
    return arenaLeft;
  }

  /**
   * @return the world x of the arena's right wall face
   */
  public float getArenaRight() {
    return arenaRight;
  }

  /**
   * @return the bolts in flight (unmodifiable)
   */
  public List<Bolt> getBolts() {
    return Collections.unmodifiableList(bolts);
  }

  /**
   * @return the ground strikes warned of or landing (unmodifiable)
   */
  public List<Strike> getStrikes() {
    return Collections.unmodifiableList(strikes);
  }

  /**
   * @return the shockwaves crossing the floor (unmodifiable)
   */
  public List<Wave> getWaves() {
    return Collections.unmodifiableList(waves);
  }

  Action getAction() {
    return action;
  }

  /** Puts off his next attack, so a test can follow the one in flight to its end. */
  void holdAttacks() {
    cooldown = Math.max(cooldown, 1f);
  }

  /** A thrown bolt. */
  public static final class Bolt {
    final Vector2 position;
    final Vector2 velocity;
    float age;

    Bolt(Vector2 position, Vector2 velocity) {
      this.position = position;
      this.velocity = velocity;
    }

    /**
     * @return the bolt's centre in the world
     */
    public Vector2 getPosition() {
      return position;
    }

    /**
     * @return the bolt's velocity in world units per second
     */
    public Vector2 getVelocity() {
      return velocity;
    }

    /**
     * @return seconds the bolt has been flying
     */
    public float getAge() {
      return age;
    }
  }

  /** A ground strike: a warning on one spot, then a column of lightning. */
  public static final class Strike {
    final float x;
    final float groundY;
    final float warning;
    float age;
    boolean spent;

    Strike(float x, float groundY, float warning) {
      this.x = x;
      this.groundY = groundY;
      this.warning = warning;
    }

    /**
     * @return the world x of the strike's centre
     */
    public float getX() {
      return x;
    }

    /**
     * @return the world y of the surface the strike lands on
     */
    public float getGroundY() {
      return groundY;
    }

    /**
     * @return seconds of warning before the strike lands
     */
    public float getWarning() {
      return warning;
    }

    /**
     * @return seconds since the warning appeared
     */
    public float getAge() {
      return age;
    }
  }

  /** A shockwave running along the floor. */
  public static final class Wave {
    float x;
    final float direction;
    boolean spent;

    Wave(float x, float direction) {
      this.x = x;
      this.direction = direction;
    }

    /**
     * @return the world x of the wave's centre
     */
    public float getX() {
      return x;
    }

    /**
     * @return 1 for a wave running right, -1 for one running left
     */
    public float getDirection() {
      return direction;
    }
  }
}
