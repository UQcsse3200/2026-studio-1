package com.csse3200.game.components.player;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the player to activate a picked-up Shield to temporarily block incoming damage.
 *
 * <p>The shield does not use an inventory slot. Picking it up (see {@code LootPickupComponent},
 * which special-cases {@code ItemType.SHIELD} the same way it already does for currency) calls
 * {@link #grantShield()} directly, so it can always be collected even when the player's inventory
 * is full. Pressing the dedicated shield key (see {@link KeyboardPlayerInputComponent}) fires
 * {@code "activateShield"}, which this component listens for.
 *
 * <p>While active, any decrease in health reported through {@code CombatStatsComponent}'s {@code
 * "updateHealth"} event is fully reverted, so damage taken while shielded has no effect. This is
 * implemented by observing and reverting health changes rather than modifying {@code
 * CombatStatsComponent} or the attack components, since those are shared with other teams.
 */
public class ShieldComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ShieldComponent.class);
  private static final long DEFAULT_DURATION_MILLIS = 30000L;

  private final long durationMillis;
  private GameTime timeSource;
  private CombatStatsComponent combatStats;

  private boolean hasShield = false;
  private boolean active = false;
  private boolean pendingRevert = false;
  private long activeUntil;
  private int lastKnownHealth;

  /**
   * Creates a shield that blocks damage for {@value #DEFAULT_DURATION_MILLIS}ms (30 seconds) once
   * activated, after which it automatically deactivates and disappears.
   */
  public ShieldComponent() {
    this(DEFAULT_DURATION_MILLIS);
  }

  /**
   * Creates a shield with an explicit active duration.
   *
   * @param durationMillis how long the shield blocks damage once activated; must be {@code > 0}
   * @throws IllegalArgumentException if {@code durationMillis} is not positive
   */
  public ShieldComponent(long durationMillis) {
    if (durationMillis <= 0) {
      throw new IllegalArgumentException("durationMillis must be greater than 0.");
    }
    this.durationMillis = durationMillis;
  }

  /**
   * Registers the {@code "activateShield"} listener and, if present, tracks health for blocking.
   */
  @Override
  public void create() {
    timeSource = ServiceLocator.getTimeSource();
    combatStats = entity.getComponent(CombatStatsComponent.class);

    if (combatStats != null) {
      lastKnownHealth = combatStats.getHealth();
      entity.getEvents().addListener("updateHealth", this::onHealthChanged);
    }

    entity.getEvents().addListener("activateShield", this::activateShield);
  }

  /**
   * Applies any pending damage reversal, then deactivates the shield once its duration has elapsed.
   *
   * <p>The reversal is applied here, outside of any event dispatch, rather than directly inside
   * {@link #onHealthChanged}. Calling {@code CombatStatsComponent.setHealth} synchronously from
   * within its own {@code "updateHealth"} listener re-enters that event's dispatch while it is
   * still iterating its listener list, which this engine's event system does not support and throws
   * {@code GdxRuntimeException: #iterator() cannot be used nested.}
   */
  @Override
  public void update() {
    if (pendingRevert) {
      pendingRevert = false;
      combatStats.setHealth(lastKnownHealth);
    }

    if (active && timeSource != null && timeSource.getTime() >= activeUntil) {
      deactivate();
    }
  }

  /**
   * Grants the player a shield to activate later. Called on pickup, bypassing the inventory slot
   * system entirely (the same way currency does), so a full inventory never blocks collecting it.
   */
  public void grantShield() {
    hasShield = true;
    Quest.incrementGlobalShieldsCollected();
    logger.info("Shield granted, press the shield key to activate");
    entity.getEvents().trigger("shieldGranted");
  }

  /**
   * Activates the shield if the player currently holds one and it isn't already active.
   *
   * @return {@code true} if a held shield was activated
   */
  public boolean activateShield() {
    if (!hasShield) {
      logger.debug("No shield held, ignoring activation");
      return false;
    }
    if (active) {
      logger.debug("Shield already active, ignoring activation");
      return false;
    }

    hasShield = false;
    activate();
    return true;
  }

  /**
   * Returns whether the player currently holds an unactivated shield.
   *
   * @return {@code true} if a shield is held
   */
  public boolean hasShield() {
    return hasShield;
  }

  /**
   * Returns whether the shield is currently blocking damage.
   *
   * @return {@code true} if active
   */
  public boolean isActive() {
    return active;
  }

  private void activate() {
    active = true;
    activeUntil = timeSource != null ? timeSource.getTime() + durationMillis : 0L;
    logger.info("Shield activated for {}ms", durationMillis);
    entity.getEvents().trigger("shieldActivated", durationMillis);
  }

  private void deactivate() {
    active = false;
    logger.info("Shield deactivated");
    entity.getEvents().trigger("shieldDeactivated");
  }

  private void onHealthChanged(int newHealth) {
    if (active && newHealth < lastKnownHealth) {
      int blocked = lastKnownHealth - newHealth;
      logger.debug("Shield blocked {} damage, reverting next update", blocked);
      pendingRevert = true;
      entity.getEvents().trigger("shieldBlocked", blocked);
      return;
    }
    lastKnownHealth = newHealth;
  }
}
