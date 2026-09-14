package com.csse3200.game.components.pet;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/**
 * Core component for a companion pet.
 *
 * <p>This component stores the relationship between a pet and its owning player. Pet-specific
 * movement and combat behaviour should be implemented in separate components.
 */
public class PetComponent extends Component {
  private final Entity owner;

  /**
   * Creates a pet component owned by the given player.
   *
   * @param owner the player entity that owns this pet
   * @throws IllegalArgumentException if owner is null
   */
  public PetComponent(Entity owner) {
    if (owner == null) {
      throw new IllegalArgumentException("Pet owner cannot be null");
    }

    this.owner = owner;
  }

  /**
   * Gets the player that owns this pet.
   *
   * @return the owning player entity
   */
  public Entity getOwner() {
    return owner;
  }
}
