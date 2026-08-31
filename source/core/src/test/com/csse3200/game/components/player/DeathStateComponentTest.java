package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class DeathStateComponentTest {
    private GameTime gameTime;

    @BeforeEach
    void setUp() {
        gameTime = mock(GameTime.class);
        ServiceLocator.registerTimeSource(gameTime);
    }

    @Test
    void shouldNotDieAboveZeroHealth() {
        CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
        DeathStateComponent deathState = new DeathStateComponent();

        Entity player =
                new Entity()
                        .addComponent(combatStats)
                        .addComponent(deathState);

        player.create();

        combatStats.setHealth(50);

        assertFalse(deathState.isDead());
        verify(gameTime, never()).setTimeScale(0f);
    }

    @Test
    void shouldDieAtZeroHealth() {
        CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
        DeathStateComponent deathState = new DeathStateComponent();

        Entity player =
                new Entity()
                        .addComponent(combatStats)
                        .addComponent(deathState);

        player.create();

        combatStats.setHealth(0);

        assertTrue(deathState.isDead());
        verify(gameTime).setTimeScale(0f);
    }

    @Test
    void shouldOnlyTriggerDeathOnce() {
        CombatStatsComponent combatStats = new CombatStatsComponent(100, 10);
        DeathStateComponent deathState = new DeathStateComponent();

        Entity player =
                new Entity()
                        .addComponent(combatStats)
                        .addComponent(deathState);

        player.create();

        combatStats.setHealth(0);
        combatStats.setHealth(0);
        combatStats.addHealth(-10);

        assertTrue(deathState.isDead());

        verify(gameTime, times(1)).setTimeScale(0f);
    }
}