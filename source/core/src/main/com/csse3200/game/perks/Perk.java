package com.csse3200.game.perks;

public class Perk {
  private final String id;
  private final String name;
  private final String description;
  private final String eventKey;
  private final int threshold;

  private int progress = 0;
  private boolean unlocked = false;
  private boolean active = false;
  private Runnable onUnlocked;
  private Runnable onActivated;
  private Runnable onDeactivated;

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

  /**
   * Whether this perk is currently one of the player's up-to-2 equipped perks. Unlike {@link
   * #isUnlocked()} (permanent, once earned), this can toggle freely - see {@link
   * PerkService#activate} / {@link PerkService#deactivate}, driven by the perk-selection screen
   * shown after death.
   *
   * @return {@code true} if currently active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Package-private: only {@link PerkService} should change this, so the max-2 rule is enforced in
   * exactly one place.
   */
  void setActive(boolean active) {
    this.active = active;
  }

  public void setOnUnlocked(Runnable onUnlocked) {
    this.onUnlocked = onUnlocked;
  }

  /**
   * Registers what should happen the moment this perk becomes active - e.g. {@code ShieldComponent}
   * applying its duration bonus. Called by {@link PerkService#activate}, and also checked directly
   * against {@link #isActive()} at entity creation (see that class), so a reward is applied
   * correctly whether the perk was already active before this entity existed or becomes active
   * while it's alive.
   */
  public void setOnActivated(Runnable onActivated) {
    this.onActivated = onActivated;
  }

  /**
   * Registers what should happen the moment this perk becomes inactive - the exact inverse of
   * {@link #setOnActivated}, e.g. {@code ConsumableUseComponent} removing its max-health bonus
   * (clamping current health down if it's above the new, lower cap). Called by {@link
   * PerkService#deactivate}.
   */
  public void setOnDeactivated(Runnable onDeactivated) {
    this.onDeactivated = onDeactivated;
  }

  /** Package-private: only {@link PerkService} should trigger this. */
  void notifyActivated() {
    if (onActivated != null) {
      onActivated.run();
    }
  }

  /** Package-private: only {@link PerkService} should trigger this. */
  void notifyDeactivated() {
    if (onDeactivated != null) {
      onDeactivated.run();
    }
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

  void restoreState(int progress, boolean unlocked, boolean active) {
    this.progress = Math.min(threshold, progress);
    this.unlocked = unlocked;
    // A perk can never be active without being unlocked - guards against corrupted/hand-edited
    // save data claiming otherwise.
    this.active = unlocked && active;
  }

  /** Human-readable progress text for the UI, e.g. "Unlocked" or "23/50". */
  public String getProgressText() {
    return unlocked ? "Unlocked" : progress + "/" + threshold;
  }
}
