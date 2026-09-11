package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.Test;

class PetComponentTest {

    @Test
    void shouldStoreOwner() {
        Entity owner = new Entity();

        PetComponent petComponent = new PetComponent(owner);

        assertEquals(owner, petComponent.getOwner());
    }

    @Test
    void shouldRejectNullOwner() {
        assertThrows(IllegalArgumentException.class, () -> new PetComponent(null));
    }
}