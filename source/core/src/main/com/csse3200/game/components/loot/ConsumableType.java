package com.csse3200.game.components.loot;

/**
 * The different consumable items that can be generated as loot.
 *
 * <p>Each type carries the texture used to represent it, so inventory and world rendering can look
 * the sprite up from the item itself rather than hardcoding paths at every call site. Every potion
 * has its own sprite; sharing one sprite between two potions made them indistinguishable on the
 * ground.
 */
public enum ConsumableType {
  /** Restores health immediately, capped at the player's maximum health. */
  HEALTH_POTION("images/potions/health_potion.png"),
  /** Temporarily multiplies the player's attack damage. */
  DAMAGE_BUFF("images/potions/strength_potion.png"),
  /** Temporarily multiplies the player's movement speed. */
  SPEED_BUFF("images/potions/speed_potion.png"),
  /** Restores health gradually over its duration. */
  REGENERATION("images/potions/regeneration_potion.png"),
  /** Temporarily reduces the damage the player takes. */
  RESISTANCE("images/potions/resistance_potion.png");

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
