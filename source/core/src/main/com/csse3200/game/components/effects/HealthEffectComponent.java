package com.csse3200.game.components.effects;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HealthEffectComponent extends Component {
  private final List<ActiveEffect> activeEffects = new ArrayList<>();
  private CombatStatsComponent combatStats;

  @Override
  public void create() {
    combatStats = entity.getComponent(CombatStatsComponent.class);
    entity.getEvents().addListener("applyHealthEffect", this::applyEffect);
  }

  /**
   * Apply a new effect that changes health over time.
   *
   * @param time total number of ticks the effect lasts. A duration of 0 applies the effect
   *     instantly on the next update.
   * @param healthChange total amount of health to change over the effect's duration (positive =
   *     regeneration, negative = poison)
   */
  public void applyEffect(Integer time, Integer healthChange) {
    if (time < 0) throw new IllegalArgumentException("Time must not be negative");
    if (combatStats == null) return;

    if (time == 0) {
      combatStats.addHealth(healthChange); // instant
      return;
    }
    activeEffects.add(new ActiveEffect(time, healthChange));
  }

  @Override
  public void update() {
    Iterator<ActiveEffect> it = activeEffects.iterator();
    // Cycle through all active effects
    while (it.hasNext()) {
      ActiveEffect effect = it.next();
      effect.ticksRemaining--;
      // if last tick, take remainder amount, else amount per tick
      int amount = (effect.ticksRemaining == 0) ? effect.total - effect.applied : effect.perTick;
      combatStats.addHealth(amount);
      effect.applied += amount;

      if (effect.ticksRemaining <= 0) {
        it.remove();
      }
    }
  }

  private static class ActiveEffect {
    private final int total;
    private final int perTick;
    private int ticksRemaining;
    private int applied = 0;

    private ActiveEffect(int ticks, int total) {
      this.ticksRemaining = ticks;
      this.total = total;
      this.perTick = total / ticks;
    }
  }
}
