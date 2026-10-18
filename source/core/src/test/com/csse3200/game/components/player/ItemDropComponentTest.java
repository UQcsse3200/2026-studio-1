package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemDropComponentTest {

  @Test
  void shouldDropOneUnitFromSelectedStack() {
    List<Entity> spawned = new ArrayList<>();
    AtomicReference<Item> factoryItem = new AtomicReference<>();
    AtomicReference<Entity> factoryOwner = new AtomicReference<>();

    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> {
              factoryItem.set(item);
              factoryOwner.set(owner);
              return new Entity();
            },
            spawned::add);

    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 3)).addComponent(dropComponent);
    player.setPosition(2f, 3f);
    player.create();

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 2, 10, 1, 1);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 3, 9);
    inventory.addItem(sword);
    inventory.addItem(potion);
    inventory.setActiveSlot(2);

    assertTrue(dropComponent.dropActiveStack());

    assertSame(sword, inventory.getItem(1));
    assertSame(potion, inventory.getItem(2));
    assertEquals(2, inventory.getItem(2).getQuantity());
    assertEquals(2, inventory.getActiveSlot());
    assertNotSame(potion, factoryItem.get());
    assertEquals("Potion", factoryItem.get().getName());
    assertEquals(1, factoryItem.get().getQuantity());
    assertSame(player, factoryOwner.get());
    assertEquals(1, spawned.size());
    assertEquals(3.25f, spawned.getFirst().getPosition().x, 0.001f);
    assertEquals(3f, spawned.getFirst().getPosition().y, 0.001f);
  }

  @Test
  void shouldRespondToDropItemEvent() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();
    player
        .getComponent(InventoryComponent.class)
        .addItem(new Item("Potion", ItemType.CONSUMABLE, 2, 9));

    player.getEvents().trigger("dropItem");

    assertEquals(1, spawned.size());
    assertEquals(1, player.getComponent(InventoryComponent.class).getItem(1).getQuantity());
  }

  @Test
  void shouldDoNothingWhenInventoryIsEmpty() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();

    assertFalse(dropComponent.dropActiveStack());
    assertTrue(spawned.isEmpty());
  }

  @Test
  void shouldNotDropOtherItemsWhenSelectedSlotIsEmpty() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 3)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 2, 9);
    inventory.addItem(potion);
    inventory.setActiveSlot(3);

    assertFalse(dropComponent.dropActiveStack());

    assertSame(potion, inventory.getItem(1));
    assertEquals(3, inventory.getActiveSlot());
    assertTrue(spawned.isEmpty());
  }

  @Test
  void shouldDropTheSlotObjectWhenQuantityIsOne() {
    List<Entity> spawned = new ArrayList<>();
    AtomicReference<Item> factoryItem = new AtomicReference<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> {
              factoryItem.set(item);
              return new Entity();
            },
            spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();
    WeaponItem sword = new WeaponGenerator().generateWeapon(WeaponType.SWORD, 1);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(sword);

    assertTrue(dropComponent.dropActiveStack());

    assertFalse(inventory.containsItem(1));
    assertSame(sword, factoryItem.get());
    assertEquals(1, factoryItem.get().getQuantity());
    assertEquals(1, spawned.size());
  }

  @Test
  void shouldDropOneWeaponAndStackItAgain() {
    List<Entity> spawned = new ArrayList<>();
    AtomicReference<Item> factoryItem = new AtomicReference<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> {
              factoryItem.set(item);
              return new Entity();
            },
            spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 3)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    WeaponGenerator generator = new WeaponGenerator();
    WeaponItem sword = generator.generateWeapon(WeaponType.SWORD, 1);
    inventory.addItem(sword);
    inventory.addItem(generator.generateWeapon(WeaponType.SWORD, 1));

    assertTrue(dropComponent.dropActiveStack());

    assertEquals(1, inventory.getOccupiedSlots());
    assertSame(sword, inventory.getItem(1));
    assertEquals(1, inventory.getItem(1).getQuantity());
    Item dropped = factoryItem.get();
    assertNotSame(sword, dropped);
    assertEquals(1, dropped.getQuantity());
    assertEquals(sword.getSellPrice(), dropped.getSellPrice());
    assertInstanceOf(WeaponItem.class, dropped);
    assertEquals(WeaponType.SWORD, ((WeaponItem) dropped).getWeaponType());

    assertEquals(0, inventory.addItem(dropped));
    assertEquals(1, inventory.getOccupiedSlots());
    assertEquals(2, inventory.getItem(1).getQuantity());
    assertEquals(sword.getSellPrice(), inventory.getItem(1).getSellPrice());
  }

  @Test
  void shouldDropOneUnitFromTheFirstOccupiedStack() {
    List<Entity> spawned = new ArrayList<>();
    AtomicReference<Item> factoryItem = new AtomicReference<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> {
              factoryItem.set(item);
              return new Entity();
            },
            spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 3)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    Item first = new Item("Potion", ItemType.CONSUMABLE, 4, 9);
    Item second = new Item("Bread", ItemType.CONSUMABLE, 2, 9);
    inventory.addItem(first);
    inventory.addItem(second);

    assertTrue(dropComponent.dropFirstStack());

    assertSame(first, inventory.getItem(1));
    assertEquals(3, inventory.getItem(1).getQuantity());
    assertSame(second, inventory.getItem(2));
    assertEquals(2, inventory.getItem(2).getQuantity());
    assertEquals(1, factoryItem.get().getQuantity());
    assertEquals("Potion", factoryItem.get().getName());
  }

  @Test
  void shouldLeaveTheStackUntouchedWhenTheLootFactoryReturnsNull() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent = new ItemDropComponent((item, owner) -> null, spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(new Item("Potion", ItemType.CONSUMABLE, 3, 9));

    assertFalse(dropComponent.dropActiveStack());

    assertEquals(3, inventory.getItem(1).getQuantity());
    assertTrue(spawned.isEmpty());
  }

  @Test
  void shouldRestoreTheDroppedUnitWhenSpawningFails() {
    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> new Entity(),
            loot -> {
              throw new IllegalStateException("spawn failed");
            });
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(new Item("Potion", ItemType.CONSUMABLE, 3, 9));

    assertThrows(IllegalStateException.class, dropComponent::dropActiveStack);

    assertEquals(3, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldDropGold() {
    List<Entity> spawned = new ArrayList<>();
    AtomicReference<Item> factoryItem = new AtomicReference<>();
    AtomicReference<Entity> factoryOwner = new AtomicReference<>();

    int numGold = 3;

    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> {
              factoryItem.set(item);
              factoryOwner.set(owner);
              return new Entity();
            },
            spawned::add);

    Entity player =
        new Entity().addComponent(new InventoryComponent(numGold, 3)).addComponent(dropComponent);
    player.setPosition(2f, 3f);
    player.create();

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    assertTrue(dropComponent.dropGold());

    assertFalse(inventory.hasGold(1));
    assertSame(ItemType.CURRENCY, factoryItem.get().getItemType());
    assertEquals(numGold, factoryItem.get().getQuantity());
    assertSame(player, factoryOwner.get());
    assertEquals(1, spawned.size());
    assertEquals(3.25f, spawned.getFirst().getPosition().x, 0.001f);
    assertEquals(3f, spawned.getFirst().getPosition().y, 0.001f);
  }

  @Test
  void shouldNotDropZeroGold() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();

    assertFalse(dropComponent.dropGold());
    assertTrue(spawned.isEmpty());
  }
}
