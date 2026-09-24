package com.csse3200.game.components.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard and Premium spin catalogs held by the player's {@link ShopComponent}.
 *
 * <p>Not a {@link com.csse3200.game.components.Component} and not a wallet. Each catalog stores a
 * spin price and five weighted prizes. Later probability for a slot is {@code weight / sum(weights
 * in that catalog)}. This class stores weights and does not compute or roll them.
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
}
