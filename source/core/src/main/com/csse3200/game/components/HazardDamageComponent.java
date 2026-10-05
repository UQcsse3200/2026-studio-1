package com.csse3200.game.components;

/**
 * How much health a single hazard tile takes off the player on contact.
 *
 * <p>Attached to the collider entity spawned for a hazard tile, so damage is a property of the tile
 * rather than one constant shared by every hazard in the game. The value comes from the {@code
 * damage} property on the tile's legend entry, falling back to the level's default when the map
 * does not set one.
 *
 * <pre>{@code
 * "~": { "type": "HAZARD", "texture": "river-styx.png",   "damage": "15" },
 * "L": { "type": "HAZARD", "texture": "hazard-firepit.png", "damage": "25" }
 * }</pre>
 */
public class HazardDamageComponent extends Component {
  private final int damage;

  /**
   * @param damage health removed per hit, clamped to zero or more
   */
  public HazardDamageComponent(int damage) {
    this.damage = Math.max(0, damage);
  }

  /**
   * @return health removed per hit
   */
  public int getDamage() {
    return damage;
  }
}
