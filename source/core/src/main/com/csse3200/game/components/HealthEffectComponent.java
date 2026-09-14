package com.csse3200.game.components;

public class HealthEffectComponent extends Component {
  private int timeRemaining;
  private final int healthPerTick;
  private final int totalHealth;
  private int totalApplied = 0;
  private CombatStatsComponent combatStats;

  /**
   * Create a component which changes health over time.
   *
   * @param time total number of ticks the effect lasts. A duration of 0 applies the effect
   *     instantly on the next update.
   * @param healthChange total amount of health to change over the effect's duration (positive =
   *     regeneration, negative = poison)
   */
  public HealthEffectComponent(int time, int healthChange) {
    if (time < 0) {
      throw new IllegalArgumentException("Time must not be negative");
    }

    this.totalHealth = healthChange;
    this.timeRemaining = time;
    this.healthPerTick = (time == 0) ? 0 : healthChange / time;
  }

  @Override
  public void create() {
    combatStats = entity.getComponent((CombatStatsComponent.class));

    if (timeRemaining == 0) {
      // Instant effect: apply immediately, no ticking needed
      if (combatStats != null) {
        combatStats.addHealth(totalHealth);
      }
      setEnabled(false);
    }
  }

  @Override
  public void update() {
    if (combatStats == null || timeRemaining <= 0) {
      setEnabled(false);
      return;
    }

    timeRemaining--;

    int amount;
    if (timeRemaining == 0) {
      // Last tick: apply whatever remains so the total is exact
      amount = totalHealth - totalApplied;
    } else {
      amount = healthPerTick;
    }

    combatStats.addHealth(amount);
    totalApplied += amount;

    if (timeRemaining <= 0) {
      setEnabled(false);
    }
  }
}
