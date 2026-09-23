package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;

public class StaminaComponent extends Component {

  private static final float MAX_STAMINA = 100f;
  private static final float DASH_COST = 20f;
  private static final float JUMP_COST = 10f;
  private static final float REGEN_RATE = 5f;
  private static final float REGEN_DELAY = 3f;

  private float stamina = MAX_STAMINA;
  private float regenDelayRemaining = 0f;

  public float getStamina() {
    return stamina;
  }

  public float getMaxStamina() {
    return MAX_STAMINA;
  }

  public boolean hasEnoughStamina(float amount) {
    return stamina >= amount;
  }

  public void useStamina(float amount) {
    stamina = Math.max(0f, stamina - amount);
    regenDelayRemaining = REGEN_DELAY;
    entity.getEvents().trigger("updateStamina", stamina);
  }

  public void regenerate(float delta) {
    if (regenDelayRemaining > 0f) {
      regenDelayRemaining = Math.max(0f, regenDelayRemaining - delta);
      return;
    }

    stamina = Math.min(MAX_STAMINA, stamina + REGEN_RATE * delta);
    entity.getEvents().trigger("updateStamina", stamina);
  }

  public float getDashCost() {
    return DASH_COST;
  }

  public float getJumpCost() {
    return JUMP_COST;
  }

  public float getRegenRate() {
    return REGEN_RATE;
  }
}
