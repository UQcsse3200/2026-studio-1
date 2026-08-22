package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Stores gold and item slots. Can also be used as a more generic component for other entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);
  /** Default slot capacity when {@link #InventoryComponent(int)} is used. */
  private static final int DEFAULT_MAX_SLOTS = 5;

  private final int maxSlots;
  private int gold;

  public InventoryComponent(int gold) {
    this(gold, DEFAULT_MAX_SLOTS);
  }

  /**
   * Creates an inventory with an explicit slot capacity.
   *
   * @param gold starting gold; values below 0 are clamped to 0
   * @param maxSlots maximum number of item slots; must be {@code >= 1}
   * @throws IllegalArgumentException if {@code maxSlots < 1}
   */
  public InventoryComponent(int gold, int maxSlots) {
    if (maxSlots < 1) {
      throw new IllegalArgumentException("maxSlots must be >= 1");
    }
    this.maxSlots = maxSlots;
    setGold(gold);
  }

  /**
   * Returns the player's gold.
   *
   * @return entity's health
   */
  public int getGold() {
    return this.gold;
  }

  /**
   * Returns if the player has a certain amount of gold.
   *
   * @param gold required amount of gold
   * @return player has greater than or equal to the required amount of gold
   */
  public Boolean hasGold(int gold) {
    return this.gold >= gold;
  }

  /**
   * Sets the player's gold. Gold has a minimum bound of 0.
   *
   * @param gold gold
   */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);
  }

  /**
   * Adds to the player's gold. The amount added can be negative.
   *
   * @param gold gold to add
   */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }
}
