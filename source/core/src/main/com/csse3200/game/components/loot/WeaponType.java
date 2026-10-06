package com.csse3200.game.components.loot;

// Represents the different types of weapons available in the game.

/** The set of weapon stypes avaliable in the game. */
public enum WeaponType {
  SWORD,
  BOW,
  DAGGER,
  AXE,
  /**
   * Tags a {@link WeaponItem} built via {@link WeaponItem#natural} for an enemy with no carried
   * weapon (e.g. a Cyclops's fists). Deliberately has no {@link WeaponTier} stats - damage and
   * windup for a NATURAL weapon are whatever the caller passed in directly, not looked up from a
   * tier table.
   */
  NATURAL
}
