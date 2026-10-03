package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeathLootDropComponentTest {

  private Application originalApplication;

  @BeforeEach
  void setUp() {
    originalApplication = Gdx.app;
    Gdx.app = mock(Application.class);

    doAnswer(
            invocation -> {
              Runnable runnable = invocation.getArgument(0);
              runnable.run();
              return null;
            })
        .when(Gdx.app)
        .postRunnable(org.mockito.ArgumentMatchers.any(Runnable.class));
  }

  @AfterEach
  void tearDown() {
    Gdx.app = originalApplication;
  }

  @Test
  void deathDropsAllInventoryStacksAndGold() {
    InventoryComponent inventory = mock(InventoryComponent.class);
    ItemDropComponent itemDrop = mock(ItemDropComponent.class);

    when(inventory.getOccupiedSlots()).thenReturn(3, 2, 1, 0);
    when(itemDrop.dropFirstStack()).thenReturn(true);

    Entity entity =
        new Entity()
            .addComponent(inventory)
            .addComponent(itemDrop)
            .addComponent(new DeathLootDropComponent());

    DeathLootDropComponent deathLoot = entity.getComponent(DeathLootDropComponent.class);
    deathLoot.create();

    entity.getEvents().trigger("death");

    verify(itemDrop, times(3)).dropFirstStack();
    verify(itemDrop, times(1)).dropGold();
  }

  @Test
  void repeatedDeathEventsDropLootOnlyOnce() {
    InventoryComponent inventory = mock(InventoryComponent.class);
    ItemDropComponent itemDrop = mock(ItemDropComponent.class);

    when(inventory.getOccupiedSlots()).thenReturn(2, 1, 0);
    when(itemDrop.dropFirstStack()).thenReturn(true);

    Entity entity =
        new Entity()
            .addComponent(inventory)
            .addComponent(itemDrop)
            .addComponent(new DeathLootDropComponent());

    DeathLootDropComponent deathLoot = entity.getComponent(DeathLootDropComponent.class);
    deathLoot.create();

    entity.getEvents().trigger("death");
    entity.getEvents().trigger("death");

    verify(itemDrop, times(2)).dropFirstStack();
    verify(itemDrop, times(1)).dropGold();
  }

  @Test
  void deathDoesNotThrowWhenLootComponentsAreMissing() {
    Entity entity = new Entity().addComponent(new DeathLootDropComponent());

    DeathLootDropComponent deathLoot = entity.getComponent(DeathLootDropComponent.class);
    deathLoot.create();

    assertDoesNotThrow(() -> entity.getEvents().trigger("death"));
  }
}
