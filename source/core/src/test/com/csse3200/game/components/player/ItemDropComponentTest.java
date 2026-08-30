package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
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
  void shouldDropWholeStackFromLowestOccupiedSlot() {
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
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 10, 1, 1);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 3, 9);
    inventory.addItem(sword);
    inventory.addItem(potion);

    assertTrue(dropComponent.dropFirstStack());

    assertFalse(inventory.containsItem(1));
    assertSame(potion, inventory.getItem(2));
    assertSame(sword, factoryItem.get());
    assertSame(player, factoryOwner.get());
    assertEquals(1, spawned.size());
    assertEquals(3.25f, spawned.get(0).getPosition().x, 0.001f);
    assertEquals(3f, spawned.get(0).getPosition().y, 0.001f);
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
    assertFalse(player.getComponent(InventoryComponent.class).containsItem(1));
  }

  @Test
  void shouldDoNothingWhenInventoryIsEmpty() {
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent dropComponent =
        new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    Entity player =
        new Entity().addComponent(new InventoryComponent(0, 2)).addComponent(dropComponent);
    player.create();

    assertFalse(dropComponent.dropFirstStack());
    assertTrue(spawned.isEmpty());
  }
}
