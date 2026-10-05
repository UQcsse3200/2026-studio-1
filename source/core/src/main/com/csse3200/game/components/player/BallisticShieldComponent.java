package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tier 2 defensive shield.
 *
 * <p>The Ballistic Shield blocks all incoming damage while active and retaliates by dealing 25% of
 * the blocked damage to the attacker. It lasts 30 seconds once activated.
 *
 * <p>The Ballistic Shield is obtained as rare loot.
 */
public class BallisticShieldComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(BallisticShieldComponent.class);

  private static final long DEFAULT_DURATION_MILLIS = 30000L;
  private static final float RETALIATION_RATIO = 0.25f;

  private final long durationMillis;

  private GameTime timeSource;

  private boolean hasShield = false;
  private boolean active = false;
  private long activeUntil = 0L;

  public BallisticShieldComponent() {
    this(DEFAULT_DURATION_MILLIS);
  }

  public BallisticShieldComponent(long durationMillis) {
    if (durationMillis <= 0) {
      throw new IllegalArgumentException("durationMillis must be greater than 0.");
    }

    this.durationMillis = durationMillis;
  }

  @Override
  public void create() {
    timeSource = ServiceLocator.getTimeSource();

    entity.getEvents().addListener("activateBallisticShield", this::activateBallisticShield);
  }

  @Override
  public void update() {
    if (active && timeSource != null && timeSource.getTime() >= activeUntil) {
      deactivate();
    }
  }

  /** Gives the player a Ballistic Shield. */
  public void grantShield() {
    hasShield = true;

    logger.info("Ballistic Shield granted.");

    entity.getEvents().trigger("ballisticShieldGranted");
  }

  /**
   * Activates the held Ballistic Shield.
   *
   * @return true if activation was successful
   */
  public boolean activateBallisticShield() {
    if (!hasShield) {
      logger.debug("No Ballistic Shield held.");
      return false;
    }

    if (active) {
      logger.debug("Ballistic Shield already active.");
      return false;
    }

    hasShield = false;
    active = true;

    activeUntil = timeSource != null ? timeSource.getTime() + durationMillis : 0L;

    logger.info("Ballistic Shield activated for {}ms.", durationMillis);

    entity.getEvents().trigger("ballisticShieldActivated", durationMillis);

    return true;
  }

  /** Returns whether the player currently has an unused Ballistic Shield. */
  public boolean hasShield() {
    return hasShield;
  }

  /**
   * Removes the held Ballistic Shield without activating it.
   *
   * @return true if a held shield was removed
   */
  public boolean consumeHeldShield() {
    if (!hasShield) {
      return false;
    }

    hasShield = false;
    return true;
  }

  /** Returns whether the Ballistic Shield is currently active. */
  public boolean isActive() {
    return active;
  }

  /**
   * Blocks incoming damage and retaliates against the attacker.
   *
   * <p>The damage is completely prevented from reaching the player. The attacker receives 25% of
   * the blocked damage (minimum 1).
   *
   * @param attacker the entity that caused the damage
   * @param incomingDamage the incoming damage before blocking
   */
  public void blockDamage(CombatStatsComponent attacker, int incomingDamage) {
    if (!active || incomingDamage <= 0) {
      return;
    }

    int retaliationDamage = Math.max(1, Math.round(incomingDamage * RETALIATION_RATIO));

    if (attacker != null && !attacker.isDead()) {
      /*
       * Route the retaliation through hit() rather than addHealth() so it behaves like a real
       * player attack: the enemy is notified via "damagedBy", and if it dies the kill triggers the
       * usual enemyKilled events (loot, score) credited to the player.
       */
      CombatStatsComponent playerStats = entity.getComponent(CombatStatsComponent.class);
      if (playerStats != null) {
        attacker.hit(playerStats, retaliationDamage);
      } else {
        attacker.addHealth(-retaliationDamage);
      }

      logger.info(
          "Ballistic Shield blocked {} damage and retaliated for {} damage.",
          incomingDamage,
          retaliationDamage);

      entity
          .getEvents()
          .trigger(
              "ballisticShieldBlocked", incomingDamage, retaliationDamage, attacker.getEntity());
    } else {
      logger.debug("Ballistic Shield blocked {} damage but had no valid attacker.", incomingDamage);
    }
  }

  private void deactivate() {
    active = false;
    activeUntil = 0L;

    logger.info("Ballistic Shield deactivated.");

    entity.getEvents().trigger("ballisticShieldDeactivated");
  }
}
