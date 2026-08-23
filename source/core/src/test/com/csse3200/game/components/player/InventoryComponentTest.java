package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InventoryComponentTest {
  @Test
  void shouldSetGetGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    assertEquals(100, inventory.getGold());

    inventory.setGold(150);
    assertEquals(150, inventory.getGold());

    inventory.setGold(-50);
    assertEquals(0, inventory.getGold());
  }

  @Test
  void shouldCheckHasGold() {
    InventoryComponent inventory = new InventoryComponent(150);
    assertTrue(inventory.hasGold(100));
    assertFalse(inventory.hasGold(200));
  }

  @Test
  void shouldAddGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    inventory.addGold(-500);
    assertEquals(0, inventory.getGold());

    inventory.addGold(100);
    inventory.addGold(-20);
    assertEquals(80, inventory.getGold());
  }

  @Test
  void shouldStartEmpty() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    assertEquals(0, inventory.getOccupiedSlots());
    assertFalse(inventory.isFull());
    assertNull(inventory.getItem(1));
  }

  @Test
  void shouldReturnZeroWhenAddingNullItem() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    assertEquals(0, inventory.addItem(null));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReturnZeroWhenAddingZeroQuantityItem() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item empty = potion(0, 10);
    assertEquals(0, inventory.addItem(empty));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldKeepIncomingItemIdentityInFirstEmptySlot() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item incoming = potion(4, 10);

    assertEquals(0, inventory.addItem(incoming));

    assertSame(incoming, inventory.getItem(1));
    assertEquals(4, incoming.getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldStackCompatibleItemsIntoExistingSlot() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(3, 10));
    Item incoming = potion(4, 10);

    assertEquals(0, inventory.addItem(incoming));

    assertEquals(7, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertFalse(inventory.containsItem(2));
  }

  @Test
  void shouldNotExceedMaxQuantityWhenStacking() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(7, 10));

    assertEquals(0, inventory.addItem(potion(8, 10)));

    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(10, inventory.getItem(1).getMaxQuantity());
    assertEquals(5, inventory.getItem(2).getQuantity());
  }

  @Test
  void shouldPlaceOverflowInNextEmptySlot() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(7, 10));
    Item incoming = potion(8, 10);

    assertEquals(0, inventory.addItem(incoming));

    assertEquals(10, inventory.getItem(1).getQuantity());
    assertSame(incoming, inventory.getItem(2));
    assertEquals(5, incoming.getQuantity());
  }

  @Test
  void shouldNotStackIncompatibleItems() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(3, 10));
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);

    assertEquals(0, inventory.addItem(sword));

    assertEquals(3, inventory.getItem(1).getQuantity());
    assertSame(sword, inventory.getItem(2));
  }

  @Test
  void shouldNotStackWhenMaxQuantityDiffers() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(2, 10));
    Item incoming = potion(2, 5);

    assertEquals(0, inventory.addItem(incoming));

    assertEquals(2, inventory.getItem(1).getQuantity());
    assertSame(incoming, inventory.getItem(2));
  }

  @Test
  void shouldReturnLeftoverWhenInventoryIsFull() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    inventory.addItem(potion(10, 10));
    Item incoming = potion(3, 10);

    assertEquals(3, inventory.addItem(incoming));

    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertTrue(inventory.isFull());
    assertEquals(3, incoming.getQuantity());
  }

  @Test
  void shouldReturnLeftoverAfterFillingLastCompatibleStack() {
    InventoryComponent inventory = new InventoryComponent(0, 1);
    inventory.addItem(potion(7, 10));
    Item incoming = potion(8, 10);

    assertEquals(5, inventory.addItem(incoming));

    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertTrue(inventory.isFull());
  }

  @Test
  void shouldFillLowestEmptySlotInOrder() {
    InventoryComponent inventory = new InventoryComponent(0, 3);
    Item first = potion(1, 10);
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);

    inventory.addItem(first);
    inventory.addItem(sword);

    assertSame(first, inventory.getItem(1));
    assertSame(sword, inventory.getItem(2));
    assertFalse(inventory.containsItem(3));
  }

  @Test
  void shouldNotStoreIncomingItemWhenFullyMerged() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item existing = potion(5, 10);
    inventory.addItem(existing);
    Item incoming = potion(3, 10);

    assertEquals(0, inventory.addItem(incoming));

    assertSame(existing, inventory.getItem(1));
    assertNotSame(incoming, inventory.getItem(1));
    assertEquals(8, existing.getQuantity());
    assertEquals(3, incoming.getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldNotCreateExtraStackWhenIncomingQuantityIsClamped() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item incoming = potion(15, 10);

    assertEquals(10, incoming.getQuantity());
    assertEquals(0, inventory.addItem(incoming));

    assertSame(incoming, inventory.getItem(1));
    assertEquals(10, incoming.getQuantity());
    assertFalse(inventory.containsItem(2));
  }

  @Test
  void shouldUseDefaultMaxSlotsWhenCapacityOmitted() {
    InventoryComponent inventory = new InventoryComponent(0);
    assertEquals(5, inventory.getMaxSlots());
    assertFalse(inventory.isFull());
  }

  @Test
  void shouldRejectNonPositiveMaxSlots() {
    assertThrows(IllegalArgumentException.class, () -> new InventoryComponent(0, 0));
    assertThrows(IllegalArgumentException.class, () -> new InventoryComponent(0, -1));
  }

  @Test
  void shouldReuseLowestEmptySlotAfterRemoval() {
    InventoryComponent inventory = new InventoryComponent(0, 3);
    Item first = potion(1, 10);
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);
    Item replacement = potion(2, 10);

    inventory.addItem(first);
    inventory.addItem(sword);
    inventory.removeItem(1);

    assertEquals(0, inventory.addItem(replacement));

    assertSame(replacement, inventory.getItem(1));
    assertSame(sword, inventory.getItem(2));
    assertFalse(inventory.containsItem(3));
  }

  @Test
  void shouldRemoveOnlyAvailableQuantity() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(3, 10));

    assertEquals(3, inventory.removeItem(1, 10));
    assertFalse(inventory.containsItem(1));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReduceQuantityOnPartialRemove() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(5, 10));

    assertEquals(3, inventory.removeItem(1, 3));

    assertEquals(2, inventory.getItem(1).getQuantity());
    assertTrue(inventory.containsItem(1));
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldClearSlotWhenRemovedAmountEmptiesStack() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(5, 10));

    assertEquals(5, inventory.removeItem(1, 5));

    assertFalse(inventory.containsItem(1));
    assertNull(inventory.getItem(1));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldRemoveWholeStackFromSlot() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item stored = potion(4, 10);
    inventory.addItem(stored);

    assertSame(stored, inventory.removeItem(1));
    assertFalse(inventory.containsItem(1));
    assertNull(inventory.getItem(1));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldNotCompactSlotsAfterRemoval() {
    InventoryComponent inventory = new InventoryComponent(0, 3);
    Item first = potion(1, 10);
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);
    inventory.addItem(first);
    inventory.addItem(sword);

    inventory.removeItem(1);

    assertFalse(inventory.containsItem(1));
    assertSame(sword, inventory.getItem(2));
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldHandleInvalidAndEmptySlotsSafely() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    inventory.addItem(potion(2, 10));

    assertNull(inventory.getItem(0));
    assertNull(inventory.getItem(3));
    assertNull(inventory.getItem(-1));
    assertFalse(inventory.containsItem(0));
    assertFalse(inventory.containsItem(2));

    assertEquals(0, inventory.removeItem(0, 1));
    assertEquals(0, inventory.removeItem(2, 1));
    assertEquals(0, inventory.removeItem(1, 0));
    assertEquals(0, inventory.removeItem(1, -1));
    assertNull(inventory.removeItem(0));
    assertNull(inventory.removeItem(2));

    assertEquals(2, inventory.getItem(1).getQuantity());
  }

  @Test
  void shouldMatchOccupiedCountAfterAddAndRemove() {
    InventoryComponent inventory = new InventoryComponent(0, 3);
    inventory.addItem(potion(1, 10));
    inventory.addItem(new Item("Sword", ItemType.WEAPON, 1, 1));

    assertEquals(2, inventory.getOccupiedSlots());
    assertEquals(inventory.getInventorySlots().size(), inventory.getOccupiedSlots());

    inventory.removeItem(1, 1);

    assertEquals(1, inventory.getOccupiedSlots());
    assertFalse(inventory.containsItem(1));
    assertTrue(inventory.containsItem(2));
  }

  @Test
  void shouldStoreCurrencyItemWithoutChangingGold() {
    InventoryComponent inventory = new InventoryComponent(50, 2);
    Item coins = new Item("Coins", ItemType.CURRENCY, 10, 100);

    assertEquals(0, inventory.addItem(coins));

    assertEquals(50, inventory.getGold());
    assertSame(coins, inventory.getItem(1));
  }

  @Test
  void shouldReturnUnmodifiableInventorySlots() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item stored = potion(1, 10);
    inventory.addItem(stored);

    assertThrows(
        UnsupportedOperationException.class,
        () -> inventory.getInventorySlots().put(2, potion(1, 10)));
    assertThrows(
        UnsupportedOperationException.class, () -> inventory.getInventorySlots().remove(1));
    assertEquals(1, inventory.getOccupiedSlots());
    assertSame(stored, inventory.getItem(1));
  }

  @Test
  void shouldSplitSinglePickupAcrossSlotsWhenQuantityExceedsStackCap() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldTemplate = gold(1, 9);

    assertEquals(0, inventory.addItem(goldTemplate, 17));

    assertSame(goldTemplate, inventory.getItem(1));
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertEquals(8, inventory.getItem(2).getQuantity());
    assertEquals(2, inventory.getOccupiedSlots());
  }

  @Test
  void shouldMergeThenSplitAcrossSlotsInSinglePickup() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item existing = gold(5, 9);
    Item goldTemplate = gold(1, 9);

    inventory.addItem(existing);

    assertEquals(0, inventory.addItem(goldTemplate, 17));

    assertEquals(9, inventory.getItem(1).getQuantity());
    assertEquals(9, inventory.getItem(2).getQuantity());
    assertEquals(4, inventory.getItem(3).getQuantity());
    assertEquals(3, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReturnLeftoverWhenNoSlotsRemainWithQuantityOverload() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    for (int slot = 1; slot <= 5; slot++) {
      inventory.addItem(gold(9, 9));
    }

    assertEquals(1, inventory.addItem(gold(1, 9), 1));
    assertTrue(inventory.isFull());
    assertEquals(5, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReturnZeroWhenAddingNonPositiveQuantity() {
    InventoryComponent inventory = new InventoryComponent(0, 2);
    Item goldTemplate = gold(1, 9);

    assertEquals(0, inventory.addItem(goldTemplate, 0));
    assertEquals(0, inventory.addItem(goldTemplate, -3));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReturnFullAmountWhenAddingNullItemWithQuantityOverload() {
    InventoryComponent inventory = new InventoryComponent(0, 2);

    assertEquals(5, inventory.addItem(null, 5));
    assertEquals(0, inventory.getOccupiedSlots());
  }

  @Test
  void shouldMergeIntoMultiplePartialStacksInSinglePickup() {
    InventoryComponent inventory = new InventoryComponent(0, 4);
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);

    inventory.addItem(gold(9, 9)); // slot 1
    inventory.addItem(sword); // slot 2
    inventory.addItem(gold(9, 9)); // slot 3

    inventory.removeItem(1, 4); // slot 1 = 5/9
    inventory.removeItem(3, 4); // slot 3 = 5/9

    assertEquals(0, inventory.addItem(gold(1, 9), 8));
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertSame(sword, inventory.getItem(2));
    assertEquals(9, inventory.getItem(3).getQuantity());
    assertFalse(inventory.containsItem(4));
  }

  @Test
  void shouldMergeIntoSameReferenceStackOnSecondPickup() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(5, 9);

    assertEquals(0, inventory.addItem(goldItem));
    assertEquals(0, inventory.addItem(goldItem, 7));

    assertSame(goldItem, inventory.getItem(1));
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertEquals(3, inventory.getItem(2).getQuantity());
    assertNotSame(goldItem, inventory.getItem(2));
    assertEquals(2, inventory.getOccupiedSlots());
  }

  @Test
  void shouldNotStoreSameReferenceInTwoSlots() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(7, 9);

    inventory.addItem(goldItem);
    assertEquals(0, inventory.addItem(goldItem, 5));

    assertSame(goldItem, inventory.getItem(1));
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertEquals(3, inventory.getItem(2).getQuantity());
    assertNotSame(goldItem, inventory.getItem(2));
    assertEquals(2, inventory.getOccupiedSlots());
  }

  @Test
  void shouldMergeSameReferenceAndReturnLeftoverWhenNoEmptySlot() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(5, 9);

    inventory.addItem(goldItem);
    for (int slot = 2; slot <= 5; slot++) {
      inventory.addItem(gold(9, 9));
    }

    assertEquals(6, inventory.addItem(goldItem, 10));

    assertSame(goldItem, inventory.getItem(1));
    assertEquals(9, inventory.getItem(1).getQuantity());
    assertTrue(inventory.isFull());
  }

  @Test
  void shouldCanStackWithCompatibleItems() {
    Item gold1 = gold(5, 9);
    Item gold2 = gold(3, 9);

    InventoryComponent inventory = new InventoryComponent(0);
    assertTrue(inventory.canStack(gold1, gold2));
  }

  @Test
  void shouldNotCanStackWhenExistingIsFull() {
    Item goldFull = gold(9, 9);
    Item goldNew = gold(1, 9);

    InventoryComponent inventory = new InventoryComponent(0);
    assertFalse(inventory.canStack(goldFull, goldNew));
  }

  @Test
  void shouldNotCanStackWithDifferentName() {
    Item gold = gold(5, 9);
    Item potion = new Item("Potion", ItemType.CONSUMABLE, 5, 9);

    InventoryComponent inventory = new InventoryComponent(0);
    assertFalse(inventory.canStack(gold, potion));
  }

  @Test
  void shouldNotCanStackWithDifferentMaxQuantity() {
    Item gold9 = gold(5, 9);
    Item gold10 = new Item("Gold", ItemType.CURRENCY, 5, 10);

    InventoryComponent inventory = new InventoryComponent(0);
    assertFalse(inventory.canStack(gold9, gold10));
  }

  @Test
  void shouldNotCanStackWhenExistingIsNull() {
    Item gold = gold(5, 9);

    InventoryComponent inventory = new InventoryComponent(0);
    assertFalse(inventory.canStack(null, gold));
  }

  @Test
  void shouldNotCanStackWhenIncomingIsNull() {
    Item gold = gold(5, 9);

    InventoryComponent inventory = new InventoryComponent(0);
    assertFalse(inventory.canStack(gold, null));
  }

  @Test
  void shouldReturnSingleSlotQuantityAsTotal() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    inventory.addItem(gold(5, 9));

    assertEquals(5, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 9));
    assertEquals(5, inventory.getTotalQuantity(gold(1, 9)));
  }

  @Test
  void shouldSumQuantityAcrossCompatibleSlots() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldTemplate = gold(1, 9);

    assertEquals(0, inventory.addItem(goldTemplate, 17));
    assertEquals(17, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 9));
    assertEquals(17, inventory.getTotalQuantity(goldTemplate));
  }

  @Test
  void shouldNotSumItemsWithDifferentMaxQuantity() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    inventory.addItem(gold(5, 9));
    inventory.addItem(new Item("Gold", ItemType.CURRENCY, 4, 10));

    assertEquals(5, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 9));
    assertEquals(4, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 10));
  }

  @Test
  void shouldNotSumItemsWithDifferentNameOrType() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    inventory.addItem(gold(5, 9));
    inventory.addItem(potion(3, 9));

    assertEquals(5, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 9));
    assertEquals(3, inventory.getTotalQuantity("Potion", ItemType.CONSUMABLE, 9));
  }

  @Test
  void shouldReturnZeroTotalQuantityWhenEmptyOrInvalid() {
    InventoryComponent inventory = new InventoryComponent(0, 5);

    assertEquals(0, inventory.getTotalQuantity("Gold", ItemType.CURRENCY, 9));
    assertEquals(0, inventory.getTotalQuantity(null, ItemType.CURRENCY, 9));
    assertEquals(0, inventory.getTotalQuantity("  ", ItemType.CURRENCY, 9));
    assertEquals(0, inventory.getTotalQuantity((Item) null));
  }

  @Test
  void shouldSplitStackIntoLowestEmptySlot() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(9, 9);
    inventory.addItem(goldItem);

    assertEquals(2, inventory.splitStack(1, 4));

    assertSame(goldItem, inventory.getItem(1));
    assertEquals(5, inventory.getItem(1).getQuantity());
    assertEquals(4, inventory.getItem(2).getQuantity());
    assertNotSame(goldItem, inventory.getItem(2));
    assertEquals(2, inventory.getOccupiedSlots());
  }

  @Test
  void shouldReuseLowestEmptySlotWhenSplitting() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    inventory.addItem(gold(9, 9));
    inventory.addItem(new Item("Sword", ItemType.WEAPON, 1, 1));
    inventory.addItem(gold(9, 9));
    inventory.removeItem(1);

    assertEquals(1, inventory.splitStack(3, 4));

    assertEquals(5, inventory.getItem(3).getQuantity());
    assertEquals(4, inventory.getItem(1).getQuantity());
    assertTrue(inventory.containsItem(2));
  }

  @Test
  void shouldRejectInvalidSplitRequestsWithoutMutation() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(9, 9);
    inventory.addItem(goldItem);

    assertEquals(-1, inventory.splitStack(0, 4));
    assertEquals(-1, inventory.splitStack(2, 4));
    assertEquals(-1, inventory.splitStack(1, 0));
    assertEquals(-1, inventory.splitStack(1, -1));
    assertEquals(-1, inventory.splitStack(1, 9));
    assertEquals(-1, inventory.splitStack(1, 10));

    assertSame(goldItem, inventory.getItem(1));
    assertEquals(9, goldItem.getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
  }

  @Test
  void shouldRejectSplitWhenInventoryIsFull() {
    InventoryComponent inventory = new InventoryComponent(0, 5);
    Item goldItem = gold(9, 9);
    inventory.addItem(goldItem);
    for (int slot = 2; slot <= 5; slot++) {
      inventory.addItem(gold(9, 9));
    }

    assertEquals(-1, inventory.splitStack(1, 4));
    assertEquals(9, goldItem.getQuantity());
    assertTrue(inventory.isFull());
  }

  private static Item potion(int quantity, int maxQuantity) {
    return new Item("Potion", ItemType.CONSUMABLE, quantity, maxQuantity);
  }

  private static Item gold(int quantity, int maxQuantity) {
    return new Item("Gold", ItemType.CURRENCY, quantity, maxQuantity);
  }
}
