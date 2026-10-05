package com.csse3200.game.components.pet;

import java.util.Locale;

/** Companion types, shared by pet animations and projectile selection. */
public enum PetType {
  BIRD,
  BAT,
  SPIRIT;

  /** Returns the matching atlas animation prefix. */
  public String getAnimationPrefix() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Maps shop names to companion types, retaining Bird as the default for unknown names. */
  public static PetType fromName(String name) {
    if (name == null) {
      return BIRD;
    }
    return switch (name.toLowerCase(Locale.ROOT)) {
      case "bat" -> BAT;
      case "spirit" -> SPIRIT;
      default -> BIRD;
    };
  }
}
