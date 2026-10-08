package com.csse3200.game.files;

/** One friendly NPC's quest state as stored in the save file. */
public class SavedNpc {
    /** Stable NPC id built from the map name, marker id and marker position. */
    public String id;

    /** The NPC's quest step (DialogueComponent state name), or null if never talked to. */
    public String dialogueState;

    /** Target of the quest the NPC gave out, used by perks when the quest is turned in. */
    public int amountXToDo;

    /** Type of the quest in progress, or null if no quest is in progress. */
    public String questType;

    /** Target of the quest in progress. */
    public int amountToDo;

    /** Counter value when the quest was accepted, so progress carries on correctly. */
    public float snapshot;

    public SavedNpc() {
        // Needed by the JSON reader.
    }
}