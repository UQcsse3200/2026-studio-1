package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player component that owns shop catalogs and buy/sell transactions.
 *
 * <p>Does not store gold or player items. Payments use the sibling {@link InventoryComponent} on
 * the same entity.
 */
public class ShopComponent extends Component {
  private final Map<Integer, ShopListing<Item>> itemCatalog;
  private final Map<Integer, ShopListing<Perk>> perkCatalog;
  private final Map<Integer, ShopListing<Pet>> petCatalog;
  private final List<Perk> purchasedPerks;
  private final List<Pet> purchasedPets;

  /** Creates a shop with empty item, perk, and pet catalogs. */
  public ShopComponent() {
    this.itemCatalog = new HashMap<>();
    this.perkCatalog = new HashMap<>();
    this.petCatalog = new HashMap<>();
    this.purchasedPerks = new ArrayList<>();
    this.purchasedPets = new ArrayList<>();
  }

  /**
   * Sets or clears an item catalog listing.
   *
   * @param slot catalog slot; must be {@code >= 1}
   * @param listing listing to store, or {@code null} to clear the slot
   * @return {@code true} if the slot was valid and updated
   */
  public boolean setItemListing(int slot, ShopListing<Item> listing) {
    if (slot < 1) {
      return false;
    }
    if (listing == null) {
      itemCatalog.remove(slot);
      return true;
    }
    if (listing.getProduct().getItemType() == ItemType.CURRENCY) {
      return false;
    }
    itemCatalog.put(slot, listing);
    return true;
  }

  /**
   * Sets or clears a perk catalog listing.
   *
   * @param slot catalog slot; must be {@code >= 1}
   * @param listing listing to store, or {@code null} to clear the slot
   * @return {@code true} if the slot was valid and updated
   */
  public boolean setPerkListing(int slot, ShopListing<Perk> listing) {
    return setListing(perkCatalog, slot, listing);
  }

  /**
   * Sets or clears a pet catalog listing.
   *
   * @param slot catalog slot; must be {@code >= 1}
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
   * Returns the perk listing in a catalog slot.
   *
   * @param slot catalog slot
   * @return the listing, or {@code null} if the slot is invalid or empty
   */
  public ShopListing<Perk> getPerkListing(int slot) {
    return getListing(perkCatalog, slot);
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
   * Returns an unmodifiable view of occupied perk catalog slots.
   *
   * @return unmodifiable map of slot index to listing
   */
  public Map<Integer, ShopListing<Perk>> getPerkCatalog() {
    return Collections.unmodifiableMap(perkCatalog);
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
   * Buys the item in a catalog slot using the sibling inventory wallet and slots.
   *
   * @param catalogSlot item catalog slot
   * @return {@code true} if gold was deducted and a copy of the item was added
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

    if (inventory.isFull()) {
      return false;
    }

    if (!inventory.hasGold(listing.getBuyPrice())) {
      return false;
    }

    inventory.addGold(-listing.getBuyPrice());
    inventory.addItem(copyItem(listing.getProduct()));
    return true;
  }

  /**
   * Sells the item in a player inventory slot and refunds the matching catalog sell price.
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

    ShopListing<Item> matchingListing = findItemListing(item);
    if (matchingListing == null) {
      return false;
    }

    Item removed = inventory.removeItem(playerSlot);
    if (removed == null) {
      return false;
    }

    inventory.addGold(matchingListing.getSellPrice());
    return true;
  }

  /**
   * Buys the perk in a catalog slot using gold only. Does not use item slots.
   *
   * @param catalogSlot perk catalog slot
   * @return {@code true} if gold was deducted and the purchase was recorded
   */
  public boolean buyPerk(int catalogSlot) {
    InventoryComponent inventory = getInventory();
    if (inventory == null) {
      return false;
    }

    ShopListing<Perk> listing = getPerkListing(catalogSlot);
    if (listing == null) {
      return false;
    }

    if (!inventory.hasGold(listing.getBuyPrice())) {
      return false;
    }

    inventory.addGold(-listing.getBuyPrice());
    purchasedPerks.add(listing.getProduct());
    if (entity != null) {
      entity.getEvents().trigger("perkPurchased");
    }
    return true;
  }

  /**
   * Buys the pet in a catalog slot using gold only. Does not use item slots.
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
    if (entity != null) {
      entity.getEvents().trigger("petPurchased");
    }
    return true;
  }

  /**
   * Returns an unmodifiable view of purchased perks.
   *
   * @return purchased perks in purchase order
   */
  public List<Perk> getPurchasedPerks() {
    return Collections.unmodifiableList(purchasedPerks);
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
   * Finds an item catalog listing whose product matches {@code item} by name and type.
   *
   * @param item player item to match
   * @return matching listing, or {@code null} if none
   */
  private ShopListing<Item> findItemListing(Item item) {
    for (ShopListing<Item> listing : itemCatalog.values()) {
      Item product = listing.getProduct();
      if (product.getName().equals(item.getName()) && product.getItemType() == item.getItemType()) {
        return listing;
      }
    }
    return null;
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
   * @param catalog catalog to update
   * @param slot catalog slot
   * @param listing listing to store, or {@code null} to clear
   * @param <T> product type
   * @return {@code true} if the slot was valid and updated
   */
  private <T> boolean setListing(
      Map<Integer, ShopListing<T>> catalog, int slot, ShopListing<T> listing) {
    if (slot < 1) {
      return false;
    }
    if (listing == null) {
      catalog.remove(slot);
      return true;
    }
    catalog.put(slot, listing);
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
    if (slot < 1) {
      return null;
    }
    return catalog.get(slot);
  }

  /**
   * Catalog data for a shop product. Nested on the player's shop component; not a {@link
   * Component} and not a wallet.
   *
   * @param <T> product type stored in this listing
   */
  public static class ShopListing<T> {
    private final T product;
    private final int buyPrice;
    private final int sellPrice;

    /**
     * Creates a listing.
     *
     * @param product catalog product; must be non-null
     * @param buyPrice gold cost to buy; must be {@code >= 0}
     * @param sellPrice gold refund on sell; must be {@code >= 0}
     * @throws IllegalArgumentException if {@code product} is null or a price is negative
     */
    public ShopListing(T product, int buyPrice, int sellPrice) {
      if (product == null) {
        throw new IllegalArgumentException("product must not be null");
      }
      if (buyPrice < 0 || sellPrice < 0) {
        throw new IllegalArgumentException("prices must be >= 0");
      }
      this.product = product;
      this.buyPrice = buyPrice;
      this.sellPrice = sellPrice;
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

    /**
     * Returns the sell price in gold. Unused for perk and pet listings.
     *
     * @return sell price
     */
    public int getSellPrice() {
      return sellPrice;
    }
  }

  /** Minimal perk product stub until the perks team provides a type. */
  public static class Perk {
    private final String name;

    /**
     * Creates a perk stub.
     *
     * @param name perk name; must not be null or blank
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Perk(String name) {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Perk name must not be null or blank.");
      }
      this.name = name;
    }

    /**
     * Returns the perk name.
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
