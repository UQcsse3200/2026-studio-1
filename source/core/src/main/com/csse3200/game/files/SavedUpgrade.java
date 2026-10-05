package com.csse3200.game.files;

public class SavedUpgrade {
  public String id;
  public int tier;
  public float remainingSeconds;
  public int remainingKills;

  public SavedUpgrade() {
    // required no-arg constructor so Json can reconstruct this on load
  }
}
