package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;

public class StaminaComponent extends Component {

    private static final float MAX_STAMINA = 100f;
    private static final float DASH_COST = 20f;
    private static final float JUMP_COST = 10f;
    private static final float REGEN_RATE = 5f;

    private float stamina = MAX_STAMINA;

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
        entity.getEvents().trigger("updateStamina", stamina);
    }

    public void regenerate(float delta) {
        stamina = Math.min(MAX_STAMINA, stamina + delta);
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