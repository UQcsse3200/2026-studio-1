package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KeyboardPlayerInputComponentTest {

  @Test
  void shouldTriggerDropEventWhenQIsPressed() {
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player = new Entity().addComponent(input);
    AtomicInteger drops = new AtomicInteger();
    player.getEvents().addListener("dropItem", drops::incrementAndGet);

    assertTrue(input.keyDown(Keys.Q));
    assertEquals(1, drops.get());
  }

  @Test
  void shouldDropOnlyTheSlotSelectedWithNumberKeys() {
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    InventoryComponent inventory = new InventoryComponent(0);
    List<Entity> spawned = new ArrayList<>();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), spawned::add);
    new Entity().addComponent(input).addComponent(inventory).addComponent(drop);
    drop.create();
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 10, 1, 1);
    WeaponItem bow = new WeaponItem("Bow", WeaponType.BOW, 10, 1, 1);
    inventory.addItem(sword);
    inventory.addItem(bow);

    assertTrue(input.keyDown(Keys.NUM_2));
    assertTrue(input.keyDown(Keys.Q));

    assertFalse(inventory.containsItem(2));
    assertSame(sword, inventory.getItem(1));
    assertEquals(2, inventory.getActiveSlot());
    assertEquals(1, spawned.size());

    // Pressing Q again on the emptied slot must not drop a different item.
    assertTrue(input.keyDown(Keys.Q));
    assertSame(sword, inventory.getItem(1));
    assertEquals(1, spawned.size());

    assertTrue(input.keyDown(Keys.NUM_1));
    assertTrue(input.keyDown(Keys.Q));
    assertEquals(0, inventory.getOccupiedSlots());
    assertEquals(2, spawned.size());
  }

  /** Acceptance criterion: pressing the shield key fires the shield activation event. */
  @Test
  void shouldTriggerActivateShieldEventWhenBIsPressed() {
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player = new Entity().addComponent(input);
    AtomicInteger activations = new AtomicInteger();
    player.getEvents().addListener("activateShield", activations::incrementAndGet);

    assertTrue(input.keyDown(Keys.B));
    assertEquals(1, activations.get());
  }
}
