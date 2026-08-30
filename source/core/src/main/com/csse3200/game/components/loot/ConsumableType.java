package com.csse3200.game.components.loot;

/**
 * The different consumable items that can be generated as loot.
 *
 * <p>Each type carries the texture used to represent it, so inventory and world rendering can look
 * the sprite up from the item itself rather than hardcoding paths at every call site.
 */
public enum ConsumableType {
  /** Restores health, capped at the player's maximum health. */
  HEALTH_POTION("images/Health.png"),
  /** Temporarily multiplies the player's attack damage. */
  DAMAGE_BUFF("images/Poison.png"),
  /** Temporarily multiplies the player's movement speed. */
  SPEED_BUFF("images/Strength.png");

  private final String texturePath;

  ConsumableType(String texturePath) {
    this.texturePath = texturePath;
  }

  /**
   * Returns the texture representing this consumable.
   *
   * @return internal asset path of the sprite
   */
  public String getTexturePath() {
    return texturePath;
  }
}
