package com.csse3200.game.components.npc;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Provokes an NPC when its health drops, allowing one retaliation throw per fight. */
public class ProvokedComponent extends Component {
  private final float calmDownSeconds;
  private float provokedTimeRemaining = 0f;
  private boolean throwPending = false;
  private int lastHealth;

  public ProvokedComponent(float calmDownSeconds) {
    this.calmDownSeconds = calmDownSeconds;
  }

  @Override
  public void create() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    lastHealth = stats == null ? 0 : stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthChanged);
  }

  @Override
  public void update() {
    if (provokedTimeRemaining > 0f) {
      provokedTimeRemaining -= ServiceLocator.getTimeSource().getDeltaTime();
      if (provokedTimeRemaining <= 0f) {
        throwPending = false;
      }
    }
  }

  /** Returns whether this NPC is still provoked by a recent hit. */
  public boolean isProvoked() {
    return provokedTimeRemaining > 0f;
  }

  /** Returns whether this fight's throw has not been used yet. */
  public boolean isThrowPending() {
    return throwPending;
  }

  /** Marks this fight's throw as used. */
  public void useThrow() {
    throwPending = false;
  }

  /** Starts a new fight on a hit while calm, and restarts the calm-down timer on any hit. */
  private void onHealthChanged(int newHealth) {
    if (newHealth < lastHealth) {
      if (!isProvoked()) {
        throwPending = true;
      }
      provokedTimeRemaining = calmDownSeconds;
    }
    lastHealth = newHealth;
  }
}
