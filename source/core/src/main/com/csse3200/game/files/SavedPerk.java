package com.csse3200.game.files;

/** One perk's state as stored in the save file. */
public class SavedPerk {
    public String id;
    public int progress;
    public boolean unlocked;
    public boolean active;

    public SavedPerk() {
        // Needed by the JSON reader.
    }
}