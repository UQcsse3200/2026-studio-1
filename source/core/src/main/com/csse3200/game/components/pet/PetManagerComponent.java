package com.csse3200.game.components.pet;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PetFactory;
import com.csse3200.game.services.ServiceLocator;

/**
 * Manages the player's active companion pet.
 *
 * <p>A player can have at most one active pet at a time.
 */
public class PetManagerComponent extends Component {
  private Entity activePet;

  /**
   * Creates and activates a pet for this player.
   *
   * <p>If a pet is already active, it is removed before the new pet is created.
   */
  public void activatePet() {
    removePet();

    activePet = PetFactory.createPet(entity);

    activePet.setPosition(entity.getPosition().x + 1f, entity.getPosition().y);

    ServiceLocator.getEntityService().register(activePet);
  }

  /** Returns the currently active pet, or null if there is no active pet. */
  public Entity getActivePet() {
    return activePet;
  }

  /** Removes the currently active pet. */
  public void removePet() {
    if (activePet == null) {
      return;
    }

    activePet.dispose();
    activePet = null;
  }
}
