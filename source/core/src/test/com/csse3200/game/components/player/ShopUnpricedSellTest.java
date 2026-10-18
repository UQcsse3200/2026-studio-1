package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link ShopComponent#sellItem(int)} and {@link ShopComponent#isSellable(Item)} for items
 * with no sell price: an unpriced item is not a valid sale, so selling it must change nothing.
 */
@ExtendWith(GameExtension.class)
class ShopUnpricedSellTest {
  private static final int START_GOLD = 100;
  private static final int SLOT = 1;

  @Test
  void shouldRejectSellWhenItemHasNoSellPrice() {
    ShopComponent shop = shopFor(inventoryWith(relic(0)));

    assertFalse(shop.sellItem(SLOT), "an item with no sell price is not a valid sale");
  }

  @Test
  void shouldKeepItemWhenSellPriceIsZero() {
    InventoryComponent inventory = inventoryWith(relic(0));
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertNotNull(inventory.getItem(SLOT), "a rejected sell must leave the item in its slot");
  }

  @Test
  void shouldKeepGoldWhenSellPriceIsZero() {
    InventoryComponent inventory = inventoryWith(relic(0));
    ShopComponent shop = shopFor(inventory);

    shop.sellItem(SLOT);

    assertEquals(START_GOLD, inventory.getGold(), "a rejected sell must not change gold");
  }

  @Test
  void shouldNotTriggerInventoryChangedWhenSellIsRejected() {
    InventoryComponent inventory = inventoryWith(relic(0));
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    AtomicInteger events = new AtomicInteger();
    entity.getEvents().addListener("inventoryChanged", events::incrementAndGet);
    events.set(0);

    shop.sellItem(SLOT);

    assertEquals(0, events.get(), "a rejected sell must not notify inventory listeners");
  }

  @Test
  void shouldSellWhenSellPriceIsOne() {
    InventoryComponent inventory = inventoryWith(relic(1));
    ShopComponent shop = shopFor(inventory);

    assertTrue(shop.sellItem(SLOT), "the smallest positive price is still a valid sale");
    assertEquals(START_GOLD + 1, inventory.getGold(), "the sale refunds exactly one gold");
  }

  @Test
  void shouldReportUnpricedItemAsNotSellable() {
    ShopComponent shop = new ShopComponent();

    assertFalse(shop.isSellable(relic(0)), "an item with no sell price is not sellable");
  }

  @Test
  void shouldReportPricedItemAsSellable() {
    ShopComponent shop = new ShopComponent();

    assertTrue(shop.isSellable(relic(1)), "an item with a positive sell price is sellable");
  }

  @Test
  void shouldReportNullItemAsNotSellable() {
    ShopComponent shop = new ShopComponent();

    assertFalse(shop.isSellable(null), "there is nothing to sell when the item is null");
  }

  private static Item relic(int sellPrice) {
    Item relic = new Item("Relic", ItemType.WEAPON, 1, 1);
    relic.setSellPrice(sellPrice);
    return relic;
  }

  private static InventoryComponent inventoryWith(Item item) {
    InventoryComponent inventory = new InventoryComponent(START_GOLD);
    inventory.addItem(item);
    return inventory;
  }

  private static ShopComponent shopFor(InventoryComponent inventory) {
    ShopComponent shop = new ShopComponent();
    new Entity().addComponent(inventory).addComponent(shop);
    return shop;
  }
}
