package com.csse3200.game.components.enemies;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.GameTime;
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

    entity
        .addComponent(dropper)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new EnemyTypeComponent(EnemyType.SKELETON));
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
    entity
        .addComponent(new EnemyDeathComponent())
        .addComponent(new EnemyTypeComponent(EnemyType.SKELETON));
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    verify(entityService).unregister(entity);
  }

  @Test
  void shouldDisposeEvenWithoutItemDropComponent() {
    Entity entity = new Entity();
    entity
        .addComponent(new EnemyDeathComponent())
        .addComponent(new EnemyTypeComponent(EnemyType.SKELETON));
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    verify(entityService).unregister(entity);
  }

  @Test
  void shouldDelayDisposeWhenDeathAnimationPlaying() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.getCurrentAnimation()).thenReturn("zeus_death_r");
    when(animator.isFinished()).thenReturn(false);

    EnemyDeathComponent deathComponent = new EnemyDeathComponent();
    Entity entity = new Entity();
    entity
        .addComponent(animator)
        .addComponent(deathComponent)
        .addComponent(new EnemyTypeComponent(EnemyType.ZEUS));
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    assertTrue(deathComponent.isDying());
    verify(entityService, never()).unregister(entity);

    when(animator.isFinished()).thenReturn(true);
    deathComponent.update();

    runScheduledImmediately();
    verify(entityService).unregister(entity);
    assertFalse(deathComponent.isDying());
  }

  @Test
  void shouldDisableCollidersAndAttacksWhenDying() {
    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.getCurrentAnimation()).thenReturn("centaur_death_l");
    when(animator.isFinished()).thenReturn(false);

    ColliderComponent collider = mock(ColliderComponent.class);
    HitboxComponent hitbox = mock(HitboxComponent.class);
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);
    when(physics.getBody()).thenReturn(body);

    EnemyDeathComponent deathComponent = new EnemyDeathComponent();
    Entity entity = new Entity();
    entity
        .addComponent(animator)
        .addComponent(collider)
        .addComponent(hitbox)
        .addComponent(physics)
        .addComponent(deathComponent)
        .addComponent(new EnemyTypeComponent(EnemyType.CENTAUR));
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    assertTrue(deathComponent.isDying());
    verify(body).setLinearVelocity(0f, 0f);
    verify(collider).setSensor(true);
    verify(hitbox).setLayer(PhysicsLayer.NONE);
  }

  @Test
  void shouldDisposeAfterTimeoutIfAnimationNeverFinishes() {
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(5.0f);
    ServiceLocator.registerTimeSource(gameTime);

    AnimationRenderComponent animator = mock(AnimationRenderComponent.class);
    when(animator.getCurrentAnimation()).thenReturn("cyclops_death_r");
    when(animator.isFinished()).thenReturn(false);

    EnemyDeathComponent deathComponent = new EnemyDeathComponent();
    Entity entity = new Entity();
    entity
        .addComponent(animator)
        .addComponent(deathComponent)
        .addComponent(new EnemyTypeComponent(EnemyType.CYCLOPS));
    entity.create();

    entity.getEvents().trigger("death");
    runScheduledImmediately();

    assertTrue(deathComponent.isDying());

    deathComponent.update();
    runScheduledImmediately();

    verify(entityService).unregister(entity);
    assertFalse(deathComponent.isDying());
  }

  /** Runs scheduled Runnable immediately, as opposed to waiting for it */
  private void runScheduledImmediately() {
    verify(Gdx.app, atLeastOnce())
        .postRunnable(scheduledOnDeathCall.capture()); // Gets method trying to be scheduled
    scheduledOnDeathCall.getValue().run(); // Runs it immediately instead
  }
}
