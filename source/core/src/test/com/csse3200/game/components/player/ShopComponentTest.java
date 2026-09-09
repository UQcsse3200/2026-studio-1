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
    shop.setItemListing(1, itemListing(catalogPotion, 10, 5));

    assertTrue(shop.buyItem(1));

    assertEquals(90, inventory.getGold());
    Item bought = inventory.getItem(1);
    assertNotNull(bought);
    assertNotSame(catalogPotion, bought);
    assertEquals("Potion", bought.getName());
    assertEquals(ItemType.CONSUMABLE, bought.getItemType());
    assertEquals(1, bought.getQuantity());
    assertSame(catalogPotion, shop.getItemListing(1).getProduct());
  }

  @Test
  void shouldRejectBuyItemWhenNotEnoughGold() {
    InventoryComponent inventory = new InventoryComponent(5);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));

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
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));
    shop.setItemListing(2, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20, 8));

    assertTrue(inventory.isFull());
    assertFalse(shop.buyItem(1));
    assertEquals(100, inventory.getGold());
    assertEquals(5, inventory.getOccupiedSlots());

    assertTrue(shop.sellItem(1));
    assertFalse(inventory.isFull());
    assertTrue(shop.buyItem(1));
    assertEquals(98, inventory.getGold());
    assertEquals("Potion", inventory.getItem(1).getName());
  }

  @Test
  void shouldRejectBuyOnInvalidCatalogSlot() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));
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
  void shouldSellItemSuccessClearsSlotAndRefundsGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    inventory.addItem(new Item("Sword", ItemType.WEAPON, 1, 1));
    shop.setItemListing(1, itemListing(new Item("Sword", ItemType.WEAPON, 1, 1), 20, 8));

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
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));

    assertFalse(shop.sellItem(1));
    assertFalse(shop.sellItem(0));
    assertFalse(shop.sellItem(-1));
    assertFalse(shop.sellItem(99));

    assertEquals(100, inventory.getGold());
  }

  @Test
  void shouldRejectSellWhenItemNotInCatalog() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);

    Item relic = new Item("Mystery Relic", ItemType.WEAPON, 1, 1);
    inventory.addItem(relic);
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));

    assertFalse(shop.sellItem(1));

    assertSame(relic, inventory.getItem(1));
    assertEquals(100, inventory.getGold());
  }

  @Test
  void shouldBuyUpgradeSuccessWithoutUsingItemSlots() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    inventory.addItem(potion(1, 10));
    int occupied = inventory.getOccupiedSlots();
    shop.setUpgradeListing(1, UpgradeListing("Speed Boost", 15));

    assertTrue(shop.buyUpgrade(1));

    assertEquals(85, inventory.getGold());
    assertEquals(occupied, inventory.getOccupiedSlots());
    assertEquals(1, shop.getPurchasedUpgrades().size());
    assertEquals("Speed Boost", shop.getPurchasedUpgrades().get(0).getName());
    assertNotNull(shop.getUpgradeListing(1));
  }

  @Test
  void shouldBuyPetSuccessWithoutUsingItemSlots() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = new ShopComponent();
    attach(inventory, shop);
    inventory.addItem(potion(1, 10));
    int occupied = inventory.getOccupiedSlots();
    shop.setPetListing(1, petListing("Wolf", 20));

    assertTrue(shop.buyPet(1));

    assertEquals(80, inventory.getGold());
    assertEquals(occupied, inventory.getOccupiedSlots());
    assertEquals(1, shop.getPurchasedPets().size());
    assertEquals("Wolf", shop.getPurchasedPets().get(0).getName());
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
  void shouldRejectBuyAndSellWhenShopHasNoEntity() {
    ShopComponent shop = new ShopComponent();
    shop.setItemListing(1, itemListing(potion(1, 10), 10, 5));
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

    assertFalse(shop.setItemListing(1, itemListing(coins, 1, 1)));
    assertNull(shop.getItemListing(1));
    assertTrue(shop.getItemCatalog().isEmpty());
  }

  @Test
  void shouldReturnUnmodifiableCatalogs() {
    ShopComponent shop = new ShopComponent();
    ShopComponent.ShopListing<Item> potionListing = itemListing(potion(1, 10), 10, 5);
    ShopComponent.ShopListing<ShopComponent.Upgrade> UpgradeListing =
        UpgradeListing("Speed Boost", 15);
    ShopComponent.ShopListing<ShopComponent.Pet> petListing = petListing("Wolf", 20);
    shop.setItemListing(1, potionListing);
    shop.setUpgradeListing(1, UpgradeListing);
    shop.setPetListing(1, petListing);

    ShopComponent.ShopListing<Item> extraItem = itemListing(potion(1, 10), 1, 1);
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

  private static ShopComponent.ShopListing<Item> itemListing(
      Item product, int buyPrice, int sellPrice) {
    return new ShopComponent.ShopListing<>(product, buyPrice, sellPrice);
  }

  private static ShopComponent.ShopListing<ShopComponent.Upgrade> UpgradeListing(
      String name, int buyPrice) {
    return new ShopComponent.ShopListing<>(new ShopComponent.Upgrade(name), buyPrice, 0);
  }

  private static ShopComponent.ShopListing<ShopComponent.Pet> petListing(
      String name, int buyPrice) {
    return new ShopComponent.ShopListing<>(new ShopComponent.Pet(name), buyPrice, 0);
  }
}
