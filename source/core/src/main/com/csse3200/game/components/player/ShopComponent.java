package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A player component that owns shop catalogs and buy/sell transactions.
 *
 * <p>Does not store gold or player items. Payments use the sibling {@link InventoryComponent} on
 * the same entity.
 *
 * <p>Item purchases deduct gold only when the copied product fits in full. A successful item buy
 * stamps {@code sellPrice} as 60% of the listing buy price onto the copied item, consumes that item
 * listing (one-use offer), and triggers {@code shopChanged}. Selling uses the player inventory and
 * refunds {@link Item#getSellPrice()}. Upgrade and pet purchases trigger {@code upgradePurchased}
 * and {@code petPurchased} and leave those listings in place.
 *
 * <p>Gambling catalogs (Standard and Premium) hold a spin price and five weighted prize entries,
 * installed by {@link #seedDefaultCatalog()}. Item prizes are {@link GamblingCatalogs.ItemPrize}
 * factories backed by the loot generators, so each win is a new typed item. {@link #buySpin} rolls
 * a prize by weight, delivers it, and charges the spin price, or changes nothing if the spin is
 * rejected. Replacing a spin price or prize on an attached shop triggers {@code shopChanged}.
 */
public class ShopComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(ShopComponent.class);

  /** Maximum occupied listings per catalog; matches the shop UI grid size. */
  public static final int MAX_CATALOG_SLOTS = 10;

  private final Map<Integer, ShopListing<Item>> itemCatalog;
  private final Map<Integer, ShopListing<Upgrade>> upgradeCatalog;
  private final Map<Integer, ShopListing<Pet>> petCatalog;
  private final List<Upgrade> purchasedUpgrades;
  private final List<Pet> purchasedPets;
  private GamblingCatalogs gamblingCatalogs;
  private ConsumableGenerator consumableGenerator;
  private WeaponGenerator weaponGenerator;
  private final Random random;

  /** Creates a shop with empty item, Upgrade, and pet catalogs. */
  public ShopComponent() {
    this(new Random());
  }

  /**
   * Creates a shop with empty catalogs that rolls gambling spins with the given random source.
   *
   * @param random random source for {@link #buySpin}; must be non-null
   * @throws IllegalArgumentException if {@code random} is null
   */
  public ShopComponent(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("Random must not be null.");
    }
    this.random = random;
    this.itemCatalog = new HashMap<>();
    this.upgradeCatalog = new HashMap<>();
    this.petCatalog = new HashMap<>();
    this.purchasedUpgrades = new ArrayList<>();
    this.purchasedPets = new ArrayList<>();
  }

  /**
   * Fills placeholder listings so the in-game shop is not empty.
   *
   * <p>Prices are small for playtest. Listings in these slots are replaced if already occupied.
   *
   * @return this shop, for chaining from {@code PlayerFactory}
   */
  public ShopComponent seedDefaultCatalog() {
    setItemListing(1, new ShopListing<>(new Item("Potion", ItemType.CONSUMABLE, 1, 9), 10));
    setItemListing(2, new ShopListing<>(new Item("Sword", ItemType.WEAPON, 1, 1), 20));
    setUpgradeListing(1, new ShopListing<>(new Upgrade("Health Upgrade"), 15));
    setPetListing(1, new ShopListing<>(new Pet("Bird"), 20));
    setPetListing(2, new ShopListing<>(new Pet("Bat"), 30));
    setPetListing(3, new ShopListing<>(new Pet("Spirit"), 40));
    this.gamblingCatalogs = new GamblingCatalogs(standardSpinCatalog(), premiumSpinCatalog());
    notifyShopChanged();
    return this;
  }

  /**
   * Sets or clears an item catalog listing.
   *
   * @param slot catalog slot; must be {@code 1}..{@link #MAX_CATALOG_SLOTS}
   * @param listing listing to store, or {@code null} to clear the slot
   * @return {@code true} if the slot was valid and updated
   */
  public boolean setItemListing(int slot, ShopListing<Item> listing) {
    if (listing != null && listing.getProduct().getItemType() == ItemType.CURRENCY) {
      return false;
    }
    return setListing(itemCatalog, slot, listing);
  }

  /**
   * Sets or clears an Upgrade catalog listing.
   *
   * @param slot catalog slot; must be {@code 1}..{@link #MAX_CATALOG_SLOTS}
   * @param listing listing to store, or {@code null} to clear the slot
   * @return {@code true} if the slot was valid and updated
   */
  public boolean setUpgradeListing(int slot, ShopListing<Upgrade> listing) {
    return setListing(upgradeCatalog, slot, listing);
  }

  /**
   * Sets or clears a pet catalog listing.
   *
   * @param slot catalog slot; must be {@code 1}..{@link #MAX_CATALOG_SLOTS}
   * @param listing listing to store, or {@code null} to clear the slot
   * @return {@code true} if the slot was valid and updated
   */
  public boolean setPetListing(int slot, ShopListing<Pet> listing) {
    return setListing(petCatalog, slot, listing);
  }

  /**
   * Returns the item listing in a catalog slot.
   *
   * @param slot catalog slot
   * @return the listing, or {@code null} if the slot is invalid or empty
   */
  public ShopListing<Item> getItemListing(int slot) {
    return getListing(itemCatalog, slot);
  }

  /**
   * Returns the Upgrade listing in a catalog slot.
   *
   * @param slot catalog slot
   * @return the listing, or {@code null} if the slot is invalid or empty
   */
  public ShopListing<Upgrade> getUpgradeListing(int slot) {
    return getListing(upgradeCatalog, slot);
  }

  /**
   * Returns the pet listing in a catalog slot.
   *
   * @param slot catalog slot
   * @return the listing, or {@code null} if the slot is invalid or empty
   */
  public ShopListing<Pet> getPetListing(int slot) {
    return getListing(petCatalog, slot);
  }

  /**
   * Returns an unmodifiable view of occupied item catalog slots.
   *
   * @return unmodifiable map of slot index to listing
   */
  public Map<Integer, ShopListing<Item>> getItemCatalog() {
    return Collections.unmodifiableMap(itemCatalog);
  }

  /**
   * Returns an unmodifiable view of occupied Upgrade catalog slots.
   *
   * @return unmodifiable map of slot index to listing
   */
  public Map<Integer, ShopListing<Upgrade>> getUpgradeCatalog() {
    return Collections.unmodifiableMap(upgradeCatalog);
  }

  /**
   * Returns an unmodifiable view of occupied pet catalog slots.
   *
   * @return unmodifiable map of slot index to listing
   */
  public Map<Integer, ShopListing<Pet>> getPetCatalog() {
    return Collections.unmodifiableMap(petCatalog);
  }

  /**
   * Returns the Standard and Premium spin catalogs.
   *
   * @return gambling catalogs, or {@code null} if {@link #seedDefaultCatalog()} has not run
   */
  public GamblingCatalogs getGamblingCatalogs() {
    return gamblingCatalogs;
  }

  /**
   * Returns the Standard spin catalog.
   *
   * @return Standard catalog, or {@code null} if the shop is not seeded
   */
  public GamblingCatalogs.SpinCatalog getStandardCatalog() {
    if (gamblingCatalogs == null) {
      return null;
    }
    return gamblingCatalogs.getStandard();
  }

  /**
   * Returns the Premium spin catalog.
   *
   * @return Premium catalog, or {@code null} if the shop is not seeded
   */
  public GamblingCatalogs.SpinCatalog getPremiumCatalog() {
    if (gamblingCatalogs == null) {
      return null;
    }
    return gamblingCatalogs.getPremium();
  }

  /**
   * Returns the spin price for a catalog.
   *
   * @param catalogId Standard or Premium; {@code null} is invalid
   * @return spin price, or {@code 0} if {@code catalogId} is null or the shop is not seeded
   */
  public int getSpinPrice(GamblingCatalogs.CatalogId catalogId) {
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (catalog == null) {
      return 0;
    }
    return catalog.getSpinPrice();
  }

  /**
   * Returns the prize map for a catalog.
   *
   * @param catalogId Standard or Premium; {@code null} is invalid
   * @return unmodifiable prize map, or an empty unmodifiable map if {@code catalogId} is null or
   *     the shop is not seeded
   */
  public Map<Integer, GamblingCatalogs.PrizeEntry<?>> getPrizes(
      GamblingCatalogs.CatalogId catalogId) {
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (catalog == null) {
      return Collections.emptyMap();
    }
    return catalog.getPrizes();
  }

  /**
   * Returns one prize entry.
   *
   * @param catalogId Standard or Premium; {@code null} is invalid
   * @param slot prize slot
   * @return the entry, or {@code null} if the shop is not seeded, {@code catalogId} is null, or
   *     {@code slot} is outside {@code 1}..{@code 5}
   */
  public GamblingCatalogs.PrizeEntry<?> getPrize(GamblingCatalogs.CatalogId catalogId, int slot) {
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (catalog == null) {
      return null;
    }
    return catalog.getPrize(slot);
  }

  /**
   * Replaces the spin price of a seeded catalog.
   *
   * @param catalogId Standard or Premium
   * @param spinPrice new price; must be {@code >= 0}
   * @return {@code false} if the shop is not seeded, {@code catalogId} is null, or {@code
   *     spinPrice} is negative; {@code true} if the price is stored
   */
  public boolean setSpinPrice(GamblingCatalogs.CatalogId catalogId, int spinPrice) {
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (catalog == null || spinPrice < 0) {
      return false;
    }
    if (catalog.replaceSpinPrice(spinPrice)) {
      notifyShopChanged();
    }
    return true;
  }

  /**
   * Replaces one prize in a seeded catalog. The catalog still has five prizes.
   *
   * @param catalogId Standard or Premium
   * @param slot prize slot; must be {@code 1}..{@code 5}
   * @param prize replacement entry; must be non-null
   * @return {@code false} if the shop is not seeded, {@code catalogId} or {@code prize} is null, or
   *     the slot is invalid; {@code true} if the slot still holds a prize
   */
  public boolean setPrize(
      GamblingCatalogs.CatalogId catalogId, int slot, GamblingCatalogs.PrizeEntry<?> prize) {
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (catalog == null || prize == null || !isPrizeSlot(slot)) {
      return false;
    }
    if (catalog.replacePrize(slot, prize)) {
      notifyShopChanged();
    }
    return true;
  }

  /**
   * Buys one spin from the Standard or Premium gambling catalog.
   *
   * <p>Every check runs before the roll, so a failed spin changes nothing: the inventory must
   * exist, the catalog must be seeded, every prize must be a supported product, the player must
   * afford {@code spinPrice}, and <em>every</em> item prize in the catalog must fit in the
   * inventory. Checking all item prizes, not just the rolled one, stops a full inventory from being
   * used to cancel item results for free.
   *
   * <p>On success the prize is chosen by weight ({@code P = weight / sum(weights)}, see {@link
   * GamblingRoller}), delivered, and then the spin price is deducted, matching {@link
   * #buyItem(int)}. An {@link GamblingCatalogs.ItemPrize} adds a new typed item to the inventory; a
   * {@link GamblingCatalogs.GoldPrize} adds gold; a {@link Pet} or {@link Upgrade} is recorded like
   * a purchase and triggers {@code petPurchased} or {@code upgradePurchased} without charging its
   * shop price. These events fire after the spin price is deducted, so listeners see the final
   * gold.
   *
   * <p>Finally {@code gamblingSpun} is triggered with the {@link GamblingCatalogs.CatalogId} and
   * the rolled slot ({@code 1}..{@code 5}) so the shop UI can animate the wheel to that slot; the
   * animation does not choose the prize. A rejected spin triggers no events.
   *
   * @param catalogId Standard or Premium
   * @return the chosen prize entry, or {@code null} if the spin was rejected
   */
  public GamblingCatalogs.PrizeEntry<?> buySpin(GamblingCatalogs.CatalogId catalogId) {
    InventoryComponent inventory = getInventory();
    GamblingCatalogs.SpinCatalog catalog = spinCatalog(catalogId);
    if (inventory == null || catalog == null || !hasOnlySupportedPrizes(catalog)) {
      return null;
    }

    int spinPrice = catalog.getSpinPrice();
    if (!inventory.hasGold(spinPrice) || !canReceiveAllItemPrizes(catalog, inventory)) {
      return null;
    }

    int slot = GamblingRoller.rollSlot(catalog, random);
    GamblingCatalogs.PrizeEntry<?> prize = catalog.getPrize(slot);
    if (!deliverPrize(prize.getProduct(), inventory)) {
      return null;
    }

    inventory.addGold(-spinPrice);
    notifyPrizeDelivered(prize.getProduct());
    if (entity != null) {
      entity.getEvents().trigger("gamblingSpun", catalogId, slot);
    }
    return prize;
  }

  /**
   * Buys the item in a catalog slot using the sibling inventory wallet and slots.
   *
   * <p>Gold is deducted only after the copied item fits in full ({@link
   * InventoryComponent#canFullyAdd(Item)}). Occupied inventories can still succeed when the product
   * stacks into an existing slot.
   *
   * @param catalogSlot item catalog slot
   * @return {@code true} if gold was deducted, a copy of the item was added with no leftover, and
   *     the selected listing was removed
   */
  public boolean buyItem(int catalogSlot) {
    InventoryComponent inventory = getInventory();
    if (inventory == null) {
      return false;
    }

    ShopListing<Item> listing = getItemListing(catalogSlot);
    if (listing == null) {
      return false;
    }

    Item copy = copyItem(listing.getProduct());
    copy.setSellPrice(sellPriceFromBuyPrice(listing.getBuyPrice()));
    if (!inventory.hasGold(listing.getBuyPrice()) || !inventory.canFullyAdd(copy)) {
      return false;
    }

    int leftover = inventory.addItem(copy);
    if (leftover != 0) {
      return false;
    }

    inventory.addGold(-listing.getBuyPrice());
    itemCatalog.remove(catalogSlot);
    notifyShopChanged();
    return true;
  }

  /**
   * Returns the gold refund for selling {@code item}.
   *
   * <p>Uses {@link Item#getSellPrice()} stored on the inventory item. Bought items are stamped at
   * purchase; looted items keep the value set by loot generation.
   *
   * @param item inventory item to price; {@code null} returns {@code 0}
   * @return sell refund in gold
   */
  public int getSellPrice(Item item) {
    if (item == null) {
      return 0;
    }
    return item.getSellPrice();
  }

  /**
   * Sells the entire stack in a player inventory slot and refunds {@link #getSellPrice(Item)}.
   *
   * <p>Does not require a matching BUY listing and does not change BUY stock.
   *
   * @param playerSlot inventory slot on the sibling {@link InventoryComponent}
   * @return {@code true} if the item was removed and gold was added
   */
  public boolean sellItem(int playerSlot) {
    InventoryComponent inventory = getInventory();
    if (inventory == null) {
      return false;
    }

    Item item = inventory.getItem(playerSlot);
    if (item == null) {
      return false;
    }

    int refund = getSellPrice(item);

    Item removed = inventory.removeItem(playerSlot);
    if (removed == null) {
      return false;
    }

    inventory.addGold(refund);
    return true;
  }

  /**
   * Buys the Upgrade in a catalog slot using gold only. Does not use item slots.
   *
   * <p>On success, records the purchase and triggers {@code upgradePurchased}. The catalog listing
   * stays.
   *
   * @param catalogSlot Upgrade catalog slot
   * @return {@code true} if gold was deducted and the purchase was recorded
   */
  public boolean buyUpgrade(int catalogSlot) {
    InventoryComponent inventory = getInventory();
    if (inventory == null) {
      return false;
    }

    ShopListing<Upgrade> listing = getUpgradeListing(catalogSlot);
    if (listing == null) {
      return false;
    }

    if (!inventory.hasGold(listing.getBuyPrice())) {
      return false;
    }

    inventory.addGold(-listing.getBuyPrice());
    purchasedUpgrades.add(listing.getProduct());
    notifyUpgradePurchased();
    return true;
  }

  /**
   * Buys the pet in a catalog slot using gold only. Does not use item slots.
   *
   * <p>On success, records the purchase and triggers {@code petPurchased}. Does not spawn a pet
   * entity.
   *
   * @param catalogSlot pet catalog slot
   * @return {@code true} if gold was deducted and the purchase was recorded
   */
  public boolean buyPet(int catalogSlot) {
    InventoryComponent inventory = getInventory();
    if (inventory == null) {
      return false;
    }

    ShopListing<Pet> listing = getPetListing(catalogSlot);
    if (listing == null) {
      return false;
    }

    if (!inventory.hasGold(listing.getBuyPrice())) {
      return false;
    }

    inventory.addGold(-listing.getBuyPrice());
    purchasedPets.add(listing.getProduct());
    notifyPetPurchased(listing.getProduct());
    return true;
  }

  /**
   * Returns an unmodifiable view of purchased Upgrades.
   *
   * @return purchased Upgrades in purchase order
   */
  public List<Upgrade> getPurchasedUpgrades() {
    return Collections.unmodifiableList(purchasedUpgrades);
  }

  /**
   * Returns an unmodifiable view of purchased pets.
   *
   * @return purchased pets in purchase order
   */
  public List<Pet> getPurchasedPets() {
    return Collections.unmodifiableList(purchasedPets);
  }

  /**
   * Returns the sibling inventory on the same entity.
   *
   * @return inventory, or {@code null} if this component is unattached or inventory is missing
   */
  private InventoryComponent getInventory() {
    if (entity == null) {
      return null;
    }
    return entity.getComponent(InventoryComponent.class);
  }

  /**
   * Returns the sell refund stamped onto a purchased item: 60% of {@code buyPrice}, using integer
   * division.
   *
   * @param buyPrice listing buy price
   * @return sell price in gold
   */
  private static int sellPriceFromBuyPrice(int buyPrice) {
    return (buyPrice * 3) / 5;
  }

  /** Triggers {@code shopChanged} when this component is attached to an entity. */
  private void notifyShopChanged() {
    if (entity != null) {
      entity.getEvents().trigger("shopChanged");
    }
  }

  /**
   * Looks up the Standard or Premium {@link GamblingCatalogs.SpinCatalog} after {@link
   * #seedDefaultCatalog()} has installed {@link #gamblingCatalogs}.
   *
   * <p>Read-only helper for getters and prize/price updates. Does not choose a prize or run a
   * transaction.
   *
   * @param catalogId Standard or Premium
   * @return that catalog, or {@code null} if gambling catalogs are not installed yet or {@code
   *     catalogId} is null
   */
  private GamblingCatalogs.SpinCatalog spinCatalog(GamblingCatalogs.CatalogId catalogId) {
    if (gamblingCatalogs == null || catalogId == null) {
      return null;
    }
    return gamblingCatalogs.get(catalogId);
  }

  /**
   * Returns whether every prize in a catalog is a product {@link #buySpin} can deliver.
   *
   * @param catalog catalog to check
   * @return {@code true} if every product is an item, gold, pet or Upgrade prize
   */
  private static boolean hasOnlySupportedPrizes(GamblingCatalogs.SpinCatalog catalog) {
    for (GamblingCatalogs.PrizeEntry<?> prize : catalog.getPrizes().values()) {
      Object product = prize.getProduct();
      if (!(product instanceof GamblingCatalogs.ItemPrize)
          && !(product instanceof GamblingCatalogs.GoldPrize)
          && !(product instanceof Pet)
          && !(product instanceof Upgrade)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns whether every item prize in a catalog would fit in the inventory. Each prize is checked
   * on its own because one spin awards exactly one prize.
   *
   * @param catalog catalog to check
   * @param inventory inventory that would receive the prize
   * @return {@code true} if any item prize could be rolled and fully added
   */
  private static boolean canReceiveAllItemPrizes(
      GamblingCatalogs.SpinCatalog catalog, InventoryComponent inventory) {
    for (GamblingCatalogs.PrizeEntry<?> prize : catalog.getPrizes().values()) {
      if (prize.getProduct() instanceof GamblingCatalogs.ItemPrize itemPrize
          && !inventory.canFullyAdd(itemPrize.create())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Awards one prize product. Does not trigger pet or Upgrade events; see {@link
   * #notifyPrizeDelivered}.
   *
   * @param product rolled prize product; already checked by {@link #hasOnlySupportedPrizes}
   * @param inventory inventory that receives items and gold
   * @return {@code true} if the product was delivered in full
   */
  private boolean deliverPrize(Object product, InventoryComponent inventory) {
    if (product instanceof Pet pet) {
      purchasedPets.add(pet);
      return true;
    }
    if (product instanceof Upgrade upgrade) {
      purchasedUpgrades.add(upgrade);
      return true;
    }
    if (product instanceof GamblingCatalogs.ItemPrize itemPrize) {
      int leftover = inventory.addItem(itemPrize.create());
      if (leftover != 0) {
        // Not expected: canReceiveAllItemPrizes checked this prize before the roll.
        logger.warn("Gambling item prize did not fit after pre-flight; spin not charged");
        return false;
      }
      return true;
    }
    GamblingCatalogs.GoldPrize goldPrize = (GamblingCatalogs.GoldPrize) product;
    return inventory.addGold(goldPrize.getAmount());
  }

  /**
   * Triggers the purchase event for a delivered pet or Upgrade prize. Items and gold already
   * trigger {@code inventoryChanged} from the inventory.
   *
   * @param product delivered prize product
   */
  private void notifyPrizeDelivered(Object product) {
    if (product instanceof Pet pet) {
      notifyPetPurchased(pet);
    } else if (product instanceof Upgrade) {
      notifyUpgradePurchased();
    }
  }

  /** Triggers {@code upgradePurchased} when this component is attached to an entity. */
  private void notifyUpgradePurchased() {
    if (entity != null) {
      entity.getEvents().trigger("upgradePurchased");
    }
  }

  /**
   * Triggers {@code petPurchased} when this component is attached to an entity.
   *
   * @param pet pet to activate
   */
  private void notifyPetPurchased(Pet pet) {
    if (entity != null) {
      entity.getEvents().trigger("petPurchased", pet);
    }
  }

  /**
   * Returns whether {@code slot} is a gambling prize index.
   *
   * @param slot prize slot
   * @return {@code true} if the slot is {@code 1}..{@code 5}
   */
  private static boolean isPrizeSlot(int slot) {
    return slot >= 1 && slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT;
  }

  /**
   * Builds the Standard ticket. Weights sum to 100, so each weight reads as a percentage.
   *
   * @return Standard catalog at spin price 20
   */
  private GamblingCatalogs.SpinCatalog standardSpinCatalog() {
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes = new HashMap<>();
    prizes.put(1, prize(consumablePrize("Health Potion", ConsumableType.HEALTH_POTION, 1), 40));
    prizes.put(2, prize(consumablePrize("Speed Potion", ConsumableType.SPEED_BUFF, 1), 25));
    prizes.put(3, prize(new GamblingCatalogs.GoldPrize(15), 20));
    prizes.put(4, prize(weaponPrize("Basic Sword", WeaponType.SWORD, 1), 10));
    prizes.put(5, prize(new Pet("Bird"), 5));
    return new GamblingCatalogs.SpinCatalog(20, prizes);
  }

  /**
   * Builds the Premium ticket. Weights sum to 100, so each weight reads as a percentage.
   *
   * @return Premium catalog at spin price 60
   */
  private GamblingCatalogs.SpinCatalog premiumSpinCatalog() {
    Map<Integer, GamblingCatalogs.PrizeEntry<?>> prizes = new HashMap<>();
    prizes.put(
        1,
        prize(consumablePrize("Regeneration Potion (Tier 2)", ConsumableType.REGENERATION, 2), 35));
    prizes.put(2, prize(new GamblingCatalogs.GoldPrize(50), 25));
    prizes.put(3, prize(weaponPrize("Basic Bow", WeaponType.BOW, 3), 20));
    prizes.put(4, prize(new Upgrade("Premium Health"), 15));
    prizes.put(5, prize(new Pet("Spirit"), 5));
    return new GamblingCatalogs.SpinCatalog(60, prizes);
  }

  /**
   * Creates an item prize that generates a new consumable for every win.
   *
   * @param displayName name shown on the wheel
   * @param type consumable to generate
   * @param tier loot tier
   * @return item prize
   */
  private GamblingCatalogs.ItemPrize consumablePrize(
      String displayName, ConsumableType type, int tier) {
    return new GamblingCatalogs.ItemPrize(
        displayName,
        ItemType.CONSUMABLE,
        () -> consumableGenerator().generateConsumable(type, tier));
  }

  /**
   * Creates an item prize that generates a new weapon for every win.
   *
   * @param displayName name shown on the wheel
   * @param type weapon to generate
   * @param tier loot tier
   * @return item prize
   */
  private GamblingCatalogs.ItemPrize weaponPrize(String displayName, WeaponType type, int tier) {
    return new GamblingCatalogs.ItemPrize(
        displayName, ItemType.WEAPON, () -> weaponGenerator().generateWeapon(type, tier));
  }

  /**
   * Returns the shared consumable generator, reading its configs on first use rather than at seed.
   *
   * @return consumable generator
   */
  private ConsumableGenerator consumableGenerator() {
    if (consumableGenerator == null) {
      consumableGenerator = new ConsumableGenerator();
    }
    return consumableGenerator;
  }

  /**
   * Returns the shared weapon generator.
   *
   * @return weapon generator
   */
  private WeaponGenerator weaponGenerator() {
    if (weaponGenerator == null) {
      weaponGenerator = new WeaponGenerator();
    }
    return weaponGenerator;
  }

  /**
   * Creates a weighted prize placeholder.
   *
   * @param product prize product
   * @param weight probability weight
   * @param <T> product type
   * @return prize entry
   */
  private static <T> GamblingCatalogs.PrizeEntry<T> prize(T product, int weight) {
    return new GamblingCatalogs.PrizeEntry<>(product, weight);
  }

  /**
   * Creates a new item instance from a catalog product so inventory does not share the listing
   * object.
   *
   * @param product catalog product
   * @return copy with the same name, type, quantity, and max quantity
   */
  private Item copyItem(Item product) {
    return new Item(
        product.getName(), product.getItemType(), product.getQuantity(), product.getMaxQuantity());
  }

  /**
   * Sets or clears a listing in a catalog map.
   *
   * <p>Rejects slots outside {@code 1}..{@link #MAX_CATALOG_SLOTS}. A new occupied slot is rejected
   * when the catalog is already full; replacing an existing slot is allowed.
   *
   * @param catalog catalog to update
   * @param slot catalog slot
   * @param listing listing to store, or {@code null} to clear
   * @param <T> product type
   * @return {@code true} if the slot was valid and updated
   */
  private <T> boolean setListing(
      Map<Integer, ShopListing<T>> catalog, int slot, ShopListing<T> listing) {
    if (!isCatalogSlot(slot)) {
      return false;
    }
    if (listing == null) {
      if (!catalog.containsKey(slot)) {
        return true;
      }
      catalog.remove(slot);
      notifyShopChanged();
      return true;
    }
    if (!catalog.containsKey(slot) && catalog.size() >= MAX_CATALOG_SLOTS) {
      return false;
    }
    ShopListing<T> previous = catalog.put(slot, listing);
    if (previous != listing) {
      notifyShopChanged();
    }
    return true;
  }

  /**
   * Returns a listing from a catalog map.
   *
   * @param catalog catalog to read
   * @param slot catalog slot
   * @param <T> product type
   * @return listing, or {@code null} if the slot is invalid or empty
   */
  private <T> ShopListing<T> getListing(Map<Integer, ShopListing<T>> catalog, int slot) {
    if (!isCatalogSlot(slot)) {
      return null;
    }
    return catalog.get(slot);
  }

  /**
   * Returns whether {@code slot} is a valid catalog index.
   *
   * @param slot catalog slot
   * @return {@code true} if the slot is {@code 1}..{@link #MAX_CATALOG_SLOTS}
   */
  private static boolean isCatalogSlot(int slot) {
    return slot >= 1 && slot <= MAX_CATALOG_SLOTS;
  }

  /**
   * Catalog data for a shop product. Nested on the player's shop component; not a {@link Component}
   * and not a wallet.
   *
   * @param <T> product type stored in this listing
   */
  public static class ShopListing<T> {
    private final T product;
    private final int buyPrice;

    /**
     * Creates a listing.
     *
     * @param product catalog product; must be non-null
     * @param buyPrice gold cost to buy; must be {@code >= 0}
     * @throws IllegalArgumentException if {@code product} is null or {@code buyPrice} is negative
     */
    public ShopListing(T product, int buyPrice) {
      if (product == null) {
        throw new IllegalArgumentException("product must not be null");
      }
      if (buyPrice < 0) {
        throw new IllegalArgumentException("buyPrice must be >= 0");
      }
      this.product = product;
      this.buyPrice = buyPrice;
    }

    /**
     * Returns the catalog product.
     *
     * @return product
     */
    public T getProduct() {
      return product;
    }

    /**
     * Returns the buy price in gold.
     *
     * @return buy price
     */
    public int getBuyPrice() {
      return buyPrice;
    }
  }

  /** Minimal Upgrade product stub until the Upgrades team provides a type. */
  public static class Upgrade {
    private final String name;

    /**
     * Creates a Upgrade stub.
     *
     * @param name Upgrade name; must not be null or blank
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Upgrade(String name) {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Upgrade name must not be null or blank.");
      }
      this.name = name;
    }

    /**
     * Returns the Upgrade name.
     *
     * @return name
     */
    public String getName() {
      return name;
    }
  }

  /** Minimal pet product stub until the pets team provides a type. */
  public static class Pet {
    private final String name;

    /**
     * Creates a pet stub.
     *
     * @param name pet name; must not be null or blank
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Pet(String name) {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Pet name must not be null or blank.");
      }
      this.name = name;
    }

    /**
     * Returns the pet name.
     *
     * @return name
     */
    public String getName() {
      return name;
    }
  }
}
