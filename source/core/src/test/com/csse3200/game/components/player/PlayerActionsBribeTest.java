package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerActionsBribeTest {

  @Test
  void shouldSpendFiftyGoldAndBribeLastAttacker() {
    GameTime timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);
    InventoryComponent inventory = new InventoryComponent(60);
    PlayerActions actions = new PlayerActions();

    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(actions);
    player.create();

    MeleeAttackComponent meleeAttack = new MeleeAttackComponent(1f, 2f, 1f);
    Entity enemy =
        new Entity().addComponent(new CombatStatsComponent(100, 10)).addComponent(meleeAttack);
    enemy.create();

    player.getEvents().trigger("damagedBy", enemy);

    assertTrue(actions.bribeLastAttacker());
    assertTrue(meleeAttack.isBribed());
    assertEquals(10, inventory.getGold());
    assertFalse(actions.bribeLastAttacker());
  }

  @Test
  void shouldNotSpendGoldWhenPlayerCannotAffordBribe() {
    InventoryComponent inventory = new InventoryComponent(49);
    PlayerActions actions = new PlayerActions();

    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(inventory)
            .addComponent(actions);
    player.create();

    Entity enemy = new Entity().addComponent(new CombatStatsComponent(100, 10));
    enemy.create();

    player.getEvents().trigger("damagedBy", enemy);

    assertFalse(actions.bribeLastAttacker());
    assertEquals(49, inventory.getGold());
  }
}
