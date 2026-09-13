package com.csse3200.game.components.npc;

import static org.mockito.Mockito.*;

import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EnemyDeathComponentTest {
  private EntityService entityService;

  @BeforeEach
  void setUp() {
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
  }

  @Test
  void shouldDropLootOnDeath() {
    Entity entity = new Entity();
    ItemDropComponent dropper = mock(ItemDropComponent.class);
    when(dropper.dropFirstStack()).thenReturn(true, false); // one item, then stop

    entity.addComponent(dropper);
    entity.addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("death");

    verify(dropper).dropGold();
    verify(dropper, times(2)).dropFirstStack();
  }

  @Test
  void shouldDisposeEnemyOnDeath() {
    ServiceLocator.registerEntityService(entityService);

    Entity entity = new Entity();
    entity.addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("death");

    verify(entityService).unregister(entity);
  }

  @Test
  void shouldDisposeEvenWithoutItemDropComponent() {
    Entity entity = new Entity();
    entity.addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("death");

    verify(entityService).unregister(entity);
  }
}
