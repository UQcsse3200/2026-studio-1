package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Rolls a probability check on a configurable hit event and, on success, attaches a
 * HealthEffectComponent to the target — applying a bonus health-over-time effect (e.g. poison) on
 * top of the base damage already dealt by whichever attack component fired the event.
 */
public class OnHitEffectComponent extends Component {

  private final List<String> hitEventNames;
  private final float triggerChance;
  private final int effectDuration;
  private final int effectHealthChange;
  private final Random random;
  private final List<ActiveEffect> activeEffects = new ArrayList<>();

  /**
   * Creates an on-hit effect component using a default (non-seeded) random source.
   *
   * @param hitEventNames event that triggers this component's roll, e.g. "meleeAttackHit" or
   *     "rangedAttackHit" — must match the name used by whichever attack component fires it.
   * @param triggerChance probability, in (0.0, 1.0], of applying the effect on a given hit.
   * @param effectDuration ticks over which the resulting HealthEffectComponent applies its effect;
   *     0 for an instant effect.
   * @param effectHealthChange total health change applied by the resulting HealthEffectComponent;
   *     negative for poison/DOT-style effects.
   * @throws IllegalArgumentException if triggerChance is &lt;= 0 or &gt; 1, or effectDuration is
   *     negative.
   */
  public OnHitEffectComponent(
      List<String> hitEventNames, float triggerChance, int effectDuration, int effectHealthChange) {
    if (hitEventNames == null || hitEventNames.isEmpty()) {
      throw new IllegalArgumentException("hitEventNames must not null or empty.");
    }
    if (triggerChance <= 0 || triggerChance > 1) {
      throw new IllegalArgumentException("Trigger chance probability must be within (0,1]");
    }
    if (effectDuration < 0) {
      throw new IllegalArgumentException("effectDuration must not be negative.");
    }
    this.hitEventNames = hitEventNames;
    this.triggerChance = triggerChance;
    this.effectDuration = effectDuration;
    this.effectHealthChange = effectHealthChange;
    this.random = new Random();
  }

  /**
   * Package-private constructor for injecting a specific Random instance, used by tests to force
   * deterministic roll outcomes.
   *
   * @param random the random source to use for the trigger roll.
   */
  OnHitEffectComponent(
      List<String> hitEventNames,
      float triggerChance,
      int effectDuration,
      int effectHealthChange,
      Random random) {
    if (hitEventNames == null || hitEventNames.isEmpty()) {
      throw new IllegalArgumentException("hitEventNames must not null or empty.");
    }
    if (triggerChance <= 0 || triggerChance > 1) {
      throw new IllegalArgumentException("Trigger chance probability must be within (0,1]");
    }
    if (effectDuration < 0) {
      throw new IllegalArgumentException("effectDuration must not be negative.");
    }
    this.hitEventNames = hitEventNames;
    this.triggerChance = triggerChance;
    this.effectDuration = effectDuration;
    this.effectHealthChange = effectHealthChange;
    this.random = random;
  }

  /**
   * Called when the entity is registered in the game world. Subscribes this component's
   * onMeleeAttackHit(Entity) handler to the configured hit event on the owning entity.
   */
  @Override
  public void create() {
    for (String eventName : hitEventNames) {
      entity.getEvents().addListener(eventName, this::onAttackHit);
    }
  }

  @Override
  public void update() {
    Iterator<ActiveEffect> iterator = activeEffects.iterator();
    while (iterator.hasNext()) {
      ActiveEffect effect = iterator.next();
      CombatStatsComponent targetStats = effect.target.getComponent(CombatStatsComponent.class);
      if (targetStats == null) {
        iterator.remove();
        continue;
      }

      effect.ticksRemaining--;
      int amount =
          (effect.getTicksRemaining() == 0)
              ? (effect.totalHealthChange - effect.totalApplied)
              : effect.healthPerTick;
      targetStats.addHealth(amount);
      effect.totalApplied += amount;

      if (effect.ticksRemaining <= 0) {
        iterator.remove();
      }
    }
  }

  /**
   * Event handler for the configured hit event. Rolls a random value against triggerChance; on
   * success, attaches a new HealthEffectComponent to target. On failure, or if target is null,
   * no-ops.
   *
   * @param target the entity that was hit (event payload).
   */
  public void onAttackHit(Entity target) {
    if (target == null) {
      return;
    }
    float roll = random.nextFloat();
    if (roll >= triggerChance) {
      return;
    }
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    if (targetStats == null) {
      return;
    }

    if (effectDuration == 0) {
      targetStats.addHealth(effectHealthChange);
      return;
    }

    activeEffects.add(new ActiveEffect(target, effectDuration, effectHealthChange));
  }

  /**
   * Tracks a single in-progress over-time effect this component is currently applying to a target.
   * Mirrors the per-tick math used by HealthEffectComponent (healthPerTick = totalHealthChange /
   * duration, remainder applied on the final tick so the total is exact).
   */
  private static class ActiveEffect {
    private final Entity target;
    private int healthPerTick;
    private int totalHealthChange;
    private int ticksRemaining;
    private int totalApplied = 0;

    private ActiveEffect(Entity target, int duration, int totalHealthChange) {
      this.target = target;
      this.ticksRemaining = duration;
      this.totalHealthChange = totalHealthChange;
      this.healthPerTick = totalHealthChange / duration;
    }

    public Entity getTarget() {
      return target;
    }

    public int getHealthPerTick() {
      return healthPerTick;
    }

    public int getTotalHealthChange() {
      return totalHealthChange;
    }

    public int getTicksRemaining() {
      return ticksRemaining;
    }

    public int getTotalApplied() {
      return totalApplied;
    }
  }
}
