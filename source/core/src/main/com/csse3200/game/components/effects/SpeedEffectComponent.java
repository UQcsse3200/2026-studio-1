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
 *
 * <p>It also reports when the player is fully stopped, so the screen can show it: {@link
 * #FROZEN_START_EVENT} fires when the first timed stopping effect begins and {@link
 * #FROZEN_END_EVENT} fires when the last one ends, and {@link #getFrozenTicksRemaining()} gives the
 * time left.
 */
public class SpeedEffectComponent extends Component {
  /** Event fired on the player when a timed effect first stops them. */
  public static final String FROZEN_START_EVENT = "frozenStart";

  /** Event fired on the player when the last timed stopping effect ends or is disposed. */
  public static final String FROZEN_END_EVENT = "frozenEnd";

  private final List<ActiveEffect> activeEffects = new ArrayList<>();
  private PlayerActions playerActions;

  @Override
  public void create() {
    playerActions = entity.getComponent(PlayerActions.class);
    entity.getEvents().addListener("applySpeedEffect", this::applyEffect);
  }

  /**
   * Applies the given speed effect to the player. Also fires {@link #FROZEN_START_EVENT} when this
   * is the first timed stopping effect to become active.
   *
   * @param time number of ticks the effect lasts. A duration of 0 applies the effect instantly and
   *     never reverts it (matches HealthEffectComponent's "instant" convention), and is never shown
   *     as frozen.
   * @param speedMultiplier multiplier applied to the player's max speed while active (0 = pause,
   *     0.5 = half speed, 2 = double speed).
   * @throws IllegalArgumentException if time or speedMultiplier is negative
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

    boolean wasFrozen = isFrozen();
    ActiveEffect effect = new ActiveEffect(time, speedMultiplier);
    playerActions.addSpeedModifier(effect, speedMultiplier); // each effect is its own key
    if (time > 0) {
      activeEffects.add(effect); // time 0 = permanent, matches the old convention
    }
    if (!wasFrozen && isFrozen()) {
      entity.getEvents().trigger(FROZEN_START_EVENT);
    }
  }

  /**
   * Gets how many ticks the player stays fully stopped, counting every timed effect whose speed
   * multiplier is exactly zero. Effects that only slow the player, and effects with no end (a
   * duration of zero), are not counted.
   *
   * @return the largest number of ticks remaining among the stopping effects, or 0 if the player is
   *     not being stopped by a timed effect
   */
  public int getFrozenTicksRemaining() {
    int longest = 0;
    for (ActiveEffect effect : activeEffects) {
      if (effect.multiplier == 0f && effect.ticksRemaining > longest) {
        longest = effect.ticksRemaining;
      }
    }
    return longest;
  }

  /** Whether a timed stopping effect is active right now. */
  private boolean isFrozen() {
    return getFrozenTicksRemaining() > 0;
  }

  @Override
  public void update() {
    boolean wasFrozen = isFrozen();
    Iterator<ActiveEffect> it = activeEffects.iterator();
    while (it.hasNext()) {
      ActiveEffect effect = it.next();
      if (--effect.ticksRemaining <= 0) {
        playerActions.removeSpeedModifier(effect);
        it.remove();
      }
    }
    if (wasFrozen && !isFrozen()) {
      entity.getEvents().trigger(FROZEN_END_EVENT);
    }
  }

  @Override
  public void dispose() {
    // Ensure the modifier is cleaned up if the component is removed early (e.g. entity destroyed
    // mid-effect), so it doesn't linger and permanently affect speed.
    if (playerActions == null) {
      return;
    }
    boolean wasFrozen = isFrozen();
    for (ActiveEffect effect : activeEffects) {
      playerActions.removeSpeedModifier(effect);
    }
    activeEffects.clear();
    if (wasFrozen) {
      entity.getEvents().trigger(FROZEN_END_EVENT);
    }
  }

  private static class ActiveEffect {
    private int ticksRemaining;
    private final float multiplier;

    private ActiveEffect(int ticks, float multiplier) {
      this.ticksRemaining = ticks;
      this.multiplier = multiplier;
    }
  }
}
