package com.csse3200.game.components.loot;

/**
 * Represents the base structure for all items in the loot generation system.
 *
 * Each item has a unique ID, name, type, quantity, and maximum stack quantity.
 */
public class Item {

    private static int nextId = 0;

    private final int id;
    private final String name;
    private final ItemType itemType;
    private int quantity;
    private final int maxQuantity;

    public Item(String name, ItemType itemType, int quantity, int maxQuantity) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Item name must not be null or blank.");
        }

        if (itemType == null) {
            throw new IllegalArgumentException(
                    "ItemType must not be null.");
        }

        if (maxQuantity <= 0) {
            throw new IllegalArgumentException(
                    "maxQuantity must be greater than 0.");
        }

        this.id = nextId++;
        this.name = name;
        this.itemType = itemType;
        this.maxQuantity = maxQuantity;

        setQuantity(quantity);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, Math.min(quantity, maxQuantity));
    }

    public void addQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }

    public boolean isStackFull() {
        return quantity == maxQuantity;
    }

    public boolean isEmpty() {
        return quantity == 0;
    }

    @Override
    public String toString() {
        return String.format(
                "Item{id=%d, name='%s', type=%s, quantity=%d/%d}",
                id, name, itemType, quantity, maxQuantity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Item)) {
            return false;
        }

        Item other = (Item) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}