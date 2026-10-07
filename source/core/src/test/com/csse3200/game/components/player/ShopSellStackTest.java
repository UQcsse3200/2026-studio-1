package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link ShopComponent#sellItem(int)} on stacked items: one sale takes exactly one unit from
 * the stack and refunds that one unit's sell price.
 */
@ExtendWith(GameExtension.class)
class ShopSellStackTest {
  private static final int START_GOLD = 100;
  private static final int SWORD_SELL_PRICE = 8;
  private static final int SLOT = 1;

  @Test
  void shouldRemoveOnlyOneUnitFromStack() {
    InventoryComponent inventory = inventoryWithSwords(3);
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertEquals(2, inventory.getItem(SLOT).getQuantity(), "selling once must leave 2 of 3 swords");
  }

  @Test
  void shouldRefundOneUnitPriceForOneSale() {
    InventoryComponent inventory = inventoryWithSwords(3);
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertEquals(
        START_GOLD + SWORD_SELL_PRICE,
        inventory.getGold(),
        "one sale refunds the price of one sword, not the whole stack");
  }

  @Test
  void shouldKeepSlotOccupiedWhileUnitsRemain() {
    InventoryComponent inventory = inventoryWithSwords(2);
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertEquals(1, inventory.getOccupiedSlots(), "the slot must stay occupied by the last sword");
  }

  @Test
  void shouldReportSuccessWhenSellingOneUnitOfStack() {
    InventoryComponent inventory = inventoryWithSwords(3);
    ShopComponent shop = shopFor(inventory);

    assertTrue(shop.sellItem(SLOT), "selling one unit of a stack should succeed");
  }

  @Test
  void shouldClearSlotWhenSellingLastUnit() {
    InventoryComponent inventory = inventoryWithSwords(1);
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertNull(inventory.getItem(SLOT), "selling the last unit must free the slot");
  }

  @Test
  void shouldRefundEveryUnitWhenSellingStackOneByOne() {
    InventoryComponent inventory = inventoryWithSwords(3);
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);
    shop.sellItem(SLOT);
    shop.sellItem(SLOT);

    assertEquals(
        START_GOLD + 3 * SWORD_SELL_PRICE,
        inventory.getGold(),
        "three sales must refund exactly three swords");
  }

  @Test
  void shouldRejectSellOnceStackIsSoldOut() {
    InventoryComponent inventory = inventoryWithSwords(1);
    ShopComponent shop = shopFor(inventory);
    shop.sellItem(SLOT);

    assertFalse(shop.sellItem(SLOT), "nothing is left to sell in an empty slot");
  }

  @Test
  void shouldNotChangeGoldWhenSellIsRejectedOnEmptySlot() {
    InventoryComponent inventory = inventoryWithSwords(1);
    ShopComponent shop = shopFor(inventory);
    shop.sellItem(SLOT);
    int goldAfterSale = inventory.getGold();

    shop.sellItem(SLOT);

    assertEquals(goldAfterSale, inventory.getGold(), "a rejected sell must not refund gold");
  }

  @Test
  void shouldTriggerInventoryChangedOncePerUnitSold() {
    InventoryComponent inventory = inventoryWithSwords(3);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    AtomicInteger events = new AtomicInteger();
    entity.getEvents().addListener("inventoryChanged", events::incrementAndGet);
    events.set(0);

    shop.sellItem(SLOT);

    assertEquals(
        2, events.get(), "one sale changes the stack and the gold, so two inventoryChanged events");
  }

  private static InventoryComponent inventoryWithSwords(int quantity) {
    InventoryComponent inventory = new InventoryComponent(START_GOLD);
    Item swords = new Item("Sword", ItemType.WEAPON, quantity, 9);
    swords.setSellPrice(SWORD_SELL_PRICE);
    inventory.addItem(swords);
    return inventory;
  }

  private static ShopComponent shopFor(InventoryComponent inventory) {
    ShopComponent shop = new ShopComponent();
    new Entity().addComponent(inventory).addComponent(shop);
    return shop;
  }
}
