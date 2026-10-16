package com.csse3200.game.components.effects;

/**
 * The names used to tell the screen why the player was just stopped. The attack that stops the
 * player sends {@link #STATUS_CAUSE_EVENT} on the player just before its {@code
 * "applySpeedEffect"}, so the indicator can choose between "Frozen" and "Petrified". The text is
 * typed once, here, so the attacks and the indicator cannot drift apart.
 */
public final class FrozenCause {
  /**
   * Event sent to the player just before {@code "applySpeedEffect"}, naming why the player is about
   * to be stopped. The indicator uses it to choose between "Frozen" and "Petrified".
   *
   * <p>Argument: the cause text, one of {@link #FROZEN} or {@link #PETRIFIED}.
   */
  public static final String STATUS_CAUSE_EVENT = "statusCause";

  /** Cause text for lightning. */
  public static final String FROZEN = "Frozen";

  /** Cause text for Medusa's gaze. */
  public static final String PETRIFIED = "Petrified";

  private FrozenCause() {
    throw new IllegalStateException("Utility class");
  }
}
