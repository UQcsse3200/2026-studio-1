package com.csse3200.game.components.npc;

import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyDeathComponentTest {
  private EntityService entityService;
  private ArgumentCaptor<Runnable> scheduledOnDeathCall;

  @BeforeEach
  void setUp() {
    // Mock the Gdx App to check for postRunnable calls.
    Application mockApp;
    mockApp = mock(Application.class);
    Gdx.app = mockApp;
    // Captor for the "this::onDeath" in EnemyDeathComponent's onDeathWrapper
    scheduledOnDeathCall = ArgumentCaptor.forClass(Runnable.class);
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);
  }

  @Test
  void shouldDropLootOnDeath() {
    Entity entity = new Entity();
    ItemDropComponent dropper = mock(ItemDropComponent.class);
    when(dropper.getPrio()).thenReturn(ComponentPriority.LOW);
    when(dropper.dropFirstStack()).thenReturn(true, false); // one item, then stop

    entity.addComponent(dropper);
    entity.addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

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
    runScheduledImmediately();

    verify(entityService).unregister(entity);
  }

  @Test
  void shouldDisposeEvenWithoutItemDropComponent() {
    Entity entity = new Entity();
    entity.addComponent(new EnemyDeathComponent());
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    verify(entityService).unregister(entity);
  }

  /** Runs scheduled Runnable immediately, as opposed to waiting for it */
  private void runScheduledImmediately() {
    verify(Gdx.app)
        .postRunnable(scheduledOnDeathCall.capture()); // Gets method trying to be scheduled
    scheduledOnDeathCall.getValue().run(); // Runs it immediately instead
  }
}
