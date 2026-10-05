package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.player.InventoryComponent;
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
    assertNull(manager.getActivePetType());
    assertFalse(manager.hasActivePet());
  }

  @Test
  void shouldActivatePet() {
    Entity pet = new Entity();
    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> pet);

    owner.addComponent(manager);

    manager.activatePet(bird);

    assertSame(pet, manager.getActivePet());
    assertSame(bird, manager.getActivePetType());
    assertTrue(manager.hasActivePet());
  }

  @Test
  void shouldReplaceExistingPet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    ShopComponent.Pet bat = new ShopComponent.Pet("Bat");

    manager.activatePet(bird);
    Entity firstPet = manager.getActivePet();

    manager.activatePet(bat);
    Entity secondPet = manager.getActivePet();

    assertNotSame(firstPet, secondPet);
    assertSame(secondPet, manager.getActivePet());
    assertSame(bat, manager.getActivePetType());
    assertTrue(manager.hasActivePet());
  }

  @Test
  void shouldRemoveActivePet() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    manager.activatePet(new ShopComponent.Pet("Bird"));
    manager.removePet();

    assertNull(manager.getActivePet());
    assertNull(manager.getActivePetType());
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

  @Test
  void shouldNotReplaceActivePetWhenAnotherPetPurchased() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);
    ServiceLocator.getEntityService().register(owner);

    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    ShopComponent.Pet bat = new ShopComponent.Pet("Bat");

    owner.getEvents().trigger("petPurchased", bird);

    Entity birdEntity = manager.getActivePet();

    owner.getEvents().trigger("petPurchased", bat);

    assertSame(birdEntity, manager.getActivePet());
    assertSame(bird, manager.getActivePetType());
    assertTrue(manager.hasActivePet());
  }

  @Test
  void shouldSwitchToOwnedPet() {
    InventoryComponent inventory = new InventoryComponent(100);
    owner.addComponent(inventory);

    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    ShopComponent.Pet bat = new ShopComponent.Pet("Bat");

    inventory.addPet(bird);
    inventory.addPet(bat);

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);

    manager.activatePet(bird);
    Entity birdEntity = manager.getActivePet();

    assertTrue(manager.switchActivePet(bat));

    assertNotSame(birdEntity, manager.getActivePet());
    assertSame(bat, manager.getActivePetType());

    // Switching does not remove pets from inventory.
    assertSame(bird, inventory.getPet(1));
    assertSame(bat, inventory.getPet(2));
  }

  @Test
  void shouldNotSwitchToUnownedPet() {
    InventoryComponent inventory = new InventoryComponent(100);
    owner.addComponent(inventory);

    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    ShopComponent.Pet bat = new ShopComponent.Pet("Bat");

    inventory.addPet(bird);

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);
    manager.activatePet(bird);

    Entity birdEntity = manager.getActivePet();

    assertFalse(manager.switchActivePet(bat));

    assertSame(birdEntity, manager.getActivePet());
    assertSame(bird, manager.getActivePetType());
  }

  @Test
  void shouldNotSwitchToAlreadyActivePet() {
    InventoryComponent inventory = new InventoryComponent(100);
    owner.addComponent(inventory);

    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    inventory.addPet(bird);

    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());

    owner.addComponent(manager);
    manager.activatePet(bird);

    Entity birdEntity = manager.getActivePet();

    assertFalse(manager.switchActivePet(bird));

    assertSame(birdEntity, manager.getActivePet());
    assertSame(bird, manager.getActivePetType());
  }
}
