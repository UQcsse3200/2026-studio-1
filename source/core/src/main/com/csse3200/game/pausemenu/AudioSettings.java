package com.csse3200.game.pausemenu;

public final class AudioSettings {
  private static volatile float masterVolume = 1f;
  private static volatile float effectsVolume = 1f;

  private AudioSettings() {}

  public static float getMasterVolume() {
    return masterVolume;
  }

  public static void setMasterVolume(float value) {
    masterVolume = value;
  }

  public static float getEffectsVolume() {
    return effectsVolume;
  }

  public static void setEffectsVolume(float value) {
    effectsVolume = value;
  }

  public static float getEffectiveEffectsVolume() {
    return masterVolume * effectsVolume;
  }
}