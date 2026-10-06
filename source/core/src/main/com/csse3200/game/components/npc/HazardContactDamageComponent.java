package com.csse3200.game.components.npc;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.HazardDamageComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashSet;
import java.util.Set;

/**
 * Lets an entity take damage from hazard tiles it touches: spikes, rivers and fire alike. It is the
 * enemy counterpart of the inline listener {@code LevelGameArea} adds to the player.
 *
 * <p><b>Rules (each one is tested):</b>
 *
 * <ul>
 *   <li><b>Same numbers as the player.</b> Damage comes from the hazard's {@code
 *       HazardDamageComponent}; a hazard without one uses {@link #DEFAULT_HAZARD_DAMAGE}. The
 *       cooldown is {@link #DEFAULT_COOLDOWN_MILLIS} unless a different one is given.
 *   <li><b>First contact hurts at once, then once per cooldown while the entity stays inside.</b>
 *       Leaving and re-entering does not reset the cooldown.
 *   <li><b>No stacking.</b> If several hazard tiles are touched at once, the largest damage is
 *       applied once per window.
 *   <li><b>Only the hazard layer counts.</b> Any other sensor or collider is ignored.
 *   <li><b>Time comes from the game's time source</b>, not the system clock, so it pauses with the
 *       game and can be controlled in tests.
 * </ul>
 *
 * <p><b>Limitations:</b> it does not stop an enemy walking into a hazard (see {@link
 * HazardAvoidanceComponent}); a hazard kill gives no loot attribution; it needs a {@code
 * CombatStatsComponent} on the same entity and quietly does nothing without one.
 *
 * <p><b>Style reference:</b> {@code LightningFreezeComponent} (a contact driven effect) and the
 * hazard listener in {@code LevelGameArea}.
 */
public class HazardContactDamageComponent extends Component {

  /** Same cooldown the player has between hazard hits, in milliseconds. */
  public static final long DEFAULT_COOLDOWN_MILLIS = 500;

  /** Damage used for a hazard that carries no damage of its own. Matches the level default. */
  public static final int DEFAULT_HAZARD_DAMAGE = 10;

  private final long cooldownMillis;
  private final Set<Entity> touchedHazards;
  private long lastHitMillis;

  /** Creates the component with {@link #DEFAULT_COOLDOWN_MILLIS}. */
  public HazardContactDamageComponent() {
    this(DEFAULT_COOLDOWN_MILLIS);
  }

  /**
   * Creates the component with a chosen cooldown.
   *
   * @param cooldownMillis milliseconds between hits while touching a hazard; greater than zero
   * @throws IllegalArgumentException if the cooldown is zero or negative
   */
  public HazardContactDamageComponent(long cooldownMillis) throws IllegalArgumentException {
    if (cooldownMillis <= 0) {
      throw new IllegalArgumentException("cooldownMillis must be greater than zero");
    }
    this.cooldownMillis = cooldownMillis;
    this.touchedHazards = new HashSet<>();
    // Far enough back that now - lastHitMillis is always >= cooldown on first contact,
    // without risking overflow in the subtraction.
    this.lastHitMillis = Long.MIN_VALUE / 2;
  }

  /** Starts listening for the start and end of contacts on this entity. */
  @Override
  public void create() {
    entity.getEvents().addListener("collisionStart", this::onContactStart);
    entity.getEvents().addListener("collisionEnd", this::onContactEnd);
  }

  /**
   * Remembers a hazard when contact with it starts, and hurts at once if the cooldown allows.
   * Contacts that are not on the hazard layer are ignored.
   */
  private void onContactStart(Fixture thisFixture, Fixture otherFixture) {
    if (!isHazardFixture(otherFixture)) {
      return;
    }
    Entity hazard = ownerOf(otherFixture);
    if (hazard == null) {
      return;
    }
    touchedHazards.add(hazard);
    tryDamage();
  }

  /** Forgets a hazard when contact with it ends. */
  private void onContactEnd(Fixture thisFixture, Fixture otherFixture) {
    if (!isHazardFixture(otherFixture)) {
      return;
    }
    Entity hazard = ownerOf(otherFixture);
    if (hazard != null) {
      touchedHazards.remove(hazard);
    }
  }

  /** Hurts again, once per cooldown, for as long as a hazard is still touched. */
  @Override
  public void update() {
    if (touchedHazards.isEmpty()) {
      return;
    }
    tryDamage();
  }

  /**
   * Applies one hit if the cooldown has passed, the entity has stats and is still alive. The damage
   * is the largest among the hazards touched.
   */
  private void tryDamage() {
    long now = ServiceLocator.getTimeSource().getTime();
    if (now - lastHitMillis < cooldownMillis) {
      return;
    }

    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats == null || stats.isDead()) {
      return;
    }

    int damage = 0;
    for (Entity hazard : touchedHazards) {
      HazardDamageComponent hazardDamage = hazard.getComponent(HazardDamageComponent.class);
      int value = hazardDamage != null ? hazardDamage.getDamage() : DEFAULT_HAZARD_DAMAGE;
      damage = Math.max(damage, value);
    }

    stats.setHealth(stats.getHealth() - damage);
    lastHitMillis = now;
    entity.getEvents().trigger("hazardDamage", damage);
  }

  /**
   * @return true while at least one hazard tile is touching this entity
   */
  public boolean isTouchingHazard() {
    return !touchedHazards.isEmpty();
  }

  /**
   * @return the cooldown between hits in milliseconds
   */
  public long getCooldownMillis() {
    return cooldownMillis;
  }

  /** True if the fixture belongs to the hazard physics layer. */
  private static boolean isHazardFixture(Fixture fixture) {
    return (fixture != null
        && PhysicsLayer.contains(PhysicsLayer.HAZARD, fixture.getFilterData().categoryBits));
  }

  /** Finds the entity that owns the fixture's body through the body's user data, or null. */
  private static Entity ownerOf(Fixture fixture) {
    Object userData = fixture.getBody().getUserData();
    if (userData instanceof BodyUserData) {
      return ((BodyUserData) userData).entity;
    }
    return null;
  }
}
