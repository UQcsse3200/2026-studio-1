package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NameComponentTest {

    @Test
    void shouldStoreName() {
        NameComponent component = new NameComponent("Grimfang");

        assertEquals("Grimfang", component.getName());
    }

    @Test
    void shouldUpdateName() {
        NameComponent component = new NameComponent("Grimfang");

        component.setName("Ashbane");

        assertEquals("Ashbane", component.getName());
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(IllegalArgumentException.class, () -> new NameComponent(null));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new NameComponent("   "));
    }
}