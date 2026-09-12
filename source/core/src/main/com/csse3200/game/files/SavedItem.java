package com.csse3200.game.files;

public class SavedItem {
    public int slot;
    public String name;
    public String itemType;
    public int quantity;
    public int maxQuantity;

    public String weaponType;
    public Integer damage;

    public SavedItem() {
        // required no-arg constructor so Json can reconstruct this on load
    }
}