package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
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
  public boolean hasGold(int gold) {
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
   * Returns the number of occupied slots.
   *
   * @return occupied slot count
   */
  public int getOccupiedSlots() {
    return inventorySlots.size();
  }

  /**
   * Returns the maximum number of item slots.
   *
   * @return maximum number of item slots
   */
  public int getMaxSlots() {
    return maxSlots;
  }

  /**
   * Returns an unmodifiable view of occupied slots.
   *
   * @return unmodifiable map of slot index to item
   */
  public Map<Integer, Item> getInventorySlots() {
    return Collections.unmodifiableMap(inventorySlots);
  }

  /**
   * Adds an item by filling compatible stacks first, then empty slots in order.
   *
   * @param item item to add
   * @return quantity that could not be added
   */
  public int addItem(Item item) {
    if (!isAddable(item)) {
      return 0;
    }
    return addItem(item, item.getQuantity());
  }

  /**
   * Adds a specific quantity using {@code item} as a stack template.
   *
   * @param item item template used for stack compatibility
   * @param quantity quantity to add
   * @return quantity that could not be added
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
   * Returns the total quantity of matching items.
   *
   * @param name item name to match
   * @param type item type to match
   * @param maxQuantity stack cap to match
   * @return summed quantity
   */
  public int getTotalQuantity(String name, ItemType type, int maxQuantity) {
    if (name == null || name.isBlank()) {
      return 0;
    }

    int total = 0;

    for (Item stored : inventorySlots.values()) {
      if (name.equals(stored.getName())
          && type == stored.getItemType()
          && maxQuantity == stored.getMaxQuantity()) {
        total += stored.getQuantity();
      }
    }

    return total;
  }

  /**
   * Returns the total quantity of items compatible with {@code template}.
   *
   * @param template item whose properties are used as the match key
   * @return summed quantity
   */
  public int getTotalQuantity(Item template) {
    if (template == null) {
      return 0;
    }

    return getTotalQuantity(template.getName(), template.getItemType(), template.getMaxQuantity());
  }

  /**
   * Splits part of an occupied stack into the lowest empty slot.
   *
   * @param slot source slot
   * @param splitQty quantity to move
   * @return the new slot index, or {@code -1} if the split is invalid
   */
  public int splitStack(int slot, int splitQty) {
    if (!isValidSlot(slot) || !containsItem(slot) || splitQty <= 0) {
      return -1;
    }

    Item source = inventorySlots.get(slot);
    int current = source.getQuantity();

    if (splitQty >= current) {
      return -1;
    }

    Integer empty = findEmptySlot();

    if (empty == null) {
      return -1;
    }

    source.setQuantity(current - splitQty);
    inventorySlots.put(empty, createStack(source, splitQty));

    return empty;
  }

  /**
   * Removes up to {@code amount} from a single slot.
   *
   * @param slot slot containing the item
   * @param amount quantity to remove
   * @return amount actually removed
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
   * Removes the whole stack from a slot.
   *
   * @param slot slot containing the item
   * @return removed item, or {@code null} if the slot is invalid or empty
   */
  public Item removeItem(int slot) {
    if (!isValidSlot(slot)) {
      return null;
    }

    return inventorySlots.remove(slot);
  }

  /**
   * Checks whether an item can be added.
   *
   * @param item candidate item
   * @return {@code true} if the item is non-null and has positive quantity
   */
  private boolean isAddable(Item item) {
    return item != null && item.getQuantity() > 0;
  }

  /**
   * Adds as much as possible into compatible existing stacks.
   *
   * @param item incoming item
   * @param remaining quantity still to add
   * @return quantity that could not be stacked
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
   * Calculates how much quantity can fit into an existing stack.
   *
   * @param existing existing item stack
   * @param remaining quantity still to add
   * @return quantity that can be added
   */
  private int calculateAddableQuantity(Item existing, int remaining) {
    int availableSpace = existing.getMaxQuantity() - existing.getQuantity();
    return Math.min(remaining, availableSpace);
  }

  /**
   * Places remaining quantity into empty slots.
   *
   * @param item incoming item
   * @param remaining quantity still to add
   * @return quantity that could not be placed
   */
  private int placeIntoEmptySlots(Item item, int remaining) {
    boolean useIncomingForNextSlot = !isItemAlreadyStored(item);

    while (remaining > 0) {
      Integer empty = findEmptySlot();

      if (empty == null) {
        break;
      }

      int stackSize = Math.min(remaining, item.getMaxQuantity());

      if (useIncomingForNextSlot) {
        item.setQuantity(stackSize);
        inventorySlots.put(empty, item);
        useIncomingForNextSlot = false;
      } else {
        inventorySlots.put(empty, createStack(item, stackSize));
      }

      remaining -= stackSize;
    }

    return remaining;
  }

  /**
   * Checks whether the item is already stored in the inventory.
   *
   * @param item item reference to check
   * @return {@code true} if the same item instance is stored
   */
  private boolean isItemAlreadyStored(Item item) {
    return inventorySlots.containsValue(item);
  }

  /**
   * Creates a new stack from an existing item.
   *
   * @param template item used as the template
   * @param quantity quantity for the new stack
   * @return new item stack
   */
  private Item createStack(Item template, int quantity) {
    return new Item(
        template.getName(), template.getItemType(), quantity, template.getMaxQuantity());
  }

  /**
   * Checks whether a slot number is valid.
   *
   * @param slot slot index
   * @return {@code true} if the slot is valid
   */
  private boolean isValidSlot(int slot) {
    return slot >= 1 && slot <= maxSlots;
  }

  /**
   * Finds the lowest-numbered empty slot.
   *
   * @return empty slot index, or {@code null} if inventory is full
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
   * Checks whether two items can be stacked together.
   *
   * @param existing item already in the inventory
   * @param incoming item being added
   * @return {@code true} if the items are compatible and the existing stack is not full
   */
  public boolean canStack(Item existing, Item incoming) {
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
