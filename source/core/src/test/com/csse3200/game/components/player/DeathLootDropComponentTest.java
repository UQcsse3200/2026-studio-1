package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DeathLootDropComponentTest {
  private List<Item> droppedItems;
  private List<Entity> droppedOwners;
  private List<Entity> spawnedLoot;

  @BeforeEach
  void setUp() {
    droppedItems = new ArrayList<>();
    droppedOwners = new ArrayList<>();
    spawnedLoot = new ArrayList<>();
  }

  /** Creates a hero whose loot is recorded in the lists above instead of the real game world. */
  private Entity createHero(int gold, int maxSlots) {
    DeathLootDropComponent dropComponent =
        new DeathLootDropComponent(
            (item, owner) -> {
              droppedItems.add(item);
              droppedOwners.add(owner);
              return new Entity();
            },
            spawnedLoot::add);

    Entity hero =
        new Entity()
            .addComponent(new InventoryComponent(gold, maxSlots))
            .addComponent(dropComponent);
    hero.create();
    return hero;
  }

  private Item potion(String name) {
    return new Item(name, ItemType.CONSUMABLE, 2, 9);
  }

  @Test
  void shouldDropEveryItemAndAllGoldOnDeath() {
    Entity hero = createHero(40, 5);
    InventoryComponent inventory = hero.getComponent(InventoryComponent.class);
    WeaponItem bow = new WeaponItem("Bow", WeaponType.BOW, 2, 10, 1, 3);
    Item health = potion("Health Potion");
    inventory.addItem(bow);
    inventory.addItem(health);

    hero.getEvents().trigger("death");

    assertEquals(3, spawnedLoot.size());
    assertSame(bow, droppedItems.get(0));
    assertSame(health, droppedItems.get(1));
    assertEquals(2, droppedItems.get(1).getQuantity());

    Item gold = droppedItems.get(2);
    assertEquals(ItemType.CURRENCY, gold.getItemType());
    assertEquals(40, gold.getQuantity());

    // The dead hero is passed as the dropper, so it is briefly blocked from picking loot back up.
    assertSame(hero, droppedOwners.getFirst());
  }

  @Test
  void shouldEmptyTheInventorySoNothingIsDuplicated() {
    Entity hero = createHero(25, 5);
    InventoryComponent inventory = hero.getComponent(InventoryComponent.class);
    inventory.addItem(potion("Health Potion"));
    inventory.addItem(potion("Speed Potion"));

    hero.getEvents().trigger("death");

    assertEquals(0, inventory.getOccupiedSlots());
    assertEquals(0, inventory.getGold());
  }

  @Test
  void shouldOnlyDropOnceWhenDeathFiresMoreThanOnce() {
    Entity hero = createHero(10, 5);
    hero.getComponent(InventoryComponent.class).addItem(potion("Health Potion"));

    hero.getEvents().trigger("death");
    hero.getEvents().trigger("death");

    assertEquals(2, spawnedLoot.size());
    assertEquals(0, hero.getComponent(DeathLootDropComponent.class).dropEverything());
    assertEquals(2, spawnedLoot.size());
  }

  @Test
  void shouldDropNothingForAnEmptyInventory() {
    Entity hero = createHero(0, 5);

    hero.getEvents().trigger("death");

    assertTrue(spawnedLoot.isEmpty());
  }

  @Test
  void shouldDropEverySlotOfAFullInventory() {
    Entity hero = createHero(0, 5);
    InventoryComponent inventory = hero.getComponent(InventoryComponent.class);
    for (int i = 1; i <= 5; i++) {
      inventory.addItem(potion("Potion " + i));
    }
    assertTrue(inventory.isFull());

    hero.getEvents().trigger("death");

    assertEquals(5, spawnedLoot.size());
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldDropAllGoldEvenAboveANormalStackSize() {
    Entity hero = createHero(150, 5);

    hero.getEvents().trigger("death");

    assertEquals(1, spawnedLoot.size());
    assertEquals(150, droppedItems.getFirst().getQuantity());
    assertEquals(0, hero.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void shouldPlaceLootInARowCentredOnTheDeathSpot() {
    Entity hero = createHero(5, 5);
    InventoryComponent inventory = hero.getComponent(InventoryComponent.class);
    inventory.addItem(potion("Health Potion"));
    inventory.addItem(potion("Speed Potion"));
    hero.setPosition(10f, 4f);

    hero.getEvents().trigger("death");

    assertEquals(3, spawnedLoot.size());
    assertEquals(9.6f, spawnedLoot.get(0).getPosition().x, 0.001f);
    assertEquals(10f, spawnedLoot.get(1).getPosition().x, 0.001f);
    assertEquals(10.4f, spawnedLoot.get(2).getPosition().x, 0.001f);
    for (Entity loot : spawnedLoot) {
      assertEquals(4f, loot.getPosition().y, 0.001f);
    }
  }

  @Test
  void shouldGiveEachDeadHeroItsOwnLootPile() {
    Entity firstHero = createHero(0, 5);
    firstHero.getComponent(InventoryComponent.class).addItem(potion("Health Potion"));
    firstHero.setPosition(3f, 1f);
    firstHero.getEvents().trigger("death");

    Entity secondHero = createHero(0, 5);
    secondHero.getComponent(InventoryComponent.class).addItem(potion("Speed Potion"));
    secondHero.setPosition(20f, 6f);
    secondHero.getEvents().trigger("death");

    assertEquals(2, spawnedLoot.size());
    assertEquals(3f, spawnedLoot.get(0).getPosition().x, 0.001f);
    assertEquals(20f, spawnedLoot.get(1).getPosition().x, 0.001f);
    assertSame(firstHero, droppedOwners.get(0));
    assertSame(secondHero, droppedOwners.get(1));
  }

  @Test
  void shouldDoNothingWithoutAnInventory() {
    DeathLootDropComponent dropComponent =
        new DeathLootDropComponent((item, owner) -> new Entity(), spawnedLoot::add);
    Entity hero = new Entity().addComponent(dropComponent);
    hero.create();

    hero.getEvents().trigger("death");

    assertTrue(spawnedLoot.isEmpty());
  }

  @Test
  void shouldRejectMissingFactoryOrSpawner() {
    assertThrows(
        IllegalArgumentException.class, () -> new DeathLootDropComponent(null, spawnedLoot::add));
    assertThrows(
        IllegalArgumentException.class,
        () -> new DeathLootDropComponent((item, owner) -> new Entity(), null));
  }
}
