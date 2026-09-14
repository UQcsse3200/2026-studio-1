package com.csse3200.game.components;

import com.csse3200.game.components.player.PlayerActions;

/**
 * Component which temporarily changes a player's movement speed over time, then reverts it. Can be
 * used to pause (multiplier 0), slow (multiplier &lt; 1), or speed up (multiplier &gt; 1) the
 * player's movement. Multiple instances can be active on the same player at once; their effects
 * stack multiplicatively and each reverts independently when its own timer expires.
 */
public class SpeedEffectComponent extends Component {
  private int timeRemaining;
  private final float speedMultiplier;
  private PlayerActions playerActions;

  /**
   * Create a component which changes movement speed for a fixed duration.
   *
   * @param time number of ticks the effect lasts. A duration of 0 applies the effect instantly and
   *     never reverts it (matches HealthEffectComponent's "instant" convention).
   * @param speedMultiplier multiplier applied to the player's max speed while active (0 = pause,
   *     0.5 = half speed, 2 = double speed).
   */
  public SpeedEffectComponent(int time, float speedMultiplier) {
    if (time < 0) {
      throw new IllegalArgumentException("Time must not be negative");
    }
    if (speedMultiplier < 0) {
      throw new IllegalArgumentException("Speed multiplier must not be negative");
    }

    this.timeRemaining = time;
    this.speedMultiplier = speedMultiplier;
  }

  @Override
  public void create() {
    playerActions = entity.getComponent(PlayerActions.class);

    if (playerActions != null) {
      playerActions.addSpeedModifier(this, speedMultiplier);
    }

    if (timeRemaining == 0) {
      // Instant/permanent effect: apply and don't revert
      setEnabled(false);
    }
  }

  @Override
  public void update() {
    if (playerActions == null || timeRemaining <= 0) {
      setEnabled(false);
      return;
    }

    timeRemaining--;

    if (timeRemaining <= 0) {
      playerActions.removeSpeedModifier(this);
      setEnabled(false);
    }
  }

  @Override
  public void dispose() {
    // Ensure the modifier is cleaned up if the component is removed early (e.g. entity destroyed
    // mid-effect), so it doesn't linger and permanently affect speed.
    if (playerActions != null) {
      playerActions.removeSpeedModifier(this);
    }
  }
}
