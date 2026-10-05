package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

/** Damages a target entity gradually while a poison is active, shown as an icon above it. */
public class PoisonEffectComponent extends Component {
  /** Height above the target's feet where the poison icon sits. */
  private static final float INDICATOR_HEIGHT = 1.2f;

  private final Entity target;
  private final int damagePerTick;
  private final int totalTicks;
  private final float tickSeconds;
  private int ticksRemaining;
  private float timeSinceTick = 0f;

  /**
   * @param target the entity to poison
   * @param damagePerTick health removed each tick
   * @param totalTicks number of ticks the poison lasts
   * @param tickSeconds time between ticks, in seconds
   */
  public PoisonEffectComponent(
      Entity target, int damagePerTick, int totalTicks, float tickSeconds) {
    this.target = target;
    this.damagePerTick = damagePerTick;
    this.totalTicks = totalTicks;
    this.tickSeconds = tickSeconds;
    this.ticksRemaining = totalTicks;
  }

  /** Places the poison icon above the target. */
  @Override
  public void create() {
    followTarget();
  }

  /** Applies any ticks that have come due, then stops once none are left. */
  @Override
  public void update() {
    if (ticksRemaining <= 0) {
      return;
    }
    followTarget();
    timeSinceTick += ServiceLocator.getTimeSource().getDeltaTime();
    while (timeSinceTick >= tickSeconds && ticksRemaining > 0) {
      timeSinceTick -= tickSeconds;
      ticksRemaining--;
      applyTick();
    }
    if (ticksRemaining <= 0) {
      stopPoison();
    }
  }

  /** Restarts the full duration instead of stacking a second poison. */
  public void restart() {
    ticksRemaining = totalTicks;
    timeSinceTick = 0f;
  }

  /**
   * Returns whether the poison is still running.
   *
   * @return {@code true} while ticks are left to apply
   */
  public boolean isRunning() {
    return ticksRemaining > 0;
  }

  /** Removes one tick of health from the target. */
  private void applyTick() {
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats != null) {
      stats.addHealth(-damagePerTick);
    }
  }

  /** Ends the poison and removes its entity. */
  private void stopPoison() {
    Gdx.app.postRunnable(entity::dispose);
  }

  /** Keeps the poison icon centred above the target's head. */
  private void followTarget() {
    float x = target.getCenterPosition().x - entity.getScale().x / 2f;
    float y = target.getPosition().y + INDICATOR_HEIGHT;
    entity.setPosition(x, y);
  }
}
