package com.csse3200.game.perks;

public class Perk {
  private final String id;
  private final String name;
  private final String description;
  private final String eventKey;
  private final int threshold;

  private int progress = 0;
  private boolean unlocked = false;
  private Runnable onUnlocked;

  public Perk(String id, String name, String description, String eventKey, int threshold) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.eventKey = eventKey;
    this.threshold = threshold;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getEventKey() {
    return eventKey;
  }

  public int getThreshold() {
    return threshold;
  }

  public int getProgress() {
    return progress;
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public void setOnUnlocked(Runnable onUnlocked) {
    this.onUnlocked = onUnlocked;
  }

  boolean recordProgress(int amount) {
    if (unlocked) {
      return false;
    }
    progress = Math.min(threshold, progress + amount);
    if (progress >= threshold) {
      unlocked = true;
      if (onUnlocked != null) {
        onUnlocked.run();
      }
      return true;
    }
    return false;
  }

  void restoreState(int progress, boolean unlocked) {
    this.progress = Math.min(threshold, progress);
    this.unlocked = unlocked;
  }

  /** Human-readable progress text for the UI, e.g. "Unlocked" or "23/50". */
  public String getProgressText() {
    return unlocked ? "Unlocked" : progress + "/" + threshold;
  }
}
