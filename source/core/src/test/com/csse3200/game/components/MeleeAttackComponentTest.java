package com.csse3200.game.components;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class MeleeAttackComponentTest {

    @BeforeEach
    void beforeEach() {
        // TODO: register any services this component depends on, e.g. PhysicsService
    }

    @Test
    public void shouldAttackWhenInRangeAndOffCooldown() {
        // TODO
    }

    @Test
    public void shouldNotAttackWhenOutOfRange() {
        // TODO
    }

    @Test
    public void shouldNotAttackDuringCooldown() {
        // TODO
    }

    @Test
    public void shouldNotAttackOtherLayer() {
        // TODO
    }

    @Test
    public void shouldNotAttackWithoutCombatStatsComponent() {
        // TODO
    }

    Entity createAttacker(short targetLayer) {
        // TODO: mirror TouchAttackComponentTEst.createAttacker pattern
        return null;
    }

    Entity createTarget(short layer) {
        // TODO: mirror TouchAttackComponentTest.createTarget pattern
        return null;
    }
}
