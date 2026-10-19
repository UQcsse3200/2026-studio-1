package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

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

  @ParameterizedTest
  @ValueSource(ints = {1, 3})
  void shouldRestoreTheDroppedUnitWhenSpawningFails(int quantity) {
    IllegalStateException failure = new IllegalStateException("spawn failed");
    ItemDropComponent dropComponent =
        new ItemDropComponent(
            (item, owner) -> new Entity(),
            loot -> {
              throw failure;
            });
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(new Item("Potion", ItemType.CONSUMABLE, quantity, 9));
    List<Item> reported = recordDroppedItems(player);

    assertSame(failure, assertThrows(IllegalStateException.class, dropComponent::dropActiveStack));

    assertEquals(quantity, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertTrue(reported.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 99})
  void shouldDropGold(int numGold) {
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
        new Entity().addComponent(new InventoryComponent(numGold, 3)).addComponent(dropComponent);
    player.setPosition(2f, 3f);
    player.create();

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    List<Integer> reportedGold = new ArrayList<>();
    player
        .getEvents()
        .addListener(
            "goldDropped",
            (Integer amount, Entity loot) -> {
              assertEquals(0, inventory.getGold());
              assertEquals(List.of(loot), spawned);
              reportedGold.add(amount);
            });

    assertTrue(dropComponent.dropGold());

    assertFalse(inventory.hasGold(1));
    assertSame(ItemType.CURRENCY, factoryItem.get().getItemType());
    assertEquals(numGold, factoryItem.get().getQuantity());
    assertSame(player, factoryOwner.get());
    assertEquals(1, spawned.size());
    assertEquals(3.25f, spawned.getFirst().getPosition().x, 0.001f);
    assertEquals(3f, spawned.getFirst().getPosition().y, 0.001f);
    assertEquals(List.of(numGold), reportedGold);
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

  @ParameterizedTest
  @CsvSource({"true,false", "false,true", "true,true"})
  void shouldRejectMissingWorldDependencies(boolean missingFactory, boolean missingSpawner) {
    BiFunction<Item, Entity, Entity> factory =
        missingFactory ? null : (item, owner) -> new Entity();
    Consumer<Entity> spawner = missingSpawner ? null : loot -> {};

    assertThrows(IllegalArgumentException.class, () -> new ItemDropComponent(factory, spawner));
  }

  @Test
  void shouldIgnoreDropRequestsWithoutInventoryOrShield() {
    List<Entity> spawned = new ArrayList<>();
    List<Item> requested = new ArrayList<>();
    ItemDropComponent drop =
        new ItemDropComponent(
            (item, owner) -> {
              requested.add(item);
              return new Entity();
            },
            spawned::add);
    Entity player = new Entity().addComponent(drop);
    player.create();

    assertFalse(drop.dropActiveStack());
    assertFalse(drop.dropFirstStack());
    assertFalse(drop.dropGold());
    assertFalse(drop.dropShield());
    assertTrue(requested.isEmpty());
    assertTrue(spawned.isEmpty());
  }

  @Test
  void shouldNotDropFirstStackWhenEverySlotIsEmpty() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player = new Entity().addComponent(new InventoryComponent(0, 3)).addComponent(drop);
    player.create();

    assertFalse(drop.dropFirstStack());
    assertTrue(spawned.isEmpty());
  }

  @Test
  void shouldFindFirstOccupiedSlotAfterAGapWithoutChangingSelection() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    InventoryComponent inventory = new InventoryComponent(0, 3);
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.create();
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    Item bread = new Item("Bread", ItemType.CONSUMABLE, 2, 9);
    inventory.setItem(2, potion);
    inventory.setItem(3, bread);
    inventory.setActiveSlot(3);
    List<Item> reported = recordDroppedItems(player);

    assertTrue(drop.dropFirstStack());

    assertFalse(inventory.containsItem(2));
    assertSame(bread, inventory.getItem(3));
    assertEquals(3, inventory.getActiveSlot());
    assertEquals(List.of(potion), reported);
    assertEquals(1, spawned.size());
  }

  @Test
  void shouldRegisterDefaultFactoryLootWithTheEntityService() {
    EntityService entities = mock(EntityService.class);
    ServiceLocator.registerEntityService(entities);
    InventoryComponent inventory = new InventoryComponent(0, 2);
    ItemDropComponent drop = new ItemDropComponent();
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.setPosition(2f, 3f);
    player.create();
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    inventory.addItem(potion);
    Entity loot = new Entity();
    List<Item> reported = recordDroppedItems(player);

    try (MockedStatic<LootFactory> factory = mockStatic(LootFactory.class)) {
      factory.when(() -> LootFactory.createDroppedLoot(potion, player)).thenReturn(loot);

      assertTrue(drop.dropActiveStack());

      factory.verify(() -> LootFactory.createDroppedLoot(potion, player));
      verify(entities).register(loot);
    }
    assertFalse(inventory.containsItem(1));
    assertEquals(3.25f, loot.getPosition().x, 0.001f);
    assertEquals(3f, loot.getPosition().y, 0.001f);
    assertEquals(List.of(potion), reported);
  }

  @Test
  void shouldNotSpawnWhenInventoryRefusesToRemoveTheLastUnit() {
    InventoryComponent inventory = spy(new InventoryComponent(0, 2));
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 1, 9);
    inventory.addItem(potion);
    doReturn(null).when(inventory).removeItem(1);
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.create();
    List<Item> reported = recordDroppedItems(player);

    assertFalse(drop.dropActiveStack());

    assertSame(potion, inventory.getItem(1));
    assertTrue(spawned.isEmpty());
    assertTrue(reported.isEmpty());
  }

  @Test
  void shouldPreserveTheSelectedStackWhenFactoryCreationThrows() {
    IllegalStateException failure = new IllegalStateException("factory failed");
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop =
        new ItemDropComponent(
            (item, owner) -> {
              throw failure;
            },
            spawned::add);
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.create();
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 3, 9);
    inventory.setItem(2, potion);
    inventory.setActiveSlot(2);
    List<Item> reported = recordDroppedItems(player);

    assertSame(failure, assertThrows(IllegalStateException.class, drop::dropActiveStack));

    assertSame(potion, inventory.getItem(2));
    assertEquals(3, potion.getQuantity());
    assertEquals(2, inventory.getActiveSlot());
    assertTrue(spawned.isEmpty());
    assertTrue(reported.isEmpty());
  }

  @Test
  void shouldKeepGoldWhenFactoryReturnsNull() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> null, spawned::add);
    InventoryComponent inventory = new InventoryComponent(25, 2);
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.create();
    List<Integer> reported = recordDroppedGold(player);

    assertFalse(drop.dropGold());

    assertEquals(25, inventory.getGold());
    assertTrue(spawned.isEmpty());
    assertTrue(reported.isEmpty());
  }

  @Test
  void shouldRestoreGoldWithoutReportingADropWhenSpawningFails() {
    IllegalStateException failure = new IllegalStateException("spawn failed");
    ItemDropComponent drop =
        new ItemDropComponent(
            (item, owner) -> new Entity(),
            loot -> {
              throw failure;
            });
    InventoryComponent inventory = new InventoryComponent(25, 2);
    Entity player = new Entity().addComponent(inventory).addComponent(drop);
    player.create();
    List<Integer> reported = recordDroppedGold(player);

    assertSame(failure, assertThrows(IllegalStateException.class, drop::dropGold));

    assertEquals(25, inventory.getGold());
    assertTrue(reported.isEmpty());
  }

  @Test
  void shouldDropHeldShieldWithoutUsingInventory() {
    ShieldComponent shield = new ShieldComponent();
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop =
        new ItemDropComponent(
            (item, owner) -> {
              assertSame(shield, owner.getComponent(ShieldComponent.class));
              return new Entity();
            },
            spawned::add);
    Entity player = new Entity().addComponent(shield).addComponent(drop);
    player.setPosition(4f, 2f);
    player.create();
    shield.grantShield();
    List<Item> reported = recordDroppedItems(player);

    assertTrue(drop.dropShield());

    assertFalse(shield.hasShield());
    assertEquals(1, reported.size());
    assertEquals(ItemType.SHIELD, reported.getFirst().getItemType());
    assertEquals(1, reported.getFirst().getQuantity());
    assertEquals(1, spawned.size());
    assertEquals(5.25f, spawned.getFirst().getPosition().x, 0.001f);
    assertEquals(2f, spawned.getFirst().getPosition().y, 0.001f);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void shouldNotDropAShieldThatIsNotHeld(boolean activated) {
    ShieldComponent shield = new ShieldComponent();
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player = new Entity().addComponent(shield).addComponent(drop);
    player.create();
    if (activated) {
      shield.grantShield();
      assertTrue(shield.activateShield());
    }
    List<Item> reported = recordDroppedItems(player);

    assertFalse(drop.dropShield());

    assertEquals(activated, shield.isActive());
    assertTrue(spawned.isEmpty());
    assertTrue(reported.isEmpty());
  }

  @Test
  void shouldRestoreHeldShieldWhenSpawningFails() {
    ShieldComponent shield = new ShieldComponent();
    IllegalStateException failure = new IllegalStateException("spawn failed");
    ItemDropComponent drop =
        new ItemDropComponent(
            (item, owner) -> new Entity(),
            loot -> {
              throw failure;
            });
    Entity player = new Entity().addComponent(shield).addComponent(drop);
    player.create();
    shield.grantShield();
    List<Item> reported = recordDroppedItems(player);

    assertSame(failure, assertThrows(IllegalStateException.class, drop::dropShield));

    assertTrue(shield.hasShield());
    assertFalse(shield.isActive());
    assertTrue(reported.isEmpty());
  }

  private List<Item> recordDroppedItems(Entity player) {
    List<Item> reported = new ArrayList<>();
    player.getEvents().addListener("itemDropped", (Item item, Entity loot) -> reported.add(item));
    return reported;
  }

  private List<Integer> recordDroppedGold(Entity player) {
    List<Integer> reported = new ArrayList<>();
    player
        .getEvents()
        .addListener("goldDropped", (Integer amount, Entity loot) -> reported.add(amount));
    return reported;
  }
}
