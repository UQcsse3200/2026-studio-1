package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.win.TortoiseLedger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link TortoiseComponent}: touching a tortoise as the player marks it found in the registry
 * and the ledger, reports one unit of perk progress, and removes the tortoise, all once. Anything
 * that is not the player touching the tortoise's own hitbox does nothing.
 *
 * <p>Gdx.app is a mock during a collision so the removal is posted rather than run on another
 * thread; the posted task is run by hand where its effect matters.
 */
@ExtendWith(GameExtension.class)
class TortoiseComponentTest {
  private static final String ID = "level1_a";

  private Application app;

  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoiseFactory.resetAll();
    TortoiseLedger.reset();
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(mock(EntityService.class));
    app = mock(Application.class);
    PerkService.resetAll();
  }

  @AfterEach
  void tearDown() {
    TortoiseFactory.resetAll();
    TortoiseLedger.reset();
    PerkService.resetAll();
  }

  private Entity tortoise() {
    Entity tortoise =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
            .addComponent(new TortoiseComponent(ID));
    tortoise.create();
    return tortoise;
  }

  private Entity entityOnLayer(short layer) {
    Entity entity =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(layer));
    entity.create();
    return entity;
  }

  private static Fixture hitbox(Entity entity) {
    return entity.getComponent(HitboxComponent.class).getFixture();
  }

  private void collide(Entity tortoise, Fixture me, Fixture other) {
    TortoiseTestSupport.withApp(
        app, () -> tortoise.getEvents().trigger("collisionStart", me, other));
  }

  private void touch(Entity tortoise, Entity player) {
    collide(tortoise, hitbox(tortoise), hitbox(player));
  }

  @Test
  void shouldMarkTheTortoiseFoundWhenThePlayerTouchesIt() {
    Entity tortoise = tortoise();

    touch(tortoise, entityOnLayer(PhysicsLayer.PLAYER));

    assertTrue(TortoiseFactory.isFound(ID));
    assertEquals(1, TortoiseLedger.getFoundCount());
    assertTrue(TortoiseLedger.exportAll().contains(ID));
  }

  @Test
  void shouldReportOnePerkProgress() {
    Perk perk = new Perk("tortoiseTestPerkReport", "Test", "Test", "Tortoise", 5);
    PerkService.register(perk);
    int before = perk.getProgress();

    touch(tortoise(), entityOnLayer(PhysicsLayer.PLAYER));

    assertEquals(before + 1, perk.getProgress());
  }

  @Test
  void shouldRemoveTheTortoiseOnTheRenderThread() {
    Entity tortoise = tortoise();

    touch(tortoise, entityOnLayer(PhysicsLayer.PLAYER));

    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(app).postRunnable(task.capture());
    task.getValue().run(); // disposing the tortoise must not throw
  }

  @Test
  void shouldCollectOnlyOnce() {
    Perk perk = new Perk("tortoiseTestPerkOnce", "Test", "Test", "Tortoise", 5);
    PerkService.register(perk);
    int before = perk.getProgress();
    Entity tortoise = tortoise();
    Entity player = entityOnLayer(PhysicsLayer.PLAYER);

    touch(tortoise, player);
    touch(tortoise, player);

    assertEquals(before + 1, perk.getProgress());
    assertEquals(1, TortoiseLedger.getFoundCount());
    verify(app, times(1)).postRunnable(any());
  }

  @Test
  void shouldIgnoreAnythingThatIsNotThePlayer() {
    Entity tortoise = tortoise();

    touch(tortoise, entityOnLayer(PhysicsLayer.NPC));

    assertFalse(TortoiseFactory.isFound(ID));
    assertEquals(0, TortoiseLedger.getFoundCount());
    verify(app, never()).postRunnable(any());
  }

  @Test
  void shouldIgnoreACollisionOnAnotherFixture() {
    Entity tortoise = tortoise();
    Entity player = entityOnLayer(PhysicsLayer.PLAYER);
    Entity bystander = entityOnLayer(PhysicsLayer.ITEM);

    collide(tortoise, hitbox(bystander), hitbox(player));

    assertFalse(TortoiseFactory.isFound(ID));
    verify(app, never()).postRunnable(any());
  }

  @Test
  void shouldIgnoreACollisionWhenItHasNoHitbox() {
    Entity tortoise =
        new Entity().addComponent(new PhysicsComponent()).addComponent(new TortoiseComponent(ID));
    tortoise.create();
    Entity player = entityOnLayer(PhysicsLayer.PLAYER);

    collide(tortoise, mock(Fixture.class), hitbox(player));

    assertFalse(TortoiseFactory.isFound(ID));
    assertEquals(0, TortoiseLedger.getFoundCount());
  }
}
