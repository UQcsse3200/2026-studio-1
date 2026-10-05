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
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.events.listeners.EventListener1;
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
}
