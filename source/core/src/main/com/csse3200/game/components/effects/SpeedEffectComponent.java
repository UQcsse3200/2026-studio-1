package com.csse3200.game.components.effects;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.PlayerActions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Component which temporarily changes a player's movement speed over time, then reverts it. Can be
 * used to pause (multiplier 0), slow (multiplier &lt; 1), or speed up (multiplier &gt; 1) the
 * player's movement. Multiple instances can be active on the same player at once; their effects
 * stack multiplicatively and each reverts independently when its own timer expires.
 */
public class SpeedEffectComponent extends Component {
  private final List<ActiveEffect> activeEffects = new ArrayList<>();
  private PlayerActions playerActions;

  @Override
  public void create() {
    playerActions = entity.getComponent(PlayerActions.class);
    entity.getEvents().addListener("applySpeedEffect", this::applyEffect);
  }

  /**
   * Applies the given speed effect to the player.
   *
   * @param time number of ticks the effect lasts. A duration of 0 applies the effect instantly and
   *     never reverts it (matches HealthEffectComponent's "instant" convention).
   * @param speedMultiplier multiplier applied to the player's max speed while active (0 = pause,
   *     0.5 = half speed, 2 = double speed).
   */
  public void applyEffect(Integer time, Float speedMultiplier) {
    if (time < 0) {
      throw new IllegalArgumentException("Time must not be negative");
    }
    if (speedMultiplier < 0) {
      throw new IllegalArgumentException("Speed multiplier must not be negative");
    }
    if (playerActions == null) {
      return;
    }

    ActiveEffect effect = new ActiveEffect(time);
    playerActions.addSpeedModifier(effect, speedMultiplier); // each effect is its own key
    if (time > 0) {
      activeEffects.add(effect); // time 0 = permanent, matches the old convention
    }
  }

  @Override
  public void update() {
    Iterator<ActiveEffect> it = activeEffects.iterator();
    while (it.hasNext()) {
      ActiveEffect effect = it.next();
      if (--effect.ticksRemaining <= 0) {
        playerActions.removeSpeedModifier(effect);
        it.remove();
      }
    }
  }

  @Override
  public void dispose() {
    // Ensure the modifier is cleaned up if the component is removed early (e.g. entity destroyed
    // mid-effect), so it doesn't linger and permanently affect speed.
    if (playerActions == null) {
      return;
    }
    for (ActiveEffect effect : activeEffects) {
      playerActions.removeSpeedModifier(effect);
    }
    activeEffects.clear();
  }

  private static class ActiveEffect {
    private int ticksRemaining;

    private ActiveEffect(int ticks) {
      this.ticksRemaining = ticks;
    }
  }
}
