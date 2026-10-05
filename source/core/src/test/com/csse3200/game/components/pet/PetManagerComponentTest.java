package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
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
    ServiceLocator.getEntityService().dispose();
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
  void shouldForwardPlayerHitToActivePetOnItsNextUpdate() {
    PetManagerComponent manager = createCombatManager();
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    manager.activatePet(new ShopComponent.Pet("Bird"));
    Entity pet = manager.getActivePet();
    List<Entity> attacks = new ArrayList<>();
    EventListener1<Entity> attackListener = attacks::add;
    pet.getEvents().addListener("petAttack", attackListener);

    owner.getEvents().trigger("playerAttackHit", target);
    assertTrue(attacks.isEmpty());
    pet.update();

    assertEquals(List.of(target), attacks);
  }

  @Test
  void shouldStopOldPetAttacksAndRequireANewHitForReplacement() {
    PetManagerComponent manager = createCombatManager();
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    manager.activatePet(new ShopComponent.Pet("Bird"));
    Entity first = manager.getActivePet();
    List<Entity> oldPetAttacks = new ArrayList<>();
    EventListener1<Entity> oldAttackListener = oldPetAttacks::add;
    first.getEvents().addListener("petAttack", oldAttackListener);

    owner.getEvents().trigger("playerAttackHit", target);
    first.update();
    assertEquals(List.of(target), oldPetAttacks);

    manager.activatePet(new ShopComponent.Pet("Bat"));
    Entity second = manager.getActivePet();
    List<Entity> newPetAttacks = new ArrayList<>();

    EventListener1<Entity> newAttackListener = newPetAttacks::add;
    second.getEvents().addListener("petAttack", newAttackListener);
    second.update();
    assertTrue(newPetAttacks.isEmpty());

    owner.getEvents().trigger("playerAttackHit", target);
    for (int i = 0; i < 10; i++) {
      first.update();
    }
    second.update();

    assertTrue(first.isDisposed());
    assertEquals(List.of(target), oldPetAttacks);
    assertEquals(List.of(target), newPetAttacks);
  }

  @Test
  void shouldIgnoreHitsWhenNoPetOrPetHasNoCombatComponent() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());
    owner.addComponent(manager);
    ServiceLocator.getEntityService().register(owner);
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));

    assertDoesNotThrow(() -> owner.getEvents().trigger("playerAttackHit", target));
    manager.activatePet(new ShopComponent.Pet("Bird"));
    assertDoesNotThrow(() -> owner.getEvents().trigger("playerAttackHit", target));
    manager.removePet();
    assertDoesNotThrow(() -> owner.getEvents().trigger("playerAttackHit", target));
  }

  @Test
  void shouldCancelPendingAssistWhenOwnerIsDisposed() {
    PetManagerComponent manager = createCombatManager();
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    manager.activatePet(new ShopComponent.Pet("Bird"));
    Entity pet = manager.getActivePet();
    List<Entity> attacks = new ArrayList<>();
    EventListener1<Entity> attackListener = attacks::add;

    pet.getEvents().addListener("petAttack", attackListener);
    owner.getEvents().trigger("playerAttackHit", target);

    owner.dispose();
    owner.getEvents().trigger("playerAttackHit", target);
    pet.update();

    assertTrue(attacks.isEmpty());
    assertTrue(pet.isDisposed());
    assertFalse(manager.hasActivePet());
  }

  private PetManagerComponent createCombatManager() {
    GameTime timeSource = mock(GameTime.class);
    when(timeSource.getDeltaTime()).thenReturn(0.1f);
    PetManagerComponent manager =
        new PetManagerComponent(
            (petOwner, petData) ->
                new Entity()
                    .addComponent(new PetComponent(petOwner))
                    .addComponent(new PetCombatComponent(timeSource)));
    owner.addComponent(new CombatStatsComponent(100, 10)).addComponent(manager);
    ServiceLocator.getEntityService().register(owner);
    return manager;
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
  void shouldReplaceActiveGamblingPetBeforeAnyPlayerHit() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());
    owner.addComponent(manager);
    ServiceLocator.getEntityService().register(owner);
    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    ShopComponent.Pet spirit = new ShopComponent.Pet("Spirit");
    owner.getEvents().trigger("petPurchased", bird);
    Entity previousPet = manager.getActivePet();

    owner.getEvents().trigger("gamblingPetReplaced", bird, spirit);

    assertTrue(previousPet.isDisposed());
    assertNotSame(previousPet, manager.getActivePet());
    assertSame(spirit, manager.getActivePetType());
  }

  @Test
  void shouldKeepActivePetWhenInactiveGamblingPetIsReplaced() {
    PetManagerComponent manager = new PetManagerComponent((petOwner, petData) -> new Entity());
    owner.addComponent(manager);
    ServiceLocator.getEntityService().register(owner);
    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    owner.getEvents().trigger("petPurchased", bird);
    Entity activePet = manager.getActivePet();

    owner
        .getEvents()
        .trigger(
            "gamblingPetReplaced", new ShopComponent.Pet("Bat"), new ShopComponent.Pet("Spirit"));

    assertFalse(activePet.isDisposed());
    assertSame(activePet, manager.getActivePet());
    assertSame(bird, manager.getActivePetType());
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
