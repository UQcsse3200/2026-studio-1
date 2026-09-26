package com.csse3200.game.components.player;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Standard and Premium spin catalogs held by the player's {@link ShopComponent}.
 *
 * <p>Not a {@link com.csse3200.game.components.Component} and not a wallet. Each catalog stores a
 * spin price and five weighted prizes. Later probability for a slot is {@code weight / sum(weights
 * in that catalog)}. This class stores weights and does not compute or roll them; see {@link
 * GamblingRoller}.
 *
 * <p>A prize product is one of {@link ItemPrize}, {@link GoldPrize}, {@link ShopComponent.Pet} or
 * {@link ShopComponent.Upgrade}.
 */
public class GamblingCatalogs {
  private final SpinCatalog standard;
  private final SpinCatalog premium;

  /**
   * Creates both spin catalogs.
   *
   * @param standard Standard ticket; must be non-null
   * @param premium Premium ticket; must be non-null
   * @throws IllegalArgumentException if either catalog is null
   */
  public GamblingCatalogs(SpinCatalog standard, SpinCatalog premium) {
    if (standard == null || premium == null) {
      throw new IllegalArgumentException("standard and premium catalogs must not be null");
    }
    this.standard = standard;
    this.premium = premium;
  }

  /**
   * Returns the Standard catalog.
   *
   * @return Standard spin catalog
   */
  public SpinCatalog getStandard() {
    return standard;
  }

  /**
   * Returns the Premium catalog.
   *
   * @return Premium spin catalog
   */
  public SpinCatalog getPremium() {
    return premium;
  }

  /**
   * Returns the catalog for a pool id.
   *
   * @param catalogId {@link CatalogId#STANDARD} or {@link CatalogId#PREMIUM}; {@code null} is
   *     invalid
   * @return the matching catalog, or {@code null} if {@code catalogId} is null
   */
  public SpinCatalog get(CatalogId catalogId) {
    if (catalogId == null) {
      return null;
    }
    return switch (catalogId) {
      case STANDARD -> standard;
      case PREMIUM -> premium;
    };
  }

  /** Pool id for one spin catalog. Not a third catalog. */
  public enum CatalogId {
    STANDARD,
    PREMIUM
  }

  /**
   * One gambling ticket: a spin price and exactly five prize slots.
   *
   * <p>Slot keys are {@code 1}..{@link #PRIZE_SLOT_COUNT}. Probability is not computed here.
   */
  public static final class SpinCatalog {
    /** Number of prize slots in every spin catalog. */
    public static final int PRIZE_SLOT_COUNT = 5;

    private int spinPrice;
    private final Map<Integer, PrizeEntry<?>> prizes;

    /**
     * Creates a catalog.
     *
     * @param spinPrice gold cost of one spin; must be {@code >= 0}
     * @param prizes entries for slots {@code 1}..{@code 5}; copied, so the caller map is not kept
     * @throws IllegalArgumentException if the price is negative, {@code prizes} is null, a slot is
     *     missing or null, or the map is not exactly those five keys
     */
    public SpinCatalog(int spinPrice, Map<Integer, PrizeEntry<?>> prizes) {
      if (spinPrice < 0) {
        throw new IllegalArgumentException("spinPrice must be >= 0");
      }
      if (prizes == null) {
        throw new IllegalArgumentException("prizes must not be null");
      }
      if (prizes.size() != PRIZE_SLOT_COUNT) {
        throw new IllegalArgumentException("prizes must contain exactly 5 slots");
      }
      Map<Integer, PrizeEntry<?>> copy = new HashMap<>();
      for (int slot = 1; slot <= PRIZE_SLOT_COUNT; slot++) {
        PrizeEntry<?> entry = prizes.get(slot);
        if (entry == null) {
          throw new IllegalArgumentException("prize slot " + slot + " must be non-null");
        }
        copy.put(slot, entry);
      }
      this.spinPrice = spinPrice;
      this.prizes = copy;
    }

    /**
     * Returns the spin price in gold.
     *
     * @return spin price
     */
    public int getSpinPrice() {
      return spinPrice;
    }

    /**
     * Returns the prize in a slot.
     *
     * @param slot prize slot
     * @return the entry, or {@code null} if {@code slot} is outside {@code 1}..{@code 5}
     */
    public PrizeEntry<?> getPrize(int slot) {
      if (slot < 1 || slot > PRIZE_SLOT_COUNT) {
        return null;
      }
      return prizes.get(slot);
    }

