package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaminaComponentTest {

    private StaminaComponent staminaComponent;

    @BeforeEach
    void setUp() {
        staminaComponent = new StaminaComponent();
        staminaComponent.setEntity(new Entity());
    }

    @Test
    void testInitialStamina() {
        assertEquals(100f, staminaComponent.getStamina());
    }

    @Test
    void testMaximumStamina() {
        assertEquals(100f, staminaComponent.getMaxStamina());
    }

    @Test
    void testHasEnoughStamina() {
        assertTrue(staminaComponent.hasEnoughStamina(50f));
        assertTrue(staminaComponent.hasEnoughStamina(100f));
        assertFalse(staminaComponent.hasEnoughStamina(101f));
    }

    @Test
    void testUseStamina() {
        staminaComponent.useStamina(20f);

        assertEquals(80f, staminaComponent.getStamina());
    }

    @Test
    void testUseStaminaCannotGoBelowZero() {
        staminaComponent.useStamina(150f);

        assertEquals(0f, staminaComponent.getStamina());
    }

    @Test
    void testRegenerateStamina() {
        staminaComponent.useStamina(40f);

        staminaComponent.regenerate(20f);

        assertEquals(80f, staminaComponent.getStamina());
    }

    @Test
    void testRegenerateCannotExceedMaximum() {
        staminaComponent.regenerate(50f);

        assertEquals(100f, staminaComponent.getStamina());
    }

    @Test
    void testDashCost() {
        assertEquals(20f, staminaComponent.getDashCost());
    }

    @Test
    void testJumpCost() {
        assertEquals(10f, staminaComponent.getJumpCost());
    }

    @Test
    void testRegenerationRate() {
        assertEquals(5f, staminaComponent.getRegenRate());
    }
}