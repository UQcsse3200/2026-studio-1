package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetManagerComponentTest {
  private Entity owner;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    owner = new Entity();
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.clear();
  }

  @Test
  void shouldStartWithoutActivePet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    assertNull(manager.getActivePet());
    assertFalse(manager.hasActivePet());
  }

  @Test
  void shouldActivatePet() {
    Entity pet = new Entity();

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> pet);

    owner.addComponent(manager);

    manager.activatePet(new ShopComponent.Pet("Bird"));

    assertSame(pet, manager.getActivePet());
    assertTrue(manager.hasActivePet());
  }

  @Test
  void shouldReplaceExistingPet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    manager.activatePet(new ShopComponent.Pet("Bird"));
    Entity firstPet = manager.getActivePet();

    manager.activatePet(new ShopComponent.Pet("Bat"));
    Entity secondPet = manager.getActivePet();

    assertNotSame(firstPet, secondPet);
    assertSame(secondPet, manager.getActivePet());
    assertTrue(manager.hasActivePet());
  }

  @Test
  void shouldRemoveActivePet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    manager.activatePet(new ShopComponent.Pet("Bird"));
    manager.removePet();

    assertNull(manager.getActivePet());
    assertFalse(manager.hasActivePet());
  }

  @Test
  void shouldDisposeActivePet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    manager.activatePet(new ShopComponent.Pet("Bird"));
    manager.dispose();

    assertNull(manager.getActivePet());
    assertFalse(manager.hasActivePet());
  }

  @Test
  void shouldActivatePetWhenPetPurchasedEventTriggered() {
    Entity pet = new Entity();

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> pet);

    owner.addComponent(manager);
    ServiceLocator.getEntityService().register(owner);

    owner.getEvents().trigger("petPurchased", new ShopComponent.Pet("Bird"));

    assertSame(pet, manager.getActivePet());
    assertTrue(manager.hasActivePet());
  }
}
