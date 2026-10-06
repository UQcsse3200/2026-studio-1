package com.csse3200.game.files;

public class SavedBuff {
  public String stat;
  public float magnitude;
  public float remainingSeconds;

  public SavedBuff() {
    // required no-arg constructor so Json can reconstruct this on load
  }
}
