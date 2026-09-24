package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopComponentTest {
  @Test
  void shouldBuyItemSuccessDeductsGoldAndAddsCopy() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item catalogPotion = potion(1, 10);
    shop.setItemListing(1, itemListing(catalogPotion, 10));

    assertTrue(shop.buyItem(1));

    assertEquals(90, inventory.getGold());
    Item bought = inventory.getItem(1);
    assertNotNull(bought);
    assertNotSame(catalogPotion, bought);
    assertEquals("Potion", bought.getName());
    assertEquals(ItemType.CONSUMABLE, bought.getItemType());
    assertEquals(1, bought.getQuantity());
    assertEquals(6, bought.getSellPrice());
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldRejectBuyItemWhenNotEnoughGold() {
    InventoryComponent inventory = new InventoryComponent(5);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    assertFalse(shop.buyItem(1));

    assertEquals(5, inventory.getGold());
    assertEquals(0, inventory.getOccupiedSlots());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldRejectBuyItemWhenInventoryFullUntilSellFreesSlot() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    fillInventory(inventory);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));
    shop.setItemListing(2, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20));

    assertTrue(inventory.isFull());
    assertFalse(shop.buyItem(1));
    assertEquals(100, inventory.getGold());
    assertEquals(5, inventory.getOccupiedSlots());

    assertTrue(shop.sellItem(1));
    assertNotNull(shop.getItemListing(2));
    assertNotNull(shop.getItemListing(1));
    assertFalse(inventory.isFull());
    assertTrue(shop.buyItem(1));
    assertEquals(90, inventory.getGold());
    assertEquals("Potion", inventory.getItem(1).getName());
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldBuyStackableItemWhenOccupiedInventoryStillHasStackSpace() {
    InventoryComponent inventory = new InventoryComponent(100, 1);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item existing = potion(5, 10);
    existing.setSellPrice(6);
    inventory.addItem(existing);
    shop.setItemListing(1, itemListing(potion(3, 10), 10));

    assertTrue(inventory.isFull());
    assertTrue(shop.buyItem(1));

    assertEquals(90, inventory.getGold());
    assertEquals(8, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldRejectBuyWhenPurchaseWouldLeaveLeftoverWithoutChargingGold() {
    InventoryComponent inventory = new InventoryComponent(100, 1);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    inventory.addItem(potion(10, 10));
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    assertFalse(shop.buyItem(1));

    assertEquals(100, inventory.getGold());
    assertEquals(10, inventory.getItem(1).getQuantity());
    assertEquals(1, inventory.getOccupiedSlots());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldRejectBuyOnInvalidCatalogSlot() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));
    shop.setUpgradeListing(1, UpgradeListing("Speed Boost", 15));
    shop.setPetListing(1, petListing("Wolf", 20));

    assertFalse(shop.buyItem(0));
    assertFalse(shop.buyItem(-1));
    assertFalse(shop.buyItem(99));
    assertFalse(shop.buyUpgrade(0));
    assertFalse(shop.buyUpgrade(99));
    assertFalse(shop.buyPet(0));
    assertFalse(shop.buyPet(99));

    assertEquals(100, inventory.getGold());
    assertEquals(0, inventory.getOccupiedSlots());
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
  }

  @Test
  void shouldNotBuyMissingItemListing() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    assertFalse(shop.buyItem(1));
    assertFalse(shop.buyUpgrade(1));
    assertFalse(shop.buyPet(1));

    assertEquals(100, inventory.getGold());
    assertEquals(0, inventory.getOccupiedSlots());
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
  }

  @Test
  void shouldRejectBuyAndSellWhenShopHasNoInventory() {
    ShopComponent shop = new ShopComponent();
    new Entity().addComponent(shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));
    shop.setUpgradeListing(1, UpgradeListing("Speed Boost", 15));
    shop.setPetListing(1, petListing("Wolf", 20));

    assertFalse(shop.buyItem(1));
    assertFalse(shop.sellItem(1));
    assertFalse(shop.buyUpgrade(1));
    assertFalse(shop.buyPet(1));
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
  }

  @Test
  void shouldSeedDefaultCatalogPlaceholders() {
    ShopComponent shop = new ShopComponent();

    assertSame(shop, shop.seedDefaultCatalog());

    assertEquals("Potion", shop.getItemListing(1).getProduct().getName());
    assertEquals(ItemType.CONSUMABLE, shop.getItemListing(1).getProduct().getItemType());
    assertEquals(10, shop.getItemListing(1).getBuyPrice());

    assertEquals("Sword", shop.getItemListing(2).getProduct().getName());
    assertEquals(ItemType.WEAPON, shop.getItemListing(2).getProduct().getItemType());

    assertEquals("Health Upgrade", shop.getUpgradeListing(1).getProduct().getName());
    assertEquals(15, shop.getUpgradeListing(1).getBuyPrice());

    assertEquals("Bird", shop.getPetListing(1).getProduct().getName());
    assertEquals("Bat", shop.getPetListing(2).getProduct().getName());
    assertEquals("Spirit", shop.getPetListing(3).getProduct().getName());

    assertEquals(2, shop.getItemCatalog().size());
    assertEquals(1, shop.getUpgradeCatalog().size());
    assertEquals(3, shop.getPetCatalog().size());
  }

  @Test
  void shouldSellItemSuccessClearsSlotAndRefundsGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);
    sword.setSellPrice(8);
    inventory.addItem(sword);
    shop.setItemListing(1, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20));

    assertTrue(shop.sellItem(1));

    assertNull(inventory.getItem(1));
    assertEquals(0, inventory.getOccupiedSlots());
    assertEquals(108, inventory.getGold());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldRejectSellOnEmptyOrInvalidPlayerSlot() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    assertFalse(shop.sellItem(1));
    assertFalse(shop.sellItem(0));
    assertFalse(shop.sellItem(-1));
    assertFalse(shop.sellItem(99));

    assertEquals(100, inventory.getGold());
  }

  @Test
  void shouldSellItemWithoutMatchingBuyListing() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item relic = new Item("Mystery Relic", ItemType.WEAPON, 1, 1);
    relic.setSellPrice(12);
    inventory.addItem(relic);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    assertEquals(12, shop.getSellPrice(relic));
    assertTrue(shop.sellItem(1));

    assertNull(inventory.getItem(1));
    assertEquals(112, inventory.getGold());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldReturnZeroSellPriceForNullItem() {
    ShopComponent shop = new ShopComponent();

    assertEquals(0, shop.getSellPrice(null));
  }

  @Test
  void shouldBuyUpgradeSuccessWithoutUsingItemSlots() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    inventory.addItem(potion(1, 10));
    int occupied = inventory.getOccupiedSlots();
    ShopComponent.ShopListing<ShopComponent.Upgrade> listing = UpgradeListing("Speed Boost", 15);
    shop.setUpgradeListing(1, listing);

    assertTrue(shop.buyUpgrade(1));

    assertEquals(85, inventory.getGold());
    assertEquals(occupied, inventory.getOccupiedSlots());
    assertEquals(1, shop.getPurchasedUpgrades().size());
    assertSame(listing.getProduct(), shop.getPurchasedUpgrades().get(0));
    assertNotNull(shop.getUpgradeListing(1));
  }

  @Test
  void shouldBuyPetSuccessWithoutUsingItemSlots() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    inventory.addItem(potion(1, 10));
    int occupied = inventory.getOccupiedSlots();
    ShopComponent.ShopListing<ShopComponent.Pet> listing = petListing("Wolf", 20);
    shop.setPetListing(1, listing);

    assertTrue(shop.buyPet(1));

    assertEquals(80, inventory.getGold());
    assertEquals(occupied, inventory.getOccupiedSlots());
    assertEquals(1, shop.getPurchasedPets().size());
    assertSame(listing.getProduct(), shop.getPurchasedPets().get(0));
    assertNotNull(shop.getPetListing(1));
  }

  @Test
  void shouldRejectUpgradeAndPetWhenNotEnoughGold() {
    InventoryComponent inventory = new InventoryComponent(10);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setUpgradeListing(1, UpgradeListing("Speed Boost", 15));
    shop.setPetListing(1, petListing("Wolf", 20));

    assertFalse(shop.buyUpgrade(1));
    assertFalse(shop.buyPet(1));

    assertEquals(10, inventory.getGold());
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
  }

  @Test
  void shouldTriggerUpgradePurchasedEvent() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    AtomicInteger eventCount = new AtomicInteger();
    entity.getEvents().addListener("upgradePurchased", eventCount::incrementAndGet);
    shop.setUpgradeListing(1, UpgradeListing("Health Upgrade", 40));

    assertTrue(shop.buyUpgrade(1));
    assertEquals(1, eventCount.get());
  }

  @Test
  void shouldTriggerPetPurchasedEvent() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);

    ShopComponent.Pet dog = new ShopComponent.Pet("Dog");
    shop.setPetListing(1, new ShopComponent.ShopListing<>(dog, 50));

    AtomicInteger eventCount = new AtomicInteger();

    entity
        .getEvents()
        .addListener(
            "petPurchased",
            (ShopComponent.Pet purchasedPet) -> {
              assertSame(dog, purchasedPet);
              eventCount.incrementAndGet();
            });

    assertTrue(shop.buyPet(1));
    assertEquals(1, eventCount.get());
  }

  @Test
  void shouldRejectBuyAndSellWhenShopHasNoEntity() {
    ShopComponent shop = new ShopComponent();
    shop.setItemListing(1, itemListing(potion(1, 10), 10));
    shop.setUpgradeListing(1, UpgradeListing("Speed Boost", 15));
    shop.setPetListing(1, petListing("Wolf", 20));

    assertFalse(shop.buyItem(1));
    assertFalse(shop.sellItem(1));
    assertFalse(shop.buyUpgrade(1));
    assertFalse(shop.buyPet(1));
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
  }

  @Test
  void shouldRejectCurrencyItemListing() {
    ShopComponent shop = new ShopComponent();
    Item coins = new Item("Coins", ItemType.CURRENCY, 10, 100);

    assertFalse(shop.setItemListing(1, itemListing(coins, 1)));
    assertNull(shop.getItemListing(1));
    assertTrue(shop.getItemCatalog().isEmpty());
  }

  @Test
  void shouldReturnUnmodifiableCatalogs() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.ShopListing<Item> potionListing = itemListing(potion(1, 10), 10);
    ShopComponent.ShopListing<ShopComponent.Upgrade> UpgradeListing =
        UpgradeListing("Speed Boost", 15);
    ShopComponent.ShopListing<ShopComponent.Pet> petListing = petListing("Wolf", 20);
    shop.setItemListing(1, potionListing);
    shop.setUpgradeListing(1, UpgradeListing);
    shop.setPetListing(1, petListing);

    ShopComponent.ShopListing<Item> extraItem = itemListing(potion(1, 10), 1);
    ShopComponent.ShopListing<ShopComponent.Upgrade> extraUpgrade = UpgradeListing("Shield", 5);
    ShopComponent.ShopListing<ShopComponent.Pet> extraPet = petListing("Cat", 5);

    assertThrows(
        UnsupportedOperationException.class, () -> shop.getItemCatalog().put(2, extraItem));
    assertThrows(UnsupportedOperationException.class, () -> shop.getItemCatalog().remove(1));
    assertThrows(
        UnsupportedOperationException.class, () -> shop.getUpgradeCatalog().put(2, extraUpgrade));
    assertThrows(UnsupportedOperationException.class, () -> shop.getUpgradeCatalog().remove(1));
    assertThrows(UnsupportedOperationException.class, () -> shop.getPetCatalog().put(2, extraPet));
    assertThrows(UnsupportedOperationException.class, () -> shop.getPetCatalog().remove(1));

    assertEquals(1, shop.getItemCatalog().size());
    assertEquals(1, shop.getUpgradeCatalog().size());
    assertEquals(1, shop.getPetCatalog().size());
    assertSame(potionListing, shop.getItemListing(1));
    assertSame(UpgradeListing, shop.getUpgradeListing(1));
    assertSame(petListing, shop.getPetListing(1));
  }

  @Test
  void shouldReturnUnmodifiablePurchasedUpgradeList() {
    ShopComponent shop = new ShopComponent();

    assertThrows(
        UnsupportedOperationException.class,
        () -> shop.getPurchasedUpgrades().add(new ShopComponent.Upgrade("Health Upgrade")));
  }

  @Test
  void shouldReturnUnmodifiablePurchasedPetList() {
    ShopComponent shop = new ShopComponent();

    assertThrows(
        UnsupportedOperationException.class,
        () -> shop.getPurchasedPets().add(new ShopComponent.Pet("Dog")));
  }

  private static void attach(InventoryComponent inventory, ShopComponent shop) {
    new Entity().addComponent(inventory).addComponent(shop);
  }

  private static void fillInventory(InventoryComponent inventory) {
    inventory.addItem(new Item("Sword", ItemType.WEAPON, 1, 1));
    inventory.addItem(new Item("Bow", ItemType.WEAPON, 1, 1));
    inventory.addItem(new Item("Axe", ItemType.WEAPON, 1, 1));
    inventory.addItem(new Item("Dagger", ItemType.WEAPON, 1, 1));
    inventory.addItem(new Item("Mace", ItemType.WEAPON, 1, 1));
  }

  private static Item potion(int quantity, int maxQuantity) {
    return new Item("Potion", ItemType.CONSUMABLE, quantity, maxQuantity);
  }

  private static ShopComponent.ShopListing<Item> itemListing(Item product, int buyPrice) {
    return new ShopComponent.ShopListing<>(product, buyPrice);
  }

  private static ShopComponent.ShopListing<ShopComponent.Upgrade> UpgradeListing(
      String name, int buyPrice) {
    return new ShopComponent.ShopListing<>(new ShopComponent.Upgrade(name), buyPrice);
  }

  private static ShopComponent.ShopListing<ShopComponent.Pet> petListing(
      String name, int buyPrice) {
    return new ShopComponent.ShopListing<>(new ShopComponent.Pet(name), buyPrice);
  }

  private static void fillCatalogs(ShopComponent shop) {
    for (int slot = 1; slot <= ShopComponent.MAX_CATALOG_SLOTS; slot++) {
      assertTrue(shop.setItemListing(slot, itemListing(potion(1, 5), 20)));
      assertTrue(shop.setUpgradeListing(slot, UpgradeListing("Upgrade " + slot, 5)));
      assertTrue(shop.setPetListing(slot, petListing("Pet " + slot, 5)));
    }
  }

  @Test
  void shouldCreateShopListing() {
    Item item = potion(1, 5);

    ShopComponent.ShopListing<Item> listing = new ShopComponent.ShopListing<>(item, 20);

    assertSame(item, listing.getProduct());
    assertEquals(20, listing.getBuyPrice());
  }

  @Test
  void shouldRejectInvalidShopListing() {
    Item item = potion(1, 5);

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.ShopListing<>(null, 20));

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.ShopListing<>(item, -1));
  }

  @Test
  void shouldAllowZeroShopPrices() {
    Item item = potion(1, 5);

    ShopComponent.ShopListing<Item> listing = new ShopComponent.ShopListing<>(item, 0);

    assertEquals(0, listing.getBuyPrice());
  }

  @Test
  void shouldSetAndGetItemListing() {
    ShopComponent shop = new ShopComponent();
    Item item = potion(1, 5);

    ShopComponent.ShopListing<Item> listing = new ShopComponent.ShopListing<>(item, 20);

    assertTrue(shop.setItemListing(1, listing));
    assertSame(listing, shop.getItemListing(1));
  }

  @Test
  void shouldRejectInvalidItemSlot() {
    ShopComponent shop = new ShopComponent();
    Item item = potion(1, 5);

    ShopComponent.ShopListing<Item> listing = new ShopComponent.ShopListing<>(item, 20);

    assertFalse(shop.setItemListing(0, listing));
    assertFalse(shop.setItemListing(-1, listing));
    assertNull(shop.getItemListing(0));
    assertNull(shop.getItemListing(-1));
  }

  @Test
  void shouldClearItemListing() {
    ShopComponent shop = new ShopComponent();
    Item item = potion(1, 5);

    ShopComponent.ShopListing<Item> listing = new ShopComponent.ShopListing<>(item, 20);

    shop.setItemListing(1, listing);

    assertTrue(shop.setItemListing(1, null));
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldReplaceItemListing() {
    ShopComponent shop = new ShopComponent();

    ShopComponent.ShopListing<Item> first = new ShopComponent.ShopListing<>(potion(1, 5), 20);

    ShopComponent.ShopListing<Item> second = new ShopComponent.ShopListing<>(potion(1, 5), 30);

    shop.setItemListing(1, first);
    shop.setItemListing(1, second);

    assertSame(second, shop.getItemListing(1));
  }

  @Test
  void shouldSetGetAndClearUpgradeListing() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.Upgrade upgrade = new ShopComponent.Upgrade("Health Upgrade");

    ShopComponent.ShopListing<ShopComponent.Upgrade> listing =
        new ShopComponent.ShopListing<>(upgrade, 50);

    assertTrue(shop.setUpgradeListing(1, listing));
    assertSame(listing, shop.getUpgradeListing(1));

    assertTrue(shop.setUpgradeListing(1, null));
    assertNull(shop.getUpgradeListing(1));
  }

  @Test
  void shouldRejectInvalidUpgradeSlot() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.Upgrade upgrade = new ShopComponent.Upgrade("Health Upgrade");

    ShopComponent.ShopListing<ShopComponent.Upgrade> listing =
        new ShopComponent.ShopListing<>(upgrade, 50);

    assertFalse(shop.setUpgradeListing(0, listing));
    assertFalse(shop.setUpgradeListing(-1, listing));
  }

  @Test
  void shouldSetGetAndClearPetListing() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.Pet pet = new ShopComponent.Pet("Dog");

    ShopComponent.ShopListing<ShopComponent.Pet> listing =
        new ShopComponent.ShopListing<>(pet, 100);

    assertTrue(shop.setPetListing(1, listing));
    assertSame(listing, shop.getPetListing(1));

    assertTrue(shop.setPetListing(1, null));
    assertNull(shop.getPetListing(1));
  }

  @Test
  void shouldRejectInvalidPetSlot() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.Pet pet = new ShopComponent.Pet("Dog");

    ShopComponent.ShopListing<ShopComponent.Pet> listing =
        new ShopComponent.ShopListing<>(pet, 100);

    assertFalse(shop.setPetListing(0, listing));
    assertFalse(shop.setPetListing(-1, listing));
  }

  @Test
  void shouldAcceptLastCatalogSlot() {
    ShopComponent shop = new ShopComponent();
    int last = ShopComponent.MAX_CATALOG_SLOTS;

    assertTrue(shop.setItemListing(last, itemListing(potion(1, 5), 20)));
    assertTrue(shop.setUpgradeListing(last, UpgradeListing("Speed Boost", 15)));
    assertTrue(shop.setPetListing(last, petListing("Wolf", 20)));

    assertNotNull(shop.getItemListing(last));
    assertNotNull(shop.getUpgradeListing(last));
    assertNotNull(shop.getPetListing(last));
  }

  @Test
  void shouldRejectCatalogSlotAboveMax() {
    ShopComponent shop = new ShopComponent();
    int tooHigh = ShopComponent.MAX_CATALOG_SLOTS + 1;

    assertFalse(shop.setItemListing(tooHigh, itemListing(potion(1, 5), 20)));
    assertFalse(shop.setUpgradeListing(tooHigh, UpgradeListing("Speed Boost", 15)));
    assertFalse(shop.setPetListing(tooHigh, petListing("Wolf", 20)));
    assertFalse(shop.setItemListing(tooHigh, null));

    assertNull(shop.getItemListing(tooHigh));
    assertNull(shop.getUpgradeListing(tooHigh));
    assertNull(shop.getPetListing(tooHigh));
    assertTrue(shop.getItemCatalog().isEmpty());
    assertTrue(shop.getUpgradeCatalog().isEmpty());
    assertTrue(shop.getPetCatalog().isEmpty());
  }

  @Test
  void shouldReplaceListingWhenCatalogIsFull() {
    ShopComponent shop = new ShopComponent();
    fillCatalogs(shop);

    ShopComponent.ShopListing<Item> itemReplacement = itemListing(potion(2, 5), 30);
    ShopComponent.ShopListing<ShopComponent.Upgrade> upgradeReplacement =
        UpgradeListing("Shield", 25);
    ShopComponent.ShopListing<ShopComponent.Pet> petReplacement = petListing("Cat", 40);

    assertTrue(shop.setItemListing(1, itemReplacement));
    assertTrue(shop.setUpgradeListing(1, upgradeReplacement));
    assertTrue(shop.setPetListing(1, petReplacement));

    assertSame(itemReplacement, shop.getItemListing(1));
    assertSame(upgradeReplacement, shop.getUpgradeListing(1));
    assertSame(petReplacement, shop.getPetListing(1));
    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getItemCatalog().size());
    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getUpgradeCatalog().size());
    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getPetCatalog().size());
  }

  @Test
  void shouldRejectNewListingWhenCatalogIsFull() {
    ShopComponent shop = new ShopComponent();
    fillCatalogs(shop);

    int tooHigh = ShopComponent.MAX_CATALOG_SLOTS + 1;
    assertFalse(shop.setItemListing(tooHigh, itemListing(potion(1, 5), 20)));
    assertFalse(shop.setUpgradeListing(tooHigh, UpgradeListing("Shield", 25)));
    assertFalse(shop.setPetListing(tooHigh, petListing("Cat", 40)));

    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getItemCatalog().size());
    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getUpgradeCatalog().size());
    assertEquals(ShopComponent.MAX_CATALOG_SLOTS, shop.getPetCatalog().size());
  }

  @Test
  void shouldRejectInvalidUpgradeName() {
    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Upgrade(null));

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Upgrade(""));

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Upgrade("   "));
  }

  @Test
  void shouldCreateValidUpgrade() {
    ShopComponent.Upgrade upgrade = new ShopComponent.Upgrade("Health Upgrade");

    assertEquals("Health Upgrade", upgrade.getName());
  }

  @Test
  void shouldRejectInvalidPetName() {
    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Pet(null));

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Pet(""));

    assertThrows(IllegalArgumentException.class, () -> new ShopComponent.Pet("   "));
  }

  @Test
  void shouldCreateValidPet() {
    ShopComponent.Pet pet = new ShopComponent.Pet("Dog");

    assertEquals("Dog", pet.getName());
  }

  @Test
  void shouldNotChangeItemCatalogWhenInventoryIsLooted() {
    InventoryComponent inventory = new InventoryComponent(50);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    ShopComponent.ShopListing<Item> listing = itemListing(potion(1, 10), 10);
    shop.setItemListing(1, listing);

    AtomicInteger inventoryEvents = new AtomicInteger();
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("inventoryChanged", inventoryEvents::incrementAndGet);
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    inventory.addItem(new Item("Loot Potion", ItemType.CONSUMABLE, 1, 9));
    inventory.addGold(7);

    assertSame(listing, shop.getItemListing(1));
    assertEquals(1, shop.getItemCatalog().size());
    assertEquals(2, inventoryEvents.get());
    assertEquals(0, shopEvents.get());
  }

  @Test
  void shouldEmitInventoryAndShopEventsOnSuccessfulBuy() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    AtomicInteger inventoryEvents = new AtomicInteger();
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("inventoryChanged", inventoryEvents::incrementAndGet);
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    assertTrue(shop.buyItem(1));
    assertEquals(2, inventoryEvents.get());
    assertEquals(1, shopEvents.get());
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldEmitInventoryAndShopEventsOnSuccessfulSell() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    Item sword = new Item("Sword", ItemType.WEAPON, 1, 1);
    sword.setSellPrice(8);
    inventory.addItem(sword);
    shop.setItemListing(1, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20));

    AtomicInteger inventoryEvents = new AtomicInteger();
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("inventoryChanged", inventoryEvents::incrementAndGet);
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    assertTrue(shop.sellItem(1));
    assertEquals(2, inventoryEvents.get());
    assertEquals(0, shopEvents.get());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldNotEmitShopChangedWhenSellingWithoutCatalogMutation() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    Item relic = new Item("Mystery Relic", ItemType.WEAPON, 1, 1);
    relic.setSellPrice(1);
    inventory.addItem(relic);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    assertTrue(shop.sellItem(1));
    assertEquals(0, shopEvents.get());
    assertNull(inventory.getItem(1));
    assertEquals(101, inventory.getGold());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldNotEmitShopChangedWhenBuyIsRejected() {
    InventoryComponent inventory = new InventoryComponent(5);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    assertFalse(shop.buyItem(1));
    assertEquals(0, shopEvents.get());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldKeepBuyListingsWhenSellingMatchingItems() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item firstSword = new Item("Sword", ItemType.WEAPON, 1, 1);
    firstSword.setSellPrice(4);
    Item secondSword = new Item("Sword", ItemType.WEAPON, 1, 1);
    secondSword.setSellPrice(4);
    inventory.addItem(firstSword);
    inventory.addItem(secondSword);
    shop.setItemListing(3, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20));
    shop.setItemListing(1, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20));

    assertTrue(shop.sellItem(1));
    assertNotNull(shop.getItemListing(1));
    assertNotNull(shop.getItemListing(3));
    assertEquals(104, inventory.getGold());

    assertTrue(shop.sellItem(2));
    assertNotNull(shop.getItemListing(1));
    assertNotNull(shop.getItemListing(3));
    assertEquals(108, inventory.getGold());
  }

  @Test
  void shouldSellBoughtItemAtSixtyPercent() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10));

    assertTrue(shop.buyItem(1));
    assertNull(shop.getItemListing(1));
    assertEquals(90, inventory.getGold());

    assertEquals(6, shop.getSellPrice(inventory.getItem(1)));
    assertTrue(shop.sellItem(1));
    assertNull(inventory.getItem(1));
    assertEquals(96, inventory.getGold());
    assertNull(shop.getItemListing(1));
  }

  @Test
  void shouldNotifyShopChangedOnGenuineSetterAddReplaceAndRemove() {
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(shop);
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    ShopComponent.ShopListing<Item> first = itemListing(potion(1, 5), 20);
    assertTrue(shop.setItemListing(1, first));
    assertEquals(1, shopEvents.get());

    ShopComponent.ShopListing<Item> second = itemListing(potion(1, 5), 30);
    assertTrue(shop.setItemListing(1, second));
    assertEquals(2, shopEvents.get());

    assertTrue(shop.setItemListing(1, second));
    assertEquals(2, shopEvents.get());

    assertTrue(shop.setItemListing(1, null));
    assertEquals(3, shopEvents.get());

    assertTrue(shop.setItemListing(1, null));
    assertEquals(3, shopEvents.get());
  }

  @Test
  void shouldNotNotifyShopChangedOnInvalidSetterOperations() {
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(shop);
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    Item coins = new Item("Coins", ItemType.CURRENCY, 10, 100);
    assertFalse(shop.setItemListing(1, itemListing(coins, 1)));
    assertFalse(shop.setItemListing(0, itemListing(potion(1, 5), 20)));
    assertFalse(
        shop.setItemListing(ShopComponent.MAX_CATALOG_SLOTS + 1, itemListing(potion(1, 5), 20)));
    assertTrue(shop.setItemListing(2, null));
    assertEquals(0, shopEvents.get());

    fillCatalogs(shop);
    int afterFill = shopEvents.get();
    int tooHigh = ShopComponent.MAX_CATALOG_SLOTS + 1;
    assertFalse(shop.setItemListing(tooHigh, itemListing(potion(1, 5), 20)));
    assertEquals(afterFill, shopEvents.get());
  }

  @Test
  void shouldNotNotifyWhenSeedingUnattachedShop() {
    ShopComponent shop = new ShopComponent();
    assertSame(shop, shop.seedDefaultCatalog());
    assertNotNull(shop.getItemListing(1));
  }

  @Test
  void shouldLeaveGamblingCatalogsUnsetUntilSeed() {
    ShopComponent shop = new ShopComponent();

    assertNull(shop.getGamblingCatalogs());
    assertNull(shop.getStandardCatalog());
    assertNull(shop.getPremiumCatalog());
    assertEquals(0, shop.getSpinPrice(GamblingCatalogs.CatalogId.STANDARD));
    assertEquals(0, shop.getSpinPrice(null));
    assertTrue(shop.getPrizes(null).isEmpty());
    assertTrue(shop.getPrizes(GamblingCatalogs.CatalogId.PREMIUM).isEmpty());
    assertNull(shop.getPrize(GamblingCatalogs.CatalogId.STANDARD, 1));
  }

  @Test
  void shouldSeedStandardAndPremiumGamblingCatalogs() {
    ShopComponent shop = new ShopComponent();

    assertSame(shop, shop.seedDefaultCatalog());
    assertSame(shop.getGamblingCatalogs().getStandard(), shop.getStandardCatalog());
    assertSame(shop.getGamblingCatalogs().getPremium(), shop.getPremiumCatalog());
    assertEquals(20, shop.getSpinPrice(GamblingCatalogs.CatalogId.STANDARD));
    assertEquals(50, shop.getSpinPrice(GamblingCatalogs.CatalogId.PREMIUM));

    assertSeededPrize(
        shop, GamblingCatalogs.CatalogId.STANDARD, 1, "Gamble Potion", ItemType.CONSUMABLE, 40);
    assertSeededPrize(
        shop, GamblingCatalogs.CatalogId.STANDARD, 2, "Gamble Sword", ItemType.WEAPON, 25);
    assertSeededUpgrade(shop, GamblingCatalogs.CatalogId.STANDARD, 3, "Gamble Health", 15);
    assertSeededPet(shop, GamblingCatalogs.CatalogId.STANDARD, 4, "Gamble Bird", 12);
    assertSeededPrize(
        shop, GamblingCatalogs.CatalogId.STANDARD, 5, "Gamble Herb", ItemType.CONSUMABLE, 8);

    assertSeededPrize(
        shop, GamblingCatalogs.CatalogId.PREMIUM, 1, "Premium Potion", ItemType.CONSUMABLE, 20);
    assertSeededUpgrade(shop, GamblingCatalogs.CatalogId.PREMIUM, 2, "Premium Health", 20);
    assertSeededPet(shop, GamblingCatalogs.CatalogId.PREMIUM, 3, "Premium Spirit", 20);
    assertSeededPrize(
        shop, GamblingCatalogs.CatalogId.PREMIUM, 4, "Premium Sword", ItemType.WEAPON, 25);
    assertSeededPet(shop, GamblingCatalogs.CatalogId.PREMIUM, 5, "Premium Bat", 15);

    assertEquals(5, shop.getPrizes(GamblingCatalogs.CatalogId.STANDARD).size());
    assertEquals(5, shop.getPrizes(GamblingCatalogs.CatalogId.PREMIUM).size());
    assertNull(shop.getPrize(GamblingCatalogs.CatalogId.STANDARD, 0));
    assertNull(shop.getPrize(GamblingCatalogs.CatalogId.PREMIUM, 6));
  }

  @Test
  void shouldRejectMutatingReturnedPrizeMap() {
    ShopComponent shop = new ShopComponent().seedDefaultCatalog();
    GamblingCatalogs.PrizeEntry<?> replacement =
        new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Other"), 1);

    assertThrows(
        UnsupportedOperationException.class,
        () -> shop.getPrizes(GamblingCatalogs.CatalogId.STANDARD).put(1, replacement));
    assertEquals(
        "Gamble Potion", itemPrize(shop, GamblingCatalogs.CatalogId.STANDARD, 1).getName());
  }

  @Test
  void shouldNotRollOrMutateWhenBuySpinIsStub() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(inventory).addComponent(shop);
    shop.seedDefaultCatalog();
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);
    GamblingCatalogs.PrizeEntry<?> standardPrize =
        shop.getPrize(GamblingCatalogs.CatalogId.STANDARD, 1);

    assertNull(shop.buySpin(GamblingCatalogs.CatalogId.STANDARD));
    assertNull(shop.buySpin(GamblingCatalogs.CatalogId.PREMIUM));
    assertNull(shop.buySpin(null));

    assertEquals(100, inventory.getGold());
    assertEquals(0, inventory.getOccupiedSlots());
    assertTrue(shop.getPurchasedUpgrades().isEmpty());
    assertTrue(shop.getPurchasedPets().isEmpty());
    assertSame(standardPrize, shop.getPrize(GamblingCatalogs.CatalogId.STANDARD, 1));
    assertEquals(0, shopEvents.get());
  }

  @Test
  void shouldNotifyShopChangedWhenSeededOnAttachedShop() {
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(shop);
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    shop.seedDefaultCatalog();

    assertNotNull(shop.getStandardCatalog());
    assertNotNull(shop.getPremiumCatalog());
    // Six existing listing setters plus one gambling install.
    assertEquals(7, shopEvents.get());
  }

  @Test
  void shouldNotifyShopChangedWhenGamblingPriceOrPrizeChanges() {
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(shop);
    shop.seedDefaultCatalog();
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);

    assertTrue(shop.setSpinPrice(GamblingCatalogs.CatalogId.STANDARD, 20));
    assertEquals(0, shopEvents.get());

    assertTrue(shop.setSpinPrice(GamblingCatalogs.CatalogId.STANDARD, 30));
    assertEquals(1, shopEvents.get());
    assertEquals(30, shop.getSpinPrice(GamblingCatalogs.CatalogId.STANDARD));

    GamblingCatalogs.PrizeEntry<?> current = shop.getPrize(GamblingCatalogs.CatalogId.PREMIUM, 5);
    assertTrue(shop.setPrize(GamblingCatalogs.CatalogId.PREMIUM, 5, current));
    assertEquals(1, shopEvents.get());

    GamblingCatalogs.PrizeEntry<?> replacement =
        new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Premium Owl"), 3);
    assertTrue(shop.setPrize(GamblingCatalogs.CatalogId.PREMIUM, 5, replacement));
    assertEquals(2, shopEvents.get());
    assertEquals(5, shop.getPrizes(GamblingCatalogs.CatalogId.PREMIUM).size());
    assertSame(replacement, shop.getPrize(GamblingCatalogs.CatalogId.PREMIUM, 5));
  }

  @Test
  void shouldNotNotifyWhenGamblingReplaceIsInvalid() {
    ShopComponent shop = new ShopComponent();
    Entity entity = new Entity().addComponent(shop);
    AtomicInteger shopEvents = new AtomicInteger();
    entity.getEvents().addListener("shopChanged", shopEvents::incrementAndGet);
    GamblingCatalogs.PrizeEntry<?> prize =
        new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Upgrade("Nope"), 1);

    assertFalse(shop.setSpinPrice(GamblingCatalogs.CatalogId.STANDARD, 10));
    assertFalse(shop.setPrize(GamblingCatalogs.CatalogId.STANDARD, 1, prize));
    assertEquals(0, shopEvents.get());

    shop.seedDefaultCatalog();
    shopEvents.set(0);

    assertFalse(shop.setSpinPrice(null, 10));
    assertFalse(shop.setSpinPrice(GamblingCatalogs.CatalogId.PREMIUM, -1));
    assertFalse(shop.setPrize(null, 1, prize));
    assertFalse(shop.setPrize(GamblingCatalogs.CatalogId.STANDARD, 1, null));
    assertFalse(shop.setPrize(GamblingCatalogs.CatalogId.STANDARD, 0, prize));
    assertFalse(shop.setPrize(GamblingCatalogs.CatalogId.STANDARD, 6, prize));
    assertEquals(
        "Gamble Potion", itemPrize(shop, GamblingCatalogs.CatalogId.STANDARD, 1).getName());
    assertEquals(20, shop.getSpinPrice(GamblingCatalogs.CatalogId.STANDARD));
    assertEquals(0, shopEvents.get());
  }

  @Test
  void shouldRejectGamblingCatalogsThatAreNotFiveSlots() {
    assertThrows(
        IllegalArgumentException.class, () -> new GamblingCatalogs.PrizeEntry<Item>(null, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Bird"), 0));

    Map<Integer, GamblingCatalogs.PrizeEntry<?>> four = new HashMap<>();
    four.put(1, new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Bird"), 1));
    assertThrows(IllegalArgumentException.class, () -> new GamblingCatalogs.SpinCatalog(0, four));
    assertThrows(
        IllegalArgumentException.class, () -> new GamblingCatalogs.SpinCatalog(-1, fivePrizes()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GamblingCatalogs(null, new GamblingCatalogs.SpinCatalog(0, fivePrizes())));
  }

  private static Map<Integer, GamblingCatalogs.PrizeEntry<?>> fivePrizes() {
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes = new HashMap<>();
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      prizes.put(slot, new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Bird"), 1));
    }
    return prizes;
  }

  private static void assertSeededPrize(
      ShopComponent shop,
      GamblingCatalogs.CatalogId catalogId,
      int slot,
      String name,
      ItemType itemType,
      int weight) {
    GamblingCatalogs.PrizeEntry<?> entry = shop.getPrize(catalogId, slot);
    assertNotNull(entry);
    assertEquals(weight, entry.getWeight());
    assertTrue(entry.getWeight() > 0);
    Item item = (Item) entry.getProduct();
    assertEquals(name, item.getName());
    assertEquals(itemType, item.getItemType());
  }

  private static void assertSeededUpgrade(
      ShopComponent shop, GamblingCatalogs.CatalogId catalogId, int slot, String name, int weight) {
    GamblingCatalogs.PrizeEntry<?> entry = shop.getPrize(catalogId, slot);
    assertNotNull(entry);
    assertEquals(weight, entry.getWeight());
    assertEquals(name, ((ShopComponent.Upgrade) entry.getProduct()).getName());
  }

  private static void assertSeededPet(
      ShopComponent shop, GamblingCatalogs.CatalogId catalogId, int slot, String name, int weight) {
    GamblingCatalogs.PrizeEntry<?> entry = shop.getPrize(catalogId, slot);
    assertNotNull(entry);
    assertEquals(weight, entry.getWeight());
    assertEquals(name, ((ShopComponent.Pet) entry.getProduct()).getName());
  }

  private static Item itemPrize(
      ShopComponent shop, GamblingCatalogs.CatalogId catalogId, int slot) {
    return (Item) shop.getPrize(catalogId, slot).getProduct();
  }
}
