package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.Item;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component intended to be used by the player to track their inventory.
 *
 * <p>Stores gold and item slots. Can also be used as a more generic component for other entities.
 */
public class InventoryComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(InventoryComponent.class);

  /** Default slot capacity when {@link #InventoryComponent(int)} is used. */
  private static final int DEFAULT_MAX_SLOTS = 5;

  private final Map<Integer, Item> inventorySlots;
  private final int maxSlots;
  private int gold;

  /**
   * Creates an inventory with the default slot capacity of {@value #DEFAULT_MAX_SLOTS}.
   *
   * @param gold starting gold; values below 0 are clamped to 0
   */
  public InventoryComponent(int gold) {
    this(gold, DEFAULT_MAX_SLOTS);
  }

  /**
   * Creates an inventory with an explicit slot capacity.
   *
   * @param gold starting gold; values below 0 are clamped to 0
   * @param maxSlots maximum number of item slots; must be {@code >= 1}
   * @throws IllegalArgumentException if {@code maxSlots < 1}
   */
  public InventoryComponent(int gold, int maxSlots) {
    if (maxSlots < 1) {
      throw new IllegalArgumentException("maxSlots must be >= 1");
    }
    this.maxSlots = maxSlots;
    this.inventorySlots = new HashMap<>();
    setGold(gold);
  }

  /**
   * Returns the player's gold.
   *
   * @return current gold amount
   */
  public int getGold() {
    return this.gold;
  }

  /**
   * Returns if the player has a certain amount of gold.
   *
   * @param gold required amount of gold
   * @return player has greater than or equal to the required amount of gold
   */
  public Boolean hasGold(int gold) {
    return this.gold >= gold;
  }

  /**
   * Sets the player's gold. Gold has a minimum bound of 0.
   *
   * @param gold gold to set; values below 0 are clamped to 0
   */
  public void setGold(int gold) {
    this.gold = Math.max(gold, 0);
    logger.debug("Setting gold to {}", this.gold);
  }

  /**
   * Adds to the player's gold. The amount added can be negative.
   *
   * @param gold gold to add
   */
  public void addGold(int gold) {
    setGold(this.gold + gold);
  }

  /**
   * Returns the item in the given slot without removing it.
   *
   * @param slot slot index in the range 1 to {@code maxSlots}
   * @return the stored item, or {@code null} if the slot is invalid or empty
   */
  public Item getItem(int slot) {
    if (!isValidSlot(slot)) {
      return null;
    }
    return inventorySlots.get(slot);
  }

  /**
   * Returns whether the slot is valid and currently occupied.
   *
   * @param slot slot index in the range 1 to {@code maxSlots}
   * @return {@code true} only if the slot is valid and a key is present
   */
  public boolean containsItem(int slot) {
    return isValidSlot(slot) && inventorySlots.containsKey(slot);
  }

  /**
   * Returns whether every slot is occupied.
   *
   * @return {@code true} iff {@link #getOccupiedSlots()} equals {@code maxSlots}
   */
  public boolean isFull() {
    return getOccupiedSlots() == maxSlots;
  }

  /**
   * Returns the number of occupied slots, derived from the map size.
   *
   * @return occupied slot count
   */
  public int getOccupiedSlots() {
    return inventorySlots.size();
  }

  /**
   * Returns the constructor capacity.
   *
   * @return maximum number of item slots
   */
  public int getMaxSlots() {
    return maxSlots;
  }

  /**
   * Returns an unmodifiable view of occupied slots. Callers cannot insert or remove keys through
   * this map.
   *
   * @return unmodifiable map of slot index to item
   */
  public Map<Integer, Item> getInventorySlots() {
    return Collections.unmodifiableMap(inventorySlots);
  }

  /**
   * Adds an item by filling compatible stacks first, then empty slots in order.
   *
   * @param item item to add; {@code null} or non-positive quantity is a no-op
   * @return quantity that could not be added; {@code 0} means the complete quantity was added
   */
  public int addItem(Item item) {
    if (!isAddable(item)) {
      return 0;
    }
    return addItem(item, item.getQuantity());
  }

  /**
   * Adds a specific quantity using {@code item} as a stack template. This supports pickup amounts
   * that exceed a single stack cap.
   *
   * @param item item template used for stack compatibility and max quantity
   * @param quantity quantity to add
   * @return quantity that could not be added; {@code 0} means the complete quantity was added
   */
  public int addItem(Item item, int quantity) {
    if (item == null) {
      return quantity;
    }
    if (quantity <= 0) {
      return 0;
    }

    int remaining = stackIntoExistingSlots(item, quantity);
    return placeIntoEmptySlots(item, remaining);
  }

  /**
   * Removes up to {@code amount} from a single slot. Does not compact later slots.
   *
   * @param slot slot index in the range 1 to {@code maxSlots}
   * @param amount quantity to remove
   * @return the amount actually removed; {@code 0} if invalid, empty, or {@code amount <= 0}
   */
  public int removeItem(int slot, int amount) {
    if (!isValidSlot(slot) || amount <= 0 || !inventorySlots.containsKey(slot)) {
      return 0;
    }
    Item existing = inventorySlots.get(slot);
    int current = existing.getQuantity();
    int removed = Math.min(amount, current);
    existing.setQuantity(current - removed);
    if (existing.getQuantity() == 0) {
      inventorySlots.remove(slot);
    }
    return removed;
  }

  /**
   * Removes the whole stack from a slot. Does not compact later slots.
   *
   * @param slot slot index in the range 1 to {@code maxSlots}
   * @return the stored item, or {@code null} if the slot is invalid or empty
   */
  public Item removeItem(int slot) {
    if (!isValidSlot(slot)) {
      return null;
    }
    return inventorySlots.remove(slot);
  }

  /**
   * Returns whether {@code item} can be added to this inventory.
   *
   * @param item candidate item, which may be {@code null}
   * @return {@code true} if the item is non-null and has a positive quantity
   */
  private boolean isAddable(Item item) {
    return item != null && item.getQuantity() > 0;
  }

  /**
   * Adds as much of {@code remaining} as possible into compatible existing stacks.
   *
   * @param item incoming item used for stack compatibility
   * @param remaining quantity still to add
   * @return quantity that could not be stacked into existing slots
   */
  private int stackIntoExistingSlots(Item item, int remaining) {
    for (int slot = 1; slot <= maxSlots && remaining > 0; slot++) {
      Item existing = inventorySlots.get(slot);
      if (!canStack(existing, item)) {
        continue;
      }
      int addedQuantity = calculateAddableQuantity(existing, remaining);
      existing.addQuantity(addedQuantity);
      remaining -= addedQuantity;
    }
    return remaining;
  }

  /**
   * Calculates how much of {@code remaining} can fit into {@code existing} without exceeding its
   * maximum quantity. Does not modify either item.
   *
   * @param existing occupied stack to measure
   * @param remaining quantity still to add
   * @return {@code min(remaining, available space)}
   */
  private int calculateAddableQuantity(Item existing, int remaining) {
    int availableSpace = existing.getMaxQuantity() - existing.getQuantity();
    return Math.min(remaining, availableSpace);
  }

  /**
   * Places leftover quantity into empty slots in order. The incoming item occupies the first new
   * slot; later slots receive newly created stacks.
   *
   * @param item incoming item to place or copy
   * @param remaining quantity still to add
   * @return quantity that could not be placed because no empty slot remained
   */
  private int placeIntoEmptySlots(Item item, int remaining) {
    boolean placedIncoming = false;
    while (remaining > 0) {
      Integer empty = findEmptySlot();
      if (empty == null) {
        break;
      }
      int stackSize = Math.min(remaining, item.getMaxQuantity());
      if (!placedIncoming) {
        item.setQuantity(stackSize);
        inventorySlots.put(empty, item);
        placedIncoming = true;
      } else {
        inventorySlots.put(empty, createStack(item, stackSize));
      }
      remaining -= stackSize;
    }
    return remaining;
  }

  /**
   * Creates a new item with the same name, type, and maximum quantity as {@code template}.
   *
   * @param template item whose identity fields are copied
   * @param quantity quantity for the new stack
   * @return a new {@link Item} instance
   */
  private Item createStack(Item template, int quantity) {
    return new Item(
        template.getName(), template.getItemType(), quantity, template.getMaxQuantity());
  }

  /**
   * Returns whether {@code slot} is within the inventory range.
   *
   * @param slot slot index to check
   * @return {@code true} if {@code slot} is between 1 and {@code maxSlots} inclusive
   */
  private boolean isValidSlot(int slot) {
    return slot >= 1 && slot <= maxSlots;
  }

  /**
   * Finds the lowest-numbered unoccupied slot.
   *
   * @return the empty slot index, or {@code null} if the inventory is full
   */
  private Integer findEmptySlot() {
    for (int slot = 1; slot <= maxSlots; slot++) {
      if (!inventorySlots.containsKey(slot)) {
        return slot;
      }
    }
    return null;
  }

  /**
   * Returns whether {@code incoming} can stack onto {@code existing}. Compatibility uses name,
   * type, and max quantity. {@link Item#equals(Object)} is identity by unique instance id and must
   * not be used here.
   *
   * @param existing item already in a slot; {@code null} is not stackable
   * @param incoming item being added; {@code null} is not stackable
   * @return {@code true} if the stacks are compatible and {@code existing} is not full
   */
  private boolean canStack(Item existing, Item incoming) {
    if (existing == null || incoming == null) {
      return false;
    }
    if (existing.isStackFull()) {
      return false;
    }
    return existing.getName().equals(incoming.getName())
        && existing.getItemType() == incoming.getItemType()
        && existing.getMaxQuantity() == incoming.getMaxQuantity();
  }
}
