package com.csse3200.game.components.projectile;

public enum ProjectileType {
  ARROW,
  LIGHTNING,
  /**
   * Medusa's petrifying gaze: travels in a straight line like {@link #ARROW}, but immobilises
   * (rather than knocks back) whatever it hits - see {@link
   * com.csse3200.game.components.attacks.PetrifyEffectComponent}.
   */
  GAZE
}
