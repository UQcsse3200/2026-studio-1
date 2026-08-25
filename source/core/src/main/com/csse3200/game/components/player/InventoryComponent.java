package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Currently stores the player's gold and token amounts but can be extended for more advanced
 * functionality such as storing items. Can also be used as a more generic component for other
 * entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);

  private int gold;
  private int tokens;

  /**
   * Creates an inventory with the specified amount of gold and no tokens.
   *
   * @param gold starting amount of gold
   */
  public InventoryComponent(int gold) {
    this(gold, 0);
  }

  /**
   * Creates an inventory with the specified amount of gold and tokens.
   *
   * @param gold starting amount of gold
   * @param tokens starting amount of tokens
   */
  public InventoryComponent(int gold, int tokens) {
    setGold(gold);
    setTokens(tokens);
  }

  /**
   * Returns the player's gold.
   *
   * @return player's gold
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
  public boolean hasGold(int gold) {
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

  /**
   * Returns the player's tokens.
   *
   * @return player's tokens
   */
  public int getTokens() {
    return this.tokens;
  }

  /**
   * Returns if the player has a certain amount of tokens.
   *
   * @param tokens required amount of tokens
   * @return player has greater than or equal to the required amount of tokens
   */
  public boolean hasTokens(int tokens) {
    return this.tokens >= tokens;
  }

  /**
   * Sets the player's tokens. Tokens have a minimum bound of 0.
   *
   * @param tokens tokens
   */
  public void setTokens(int tokens) {
    this.tokens = Math.max(tokens, 0);
    logger.debug("Setting tokens to {}", this.tokens);
  }

  /**
   * Adds to the player's tokens. The amount added can be negative.
   *
   * @param tokens tokens to add
   */
  public void addTokens(int tokens) {
    setTokens(this.tokens + tokens);
  }
}
