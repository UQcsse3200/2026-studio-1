package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemTest {

    private Item consumable;
    private Item weapon;
    private Item currency;

    @BeforeEach
    void setUp() {
        consumable = new Item("Health Potion", ItemType.CONSUMABLE, 5, 10);
        weapon = new Item("Sword", ItemType.WEAPON, 1, 1);
        currency = new Item("Gold Coin", ItemType.CURRENCY, 10, 100);
    }

    @Test
    void shouldCreateItemCorrectly() {
        assertEquals("Health Potion", consumable.getName());
        assertEquals(ItemType.CONSUMABLE, consumable.getItemType());
        assertEquals(5, consumable.getQuantity());
        assertEquals(10, consumable.getMaxQuantity());
    }

    @Test
    void shouldAssignUniqueIds() {
        assertNotEquals(consumable.getId(), weapon.getId());
        assertNotEquals(weapon.getId(), currency.getId());
    }

    @Test
    void shouldClampQuantityToMaximum() {
        consumable.setQuantity(20);

        assertEquals(10, consumable.getQuantity());
    }

    @Test
    void shouldClampQuantityToZero() {
        consumable.setQuantity(-5);

        assertEquals(0, consumable.getQuantity());
    }

    @Test
    void shouldAddQuantity() {
        consumable.addQuantity(3);

        assertEquals(8, consumable.getQuantity());
    }

    @Test
    void shouldNotExceedMaximumWhenAddingQuantity() {
        consumable.addQuantity(20);

        assertEquals(10, consumable.getQuantity());
    }

    @Test
    void shouldIdentifyFullStack() {
        consumable.setQuantity(10);

        assertTrue(consumable.isStackFull());
    }

    @Test
    void shouldIdentifyEmptyStack() {
        consumable.setQuantity(0);

        assertTrue(consumable.isEmpty());
    }

    @Test
    void shouldRejectInvalidMaxQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item("Invalid", ItemType.CONSUMABLE, 1, 0));
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item("", ItemType.CONSUMABLE, 1, 10));
    }

    @Test
    void shouldRejectNullItemType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Item("Test", null, 1, 10));
    }
}