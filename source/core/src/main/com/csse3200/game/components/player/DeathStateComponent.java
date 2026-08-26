package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;

/**
 * Handles the player's death state.
 *
 * <p>Listens for health updates and triggers the death event once
 * when the player's health reaches zero.
 */
public class DeathStateComponent extends Component {
    private boolean dead = false;

    @Override
    public void create() {
        entity.getEvents().addListener("updateHealth", this::checkDeath);

        CombatStatsComponent combatStats =
                entity.getComponent(CombatStatsComponent.class);

        if (combatStats != null) {
            checkDeath(combatStats.getHealth());
        }
    }

    /**
     * Checks whether the player should enter the death state.
     *
     * @param health player's current health
     */
    private void checkDeath(int health) {
        if (health <= 0 && !dead) {
            dead = true;
            entity.getEvents().trigger("death");
        }
    }

    /**
     * Returns whether the player has entered the death state.
     *
     * @return true if the player is dead
     */
    public boolean isDead() {
        return dead;
    }
}