package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link ShopComponent#buySpin}. Every shop uses the same Standard test catalog, priced at
 * {@link #SPIN_PRICE}, with weights 40/25/20/10/5 so a {@link FixedRandom} draw picks the slot:
 *
 * <pre>
 * slot 1  draw  0..39  Potion item (stacks to 9)
 * slot 2  draw 40..64  15 gold
 * slot 3  draw 65..84  Basic Sword (typed WeaponItem, does not stack)
 * slot 4  draw 85..94  50 gold
 * slot 5  draw 95..99  100 gold
 * </pre>
 */
@ExtendWith(GameExtension.class)
class ShopGamblingTest {
  private static final GamblingCatalogs.CatalogId STANDARD = GamblingCatalogs.CatalogId.STANDARD;
  private static final int SPIN_PRICE = 20;
  private static final int POTION_DRAW = 0;
  private static final int GOLD_15_DRAW = 40;
  private static final int SWORD_DRAW = 65;
  private static final int SLOT_4_DRAW = 85;
  private static final int SLOT_5_DRAW = 95;

  @Test
  void shouldChargeSpinPriceWhenPrizeIsItem() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));

    shop.buySpin(STANDARD);

    assertEquals(80, inventory.getGold(), "an item win should cost exactly the spin price");
  }

  @Test
  void shouldReturnTheRolledEntry() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(GOLD_15_DRAW));

    assertSame(
        shop.getPrize(STANDARD, 2),
        shop.buySpin(STANDARD),
        "buySpin should return the entry in the rolled slot so the wheel can land on it");
  }

  @Test
  void shouldAddRolledItemToInventory() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));

    shop.buySpin(STANDARD);

    assertEquals("Potion", nameIn(inventory, 1), "the won potion should be in the first slot");
  }

  @Test
  void shouldKeepWonWeaponTyped() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(SWORD_DRAW));

    shop.buySpin(STANDARD);

    assertInstanceOf(
        WeaponItem.class, inventory.getItem(1), "a won weapon must stay usable as a WeaponItem");
  }

  @Test
  void shouldStackRepeatedItemWinsWithoutSharingTheCatalogProduct() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));

    shop.buySpin(STANDARD);
    shop.buySpin(STANDARD);

    assertEquals(
        2, inventory.getItem(1).getQuantity(), "two potion wins should stack into one slot");
  }

  @Test
  void shouldLeaveCatalogUnchangedAfterSpins() {
    ShopComponent shop = newShop(new InventoryComponent(1_000), new FixedRandom(POTION_DRAW));
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> before = Map.copyOf(shop.getPrizes(STANDARD));

    for (int i = 0; i < 5; i++) {
      shop.buySpin(STANDARD);
    }

    assertEquals(before, shop.getPrizes(STANDARD), "spinning must not change the prize pool");
  }

  @Test
  void shouldAddGoldPrizeAndChargeSpinPrice() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(GOLD_15_DRAW));

    shop.buySpin(STANDARD);

    assertEquals(95, inventory.getGold(), "gold win should be 100 - 20 price + 15 prize");
  }

  @Test
  void shouldSpinWithExactlyTheSpinPrice() {
    InventoryComponent inventory = new InventoryComponent(SPIN_PRICE);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));

    assertNotNull(shop.buySpin(STANDARD), "gold equal to the spin price is enough");
  }

  @Test
  void shouldSpinForFreeWhenSpinPriceIsZero() {
    InventoryComponent inventory = new InventoryComponent(0);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));
    shop.setSpinPrice(STANDARD, 0);

    assertNotNull(shop.buySpin(STANDARD), "a free ticket should spin with no gold");
  }

  @Test
  void shouldRejectSpinWhenOneGoldShort() {
    InventoryComponent inventory = new InventoryComponent(SPIN_PRICE - 1);
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));
    State before = State.of(inventory, shop);

    assertNull(shop.buySpin(STANDARD), "spin should be rejected one gold short");
    assertEquals(before, State.of(inventory, shop), "a rejected spin must change nothing");
  }

  @Test
  void shouldNotRollWhenGoldIsShort() {
    FixedRandom random = new FixedRandom(POTION_DRAW);
    ShopComponent shop = newShop(new InventoryComponent(0), random);

    shop.buySpin(STANDARD);

    assertTrue(random.bounds.isEmpty(), "no roll should happen before the gold check passes");
  }

  /**
   * Bug-exposing: if only the rolled prize were checked, a full inventory would cancel every item
   * result for free while gold results still paid out. The spin must be rejected up front instead.
   */
  @Test
  void shouldRejectSpinWhenAnyItemPrizeCannotFitEvenIfRollWouldBeGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    fillWithUnstackableItems(inventory, 5);
    FixedRandom random = new FixedRandom(GOLD_15_DRAW);
    ShopComponent shop = newShop(inventory, random);
    State before = State.of(inventory, shop);

    assertNull(shop.buySpin(STANDARD), "a full inventory should block the spin");
    assertEquals(before, State.of(inventory, shop), "a rejected spin must change nothing");
    assertTrue(random.bounds.isEmpty(), "the roll must not happen, so no result is revealed");
  }

  @Test
  void shouldSpinWhenFullInventoryCanStillStackEveryItemPrize() {
    InventoryComponent inventory = new InventoryComponent(100);
    fillWithUnstackableItems(inventory, 4);
    inventory.addItem(new Item("Potion", ItemType.CONSUMABLE, 1, 9));
    ShopComponent shop = newShop(inventory, new FixedRandom(POTION_DRAW));
    shop.setPrize(STANDARD, 3, new GamblingCatalogs.PrizeEntry<>(gold(5), 20));

    assertNotNull(
        shop.buySpin(STANDARD), "a potion can stack onto the existing potion in a full inventory");
  }

  @Test
  void shouldRejectCatalogWithUnsupportedPrize() {
    InventoryComponent inventory = new InventoryComponent(100);
    FixedRandom random = new FixedRandom(POTION_DRAW);
    ShopComponent shop = newShop(inventory, random);
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>("not a product", 5));
    State before = State.of(inventory, shop);

    assertNull(shop.buySpin(STANDARD), "an unknown product type cannot be delivered");
    assertEquals(before, State.of(inventory, shop), "a rejected spin must change nothing");
    assertTrue(random.bounds.isEmpty(), "invalid catalog data should be caught before the roll");
  }

  @Test
  void shouldRejectNullCatalogId() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(POTION_DRAW));

    assertNull(shop.buySpin(null), "a null catalog id is not a ticket");
  }

  @Test
  void shouldRejectSpinBeforeCatalogsAreSeeded() {
    ShopComponent shop = new ShopComponent(new FixedRandom(POTION_DRAW));
    new Entity().addComponent(new InventoryComponent(100)).addComponent(shop);

    assertNull(shop.buySpin(STANDARD), "there is nothing to spin before seeding");
  }

  @Test
  void shouldRejectSpinWithoutInventory() {
    ShopComponent shop = new ShopComponent(new FixedRandom(POTION_DRAW)).seedDefaultCatalog();
    new Entity().addComponent(shop);

    assertNull(shop.buySpin(STANDARD), "a spin needs an inventory to pay and receive the prize");
  }

  @Test
  void shouldRejectSpinWhenShopIsUnattached() {
    ShopComponent shop = new ShopComponent(new FixedRandom(POTION_DRAW)).seedDefaultCatalog();

    assertNull(shop.buySpin(STANDARD), "an unattached shop has no inventory");
  }

  @Test
  void shouldRecordWonPet() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(SLOT_5_DRAW));
    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>(bird, 5));

    shop.buySpin(STANDARD);

    assertEquals(List.of(bird), shop.getPurchasedPets(), "a won pet is owned like a bought one");
  }

  @Test
  void shouldTriggerPetPurchasedWithWonPet() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(SLOT_5_DRAW));
    ShopComponent.Pet bird = new ShopComponent.Pet("Bird");
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>(bird, 5));
    List<ShopComponent.Pet> activated = new ArrayList<>();
    shop.getEntity()
        .getEvents()
        .addListener("petPurchased", (ShopComponent.Pet pet) -> activated.add(pet));

    shop.buySpin(STANDARD);

    assertEquals(
        List.of(bird), activated, "PetManagerComponent listens to petPurchased to spawn the pet");
  }

  @Test
  void shouldChargeOnlySpinPriceForPetPrize() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(SLOT_5_DRAW));
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Bird"), 5));

    shop.buySpin(STANDARD);

    assertEquals(80, inventory.getGold(), "a won pet must not also charge its shop price");
  }

  /** Listeners such as the shop gold label must see gold after the spin price is paid. */
  @Test
  void shouldTriggerPetPurchasedAfterChargingSpinPrice() {
    InventoryComponent inventory = new InventoryComponent(100);
    ShopComponent shop = newShop(inventory, new FixedRandom(SLOT_5_DRAW));
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Pet("Bird"), 5));
    AtomicInteger goldSeen = new AtomicInteger(-1);
    shop.getEntity()
        .getEvents()
        .addListener("petPurchased", (ShopComponent.Pet pet) -> goldSeen.set(inventory.getGold()));

    shop.buySpin(STANDARD);

    assertEquals(80, goldSeen.get(), "petPurchased should fire after the spin price is deducted");
  }

  @Test
  void shouldRecordWonUpgrade() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(SLOT_4_DRAW));
    ShopComponent.Upgrade health = new ShopComponent.Upgrade("Premium Health");
    shop.setPrize(STANDARD, 4, new GamblingCatalogs.PrizeEntry<>(health, 10));

    shop.buySpin(STANDARD);

    assertEquals(
        List.of(health), shop.getPurchasedUpgrades(), "a won Upgrade is owned like a bought one");
  }

  @Test
  void shouldTriggerUpgradePurchasedOnceForWonUpgrade() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(SLOT_4_DRAW));
    shop.setPrize(
        STANDARD, 4, new GamblingCatalogs.PrizeEntry<>(new ShopComponent.Upgrade("Health"), 10));
    AtomicInteger events = new AtomicInteger();
    shop.getEntity().getEvents().addListener("upgradePurchased", events::incrementAndGet);

    shop.buySpin(STANDARD);

    assertEquals(1, events.get(), "upgradePurchased should fire exactly once");
  }

  @Test
  void shouldTriggerGamblingSpunWithCatalogAndRolledSlot() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(SWORD_DRAW));
    List<String> spins = new ArrayList<>();
    shop.getEntity()
        .getEvents()
        .addListener(
            "gamblingSpun",
            (GamblingCatalogs.CatalogId id, Integer slot) -> spins.add(id + " " + slot));

    shop.buySpin(STANDARD);

    assertEquals(
        List.of("STANDARD 3"), spins, "the wheel needs the ticket and the slot to land on");
  }

  @Test
  void shouldNotTriggerGamblingSpunWhenSpinIsRejected() {
    ShopComponent shop = newShop(new InventoryComponent(0), new FixedRandom(POTION_DRAW));
    AtomicInteger events = new AtomicInteger();
    shop.getEntity()
        .getEvents()
        .addListener(
            "gamblingSpun",
            (GamblingCatalogs.CatalogId id, Integer slot) -> events.incrementAndGet());

    shop.buySpin(STANDARD);

    assertEquals(0, events.get(), "a rejected spin must not start the wheel");
  }

  @Test
  void shouldNotTriggerShopChangedWhenSpinning() {
    ShopComponent shop = newShop(new InventoryComponent(100), new FixedRandom(POTION_DRAW));
    AtomicInteger events = new AtomicInteger();
    shop.getEntity().getEvents().addListener("shopChanged", events::incrementAndGet);

    shop.buySpin(STANDARD);

    assertEquals(0, events.get(), "a spin does not change the catalogs");
  }

  /** The real seeded tickets contain pets and Upgrades, so they only work once those deliver. */
  @ParameterizedTest
  @EnumSource(GamblingCatalogs.CatalogId.class)
  void shouldSpinSeededTicketUpToItsRarestPrize(GamblingCatalogs.CatalogId catalogId) {
    ShopComponent shop = new ShopComponent(new FixedRandom(99));
    new Entity().addComponent(new InventoryComponent(100)).addComponent(shop);
    shop.seedDefaultCatalog();

    assertInstanceOf(
        ShopComponent.Pet.class,
        shop.buySpin(catalogId).getProduct(),
        catalogId + " ticket's rarest slot should be its pet");
  }

  @Test
  void shouldRejectNullRandom() {
    assertThrows(IllegalArgumentException.class, () -> new ShopComponent(null));
  }

  /**
   * Creates a seeded shop attached to {@code inventory} and replaces the Standard ticket with the
   * test catalog described on this class.
   */
  private static ShopComponent newShop(InventoryComponent inventory, FixedRandom random) {
    ShopComponent shop = new ShopComponent(random);
    new Entity().addComponent(inventory).addComponent(shop);
    shop.seedDefaultCatalog();

    WeaponGenerator weapons = new WeaponGenerator();
    shop.setSpinPrice(STANDARD, SPIN_PRICE);
    shop.setPrize(
        STANDARD,
        1,
        new GamblingCatalogs.PrizeEntry<>(
            new GamblingCatalogs.ItemPrize(
                "Potion", ItemType.CONSUMABLE, () -> new Item("Potion", ItemType.CONSUMABLE, 1, 9)),
            40));
    shop.setPrize(STANDARD, 2, new GamblingCatalogs.PrizeEntry<>(gold(15), 25));
    shop.setPrize(
        STANDARD,
        3,
        new GamblingCatalogs.PrizeEntry<>(
            new GamblingCatalogs.ItemPrize(
                "Basic Sword", ItemType.WEAPON, () -> weapons.generateWeapon(WeaponType.SWORD, 1)),
            20));
    shop.setPrize(STANDARD, 4, new GamblingCatalogs.PrizeEntry<>(gold(50), 10));
    shop.setPrize(STANDARD, 5, new GamblingCatalogs.PrizeEntry<>(gold(100), 5));
    return shop;
  }

  private static GamblingCatalogs.GoldPrize gold(int amount) {
    return new GamblingCatalogs.GoldPrize(amount);
  }

  private static void fillWithUnstackableItems(InventoryComponent inventory, int count) {
    for (int i = 1; i <= count; i++) {
      inventory.addItem(new Item("Junk " + i, ItemType.WEAPON, 1, 1));
    }
  }

  private static String nameIn(InventoryComponent inventory, int slot) {
    Item item = inventory.getItem(slot);
    return item == null ? null : item.getName();
  }

  /** Everything a spin could change, so a rejected spin can be checked in one assertion. */
  private record State(
      int gold,
      Map<Integer, String> slots,
      int purchasedPets,
      int purchasedUpgrades,
      Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes) {
    static State of(InventoryComponent inventory, ShopComponent shop) {
      Map<Integer, String> slots = new TreeMap<>();
      inventory
          .getInventorySlots()
          .forEach((slot, item) -> slots.put(slot, item.getName() + " x" + item.getQuantity()));
      return new State(
          inventory.getGold(),
          slots,
          shop.getPurchasedPets().size(),
          shop.getPurchasedUpgrades().size(),
          Map.copyOf(shop.getPrizes(STANDARD)));
    }
  }
}
