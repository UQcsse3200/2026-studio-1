package com.csse3200.game.components.pet;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PetFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.BiFunction;

/**
 * Manages the player's active companion pet.
 *
 * <p>A player can have at most one active pet at a time. The manager listens for successful pet
 * purchases and activates a pet when the {@code petPurchased} event is triggered.
 */
public class PetManagerComponent extends Component {
  private Entity activePet;
  private final BiFunction<Entity, ShopComponent.Pet, Entity> petFactory;

  /** Creates a pet manager using the normal game pet factory. */
  public PetManagerComponent() {
    this(PetFactory::createPet);
  }

  /**
   * Creates a pet manager using the supplied pet factory.
   *
   * <p>This constructor is package-private so tests can provide a lightweight pet factory without
   * loading game assets.
   */
  PetManagerComponent(BiFunction<Entity, ShopComponent.Pet, Entity> petFactory) {
    this.petFactory = petFactory;
  }

  /**
   * Registers the pet manager to listen for successful pet purchases.
   *
   * <p>When the player successfully purchases a pet from the shop, the {@code petPurchased} event
   * is triggered and a companion pet is activated.
   */
  @Override
  public void create() {
    entity.getEvents().addListener("petPurchased", this::activatePet);
  }

  /**
   * Creates and activates a pet for this player.
   *
   * <p>If a pet is already active, it is removed before the new pet is created.
   */
  public void activatePet(ShopComponent.Pet pet) {
    removePet();

    activePet = petFactory.apply(entity, pet);
    ServiceLocator.getEntityService().register(activePet);
  }

  /** Returns the currently active pet, or null if there is no active pet. */
  public Entity getActivePet() {
    return activePet;
  }

  /** Returns whether this player currently has an active pet. */
  public boolean hasActivePet() {
    return activePet != null;
  }

  /** Removes the currently active pet. */
  public void removePet() {
    if (activePet == null) {
      return;
    }

    activePet.dispose();
    activePet = null;
  }

  /** Removes the active pet when this component is disposed. */
  @Override
  public void dispose() {
    removePet();
  }
}
