package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class InventoryComponentTest {
  @Test
  void shouldSetGetGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    assertEquals(100, inventory.getGold());

    inventory.setGold(150);
    assertEquals(150, inventory.getGold());

    inventory.setGold(-50);
    assertEquals(0, inventory.getGold());
  }

  @Test
  void shouldCheckHasGold() {
    InventoryComponent inventory = new InventoryComponent(150);
    assertTrue(inventory.hasGold(100));
    assertFalse(inventory.hasGold(200));
  }

  @Test
  void shouldAddGold() {
    InventoryComponent inventory = new InventoryComponent(100);
    inventory.addGold(-500);
    assertEquals(0, inventory.getGold());

    inventory.addGold(100);
    inventory.addGold(-20);
    assertEquals(80, inventory.getGold());
  }

  @Test
  void shouldSetGetTokens() {
    InventoryComponent inventory = new InventoryComponent(100, 25);
    assertEquals(25, inventory.getTokens());

    inventory.setTokens(50);
    assertEquals(50, inventory.getTokens());

    inventory.setTokens(-10);
    assertEquals(0, inventory.getTokens());
  }

  @Test
  void shouldCheckHasTokens() {
    InventoryComponent inventory = new InventoryComponent(100, 50);

    assertTrue(inventory.hasTokens(25));
    assertTrue(inventory.hasTokens(50));
    assertFalse(inventory.hasTokens(75));
  }

  @Test
  void shouldAddTokens() {
    InventoryComponent inventory = new InventoryComponent(100, 50);

    inventory.addTokens(25);
    assertEquals(75, inventory.getTokens());

    inventory.addTokens(-20);
    assertEquals(55, inventory.getTokens());

    inventory.addTokens(-100);
    assertEquals(0, inventory.getTokens());
  }

  @Test
  void shouldStartWithZeroTokensWhenOnlyGoldIsProvided() {
    InventoryComponent inventory = new InventoryComponent(100);

    assertEquals(100, inventory.getGold());
    assertEquals(0, inventory.getTokens());
  }
}
