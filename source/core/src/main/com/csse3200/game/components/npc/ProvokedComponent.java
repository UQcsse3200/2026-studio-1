package com.csse3200.game.components.npc;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

/** Provokes an NPC when its health drops, allowing one attack per fight or repeated attacks. */
public class ProvokedComponent extends Component {
  private final float calmDownSeconds;
  private final float repeatCooldownSeconds;
  private float provokedTimeRemaining = 0f;
  private float repeatTimeRemaining = 0f;
  private boolean attackPending = false;
  private boolean attacking = false;
  private boolean attackedThisFight = false;
  private int lastHealth;

  /** Creates a component that allows one retaliation attack per fight. */
  public ProvokedComponent(float calmDownSeconds) {
    this(calmDownSeconds, 0f);
  }

  /** Creates a component that attacks again this long after each attack while provoked. */
  public ProvokedComponent(float calmDownSeconds, float repeatCooldownSeconds) {
    this.calmDownSeconds = calmDownSeconds;
    this.repeatCooldownSeconds = repeatCooldownSeconds;
  }

  @Override
  public void create() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    lastHealth = stats == null ? 0 : stats.getHealth();
    entity.getEvents().addListener("updateHealth", this::onHealthChanged);
  }

  @Override
  public void update() {
    if (provokedTimeRemaining <= 0f) {
      return;
    }
    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    provokedTimeRemaining -= delta;
    if (provokedTimeRemaining <= 0f) {
      calmDown();
      return;
    }
    updateRepeat(delta);
  }

  /** Returns whether this NPC is still provoked by a recent hit. */
  public boolean isProvoked() {
    return provokedTimeRemaining > 0f;
  }

  /** Returns whether this fight's next attack is ready to start. */
  public boolean isAttackPending() {
    return attackPending;
  }

  /** Marks the ready attack as used, starting the repeat break if this NPC repeats attacks. */
  public void useAttack() {
    attackPending = false;
    attackedThisFight = true;
    repeatTimeRemaining = repeatCooldownSeconds;
  }

  /** Returns whether a retaliation attack is currently in progress. */
  public boolean isAttacking() {
    return attacking;
  }

  /** Marks whether a retaliation attack is currently in progress. */
  public void setAttacking(boolean attacking) {
    this.attacking = attacking;
  }

  /** Returns whether a repeating NPC should stay in fight mode between attacks. */
  public boolean isHoldingFight() {
    return repeatCooldownSeconds > 0f && attackedThisFight && isProvoked();
  }

  /** Counts down the break after an attack, readying the next one when it ends. */
  private void updateRepeat(float delta) {
    if (repeatCooldownSeconds <= 0f || !attackedThisFight || attacking || attackPending) {
      return;
    }
    repeatTimeRemaining -= delta;
    if (repeatTimeRemaining <= 0f) {
      attackPending = true;
    }
  }

  /** Ends the fight, cancelling any waiting attack. */
  private void calmDown() {
    attackPending = false;
    attackedThisFight = false;
    repeatTimeRemaining = 0f;
  }

  /** Starts a new fight on a hit while calm, and restarts the calm-down timer on any hit. */
  private void onHealthChanged(int newHealth) {
    if (newHealth < lastHealth) {
      if (!isProvoked()) {
        attackPending = true;
      }
      provokedTimeRemaining = calmDownSeconds;
    }
    lastHealth = newHealth;
  }
}