    /**
     * Returns the prize map.
     *
     * @return unmodifiable map of slot index to prize
     */
    public Map<Integer, PrizeEntry<?>> getPrizes() {
      return Collections.unmodifiableMap(prizes);
    }

    /**
     * Replaces the spin price when it differs.
     *
     * @param spinPrice new price; caller must already have rejected a negative price
     * @return {@code true} if the stored price changed
     */
    public boolean replaceSpinPrice(int spinPrice) {
      if (this.spinPrice == spinPrice) {
        return false;
      }
      this.spinPrice = spinPrice;
      return true;
    }

    /**
     * Replaces one prize when the instance differs. Does not change the slot count.
     *
     * @param slot prize slot; must be {@code 1}..{@code 5}
     * @param prize replacement entry; must be non-null
     * @return {@code true} if the stored entry changed
     */
    public boolean replacePrize(int slot, PrizeEntry<?> prize) {
      if (prizes.get(slot) == prize) {
        return false;
      }
      prizes.put(slot, prize);
      return true;
    }
  }

  /**
   * One weighted prize. Holds a product and a weight only.
   *
   * <p>Later roll probability in a catalog is {@code weight / sum(weights in that catalog)}.
   *
   * @param <T> product type
   */
  public static final class PrizeEntry<T> {
    private final T product;
    private final int weight;

    /**
     * Creates a prize entry.
     *
     * @param product prize product; must be non-null
     * @param weight probability weight; must be {@code > 0}
     * @throws IllegalArgumentException if {@code product} is null or {@code weight} is not positive
     */
    public PrizeEntry(T product, int weight) {
      if (product == null) {
        throw new IllegalArgumentException("product must not be null");
      }
      if (weight <= 0) {
        throw new IllegalArgumentException("weight must be > 0");
      }
      this.product = product;
      this.weight = weight;
    }

    /**
     * Returns the prize product.
     *
     * @return product
     */
    public T getProduct() {
      return product;
    }

    /**
     * Returns the probability weight.
     *
     * @return weight
     */
    public int getWeight() {
      return weight;
    }
  }

  /**
   * An item prize. Holds a factory rather than an {@link Item} so every win gets a new typed item
   * (for example a {@code WeaponItem} or {@code ConsumableItem}) and the catalog never shares an
   * instance with the player's inventory.
   */
  public static final class ItemPrize {
    private final String displayName;
    private final ItemType itemType;
    private final Supplier<? extends Item> factory;

    /**
     * Creates an item prize.
     *
     * @param displayName name shown on the wheel; must not be null or blank
     * @param itemType type of the item the factory creates; must not be null or {@link
     *     ItemType#CURRENCY} (use {@link GoldPrize} for gold)
     * @param factory creates a new item for each win; must be non-null
     * @throws IllegalArgumentException if any argument is invalid
     */
    public ItemPrize(String displayName, ItemType itemType, Supplier<? extends Item> factory) {
      if (displayName == null || displayName.isBlank()) {
        throw new IllegalArgumentException("displayName must not be null or blank");
      }
      if (itemType == null || itemType == ItemType.CURRENCY) {
        throw new IllegalArgumentException("itemType must be non-null and not CURRENCY");
      }
      if (factory == null) {
        throw new IllegalArgumentException("factory must not be null");
      }
      this.displayName = displayName;
      this.itemType = itemType;
      this.factory = factory;
    }

    /**
     * Returns the name shown on the wheel.
     *
     * @return display name
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Returns the type of the item this prize creates.
     *
     * @return item type
     */
    public ItemType getItemType() {
      return itemType;
    }

    /**
     * Creates a new item for one win.
     *
     * @return a new item instance
     * @throws IllegalStateException if the factory returns null or an item of another type
     */
    public Item create() {
      Item item = factory.get();
      if (item == null || item.getItemType() != itemType) {
        throw new IllegalStateException("factory must create a new " + itemType + " item");
      }
      return item;
    }
  }

  /** A gold prize added straight to the player's wallet. */
  public static final class GoldPrize {
    private final int amount;

    /**
     * Creates a gold prize.
     *
     * @param amount gold awarded; must be {@code > 0}
     * @throws IllegalArgumentException if {@code amount} is not positive
     */
    public GoldPrize(int amount) {
      if (amount <= 0) {
        throw new IllegalArgumentException("amount must be > 0");
      }
      this.amount = amount;
    }

    /**
     * Returns the gold awarded.
     *
     * @return amount
     */
    public int getAmount() {
      return amount;
    }
  }
}
