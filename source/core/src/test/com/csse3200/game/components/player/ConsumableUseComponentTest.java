package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.HealEffect;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ConsumableUseComponentTest {
  private static final int MAX_HEALTH = 100;

  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);
  }

  /**
   * Builds a player with an inventory, combat stats and consumable support.
   *
   * @param health starting health
   * @return a created entity
   */
  private Entity makePlayer(int health) {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 10))
            .addComponent(new InventoryComponent(0, 5))
            .addComponent(new PlayerBuffComponent())
            .addComponent(new ConsumableUseComponent(MAX_HEALTH));
    player.create();
    return player;
  }

  /**
   * Builds a health potion.
   *
   * @param quantity starting stack size
   * @return the potion
   */
  private ConsumableItem healthPotion(int quantity) {
    return new ConsumableItem(
        "Health Potion", ConsumableType.HEALTH_POTION, new HealEffect(25), quantity, 9);
  }

  /** Acceptance criterion: a used consumable is removed from the player's inventory. */
  @Test
  void shouldRemoveConsumableFromInventoryAfterUse() {
    Entity player = makePlayer(50);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(healthPotion(1));

    assertTrue(inventory.containsItem(1));
    player.getEvents().trigger("useItem", 1);

    assertEquals(75, player.getComponent(CombatStatsComponent.class).getHealth());
    assertFalse(inventory.containsItem(1));
  }

  /** Acceptance criterion: at full health the potion is not consumed. */
  @Test
  void shouldNotConsumePotionAtFullHealth() {
    Entity player = makePlayer(MAX_HEALTH);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(healthPotion(1));

    player.getEvents().trigger("useItem", 1);

    assertEquals(MAX_HEALTH, player.getComponent(CombatStatsComponent.class).getHealth());
    assertTrue(inventory.containsItem(1));
    assertEquals(1, inventory.getItem(1).getQuantity());
  }

  @Test
  void shouldRemoveOnlyOneFromAStack() {
    Entity player = makePlayer(50);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    inventory.addItem(healthPotion(3));

    player.getEvents().trigger("useItem", 1);

    assertTrue(inventory.containsItem(1));
    assertEquals(2, inventory.getItem(1).getQuantity());
  }

  @Test
  void shouldApplyAndRevertBuffFromInventoryItem() {
    Entity player = makePlayer(50);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    inventory.addItem(
        new ConsumableItem(
            "Damage Potion",
            ConsumableType.DAMAGE_BUFF,
            new com.csse3200.game.components.loot.BuffEffect(BuffStat.DAMAGE, 2f, 5f),
            1,
            9));

    player.getEvents().trigger("useItem", 1);

    assertEquals(20, stats.getBaseAttack());
    assertFalse(inventory.containsItem(1));

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(10, stats.getBaseAttack());
  }

  @Test
  void shouldTriggerItemConsumedEvent() {
    Entity player = makePlayer(50);
    player.getComponent(InventoryComponent.class).addItem(healthPotion(1));

    List<ConsumableItem> consumed = new ArrayList<>();
    player.getEvents().addListener("itemConsumed", (ConsumableItem item) -> consumed.add(item));

    player.getEvents().trigger("useItem", 1);

    assertEquals(1, consumed.size());
    assertEquals("Health Potion", consumed.get(0).getName());
  }

  @Test
  void shouldIgnoreEmptyInvalidAndNonConsumableSlots() {
    Entity player = makePlayer(50);
    InventoryComponent inventory = player.getComponent(InventoryComponent.class);
    ConsumableUseComponent use = player.getComponent(ConsumableUseComponent.class);
    inventory.addItem(new Item("Sword", ItemType.WEAPON, 1, 1));

    assertFalse(use.useItem(1));
    assertFalse(use.useItem(2));
    assertFalse(use.useItem(0));
    assertFalse(use.useItem(99));
    assertTrue(inventory.containsItem(1));
  }

  @Test
  void shouldReturnFalseWithoutInventory() {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(50, 10))
            .addComponent(new ConsumableUseComponent(MAX_HEALTH));
    player.create();

    assertFalse(player.getComponent(ConsumableUseComponent.class).useItem(1));
  }

  @Test
  void shouldExposeMaxHealthAndRejectInvalidValues() {
    assertEquals(MAX_HEALTH, new ConsumableUseComponent(MAX_HEALTH).getMaxHealth());
    assertThrows(IllegalArgumentException.class, () -> new ConsumableUseComponent(0));
    assertThrows(IllegalArgumentException.class, () -> new ConsumableUseComponent(-1));
  }
}
