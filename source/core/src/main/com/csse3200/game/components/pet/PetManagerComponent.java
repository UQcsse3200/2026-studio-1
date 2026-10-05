package com.csse3200.game.components.pet;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PetFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.BiFunction;

/**
 * Manages the player's active companion pet.
 *
 * <p>A player can have at most one active pet at a time. The manager listens for successful pet
 * purchases and activates the first purchased pet. Additional pets remain in the inventory until
 * selected. Confirmed player hits are forwarded to the currently active pet for combat assistance.
 */
public class PetManagerComponent extends Component {
  private Entity activePet;
  private ShopComponent.Pet activePetType;
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
   * Registers listeners for pet purchases, gambling replacements, and confirmed player hits.
   *
   * <p>The {@code petPurchased} event activates a companion only when no pet is currently active.
   */
  @Override
  public void create() {
    entity.getEvents().addListener("petPurchased", this::handlePetPurchased);
    entity.getEvents().addListener("gamblingPetReplaced", this::handleGamblingPetReplaced);
    entity.getEvents().addListener("playerAttackHit", this::onPlayerAttackHit);
  }

  private void onPlayerAttackHit(Entity target) {
    if (!enabled || entity.isDisposed() || activePet == null || activePet.isDisposed()) {
      return;
    }
    // Listen on the owner once; replacing a pet must not leave old pets subscribed to hits.
    PetCombatComponent combat = activePet.getComponent(PetCombatComponent.class);
    if (combat != null) {
      combat.requestAttack(target);
    }
  }

  /**
   * Creates and activates a pet for this player.
   *
   * <p>If a pet is already active, it is removed before the new pet is created.
   */
  public void activatePet(ShopComponent.Pet pet) {
    removePet();

    activePet = petFactory.apply(entity, pet);
    activePetType = pet;

    ServiceLocator.getEntityService().register(activePet);

    entity.getEvents().trigger("activePetChanged", pet);
  }

  /** Returns the currently active pet, or null if there is no active pet. */
  public Entity getActivePet() {
    return activePet;
  }

  /** Returns whether this player currently has an active pet. */
  public boolean hasActivePet() {
    return activePet != null;
  }

  /**
   * Returns the data for the currently active pet.
   *
   * @return active pet data, or null if no pet is active
   */
  public ShopComponent.Pet getActivePetType() {
    return activePetType;
  }

  /** Removes the currently active pet. */
  public void removePet() {
    if (activePet == null) {
      activePetType = null;
      return;
    }

    activePet.dispose();
    activePet = null;
    activePetType = null;
  }

  /** Removes the active pet when this component is disposed. */
  @Override
  public void dispose() {
    removePet();
  }

  /**
   * Activates the purchased pet if the player does not already have an active pet.
   *
   * <p>Additional purchased pets remain in the pet inventory until explicitly selected.
   */
  private void handlePetPurchased(ShopComponent.Pet pet) {
    if (!hasActivePet()) {
      activatePet(pet);
    }
  }

  /**
   * Switches the active companion to an owned pet.
   *
   * @param pet pet to activate
   * @return true if the active pet was switched
   */
  public boolean switchActivePet(ShopComponent.Pet pet) {
    if (pet == null) {
      return false;
    }

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null || !inventory.containsPet(pet)) {
      return false;
    }

    if (activePetType != null && activePetType.getName().equals(pet.getName())) {
      return false;
    }

    activatePet(pet);
    return true;
  }

  private void handleGamblingPetReplaced(ShopComponent.Pet replacedPet, ShopComponent.Pet newPet) {

    if (replacedPet == null || newPet == null || activePetType == null) {
      return;
    }

    if (activePetType.getName().equals(replacedPet.getName())) {
      activatePet(newPet);
    }
  }
}
