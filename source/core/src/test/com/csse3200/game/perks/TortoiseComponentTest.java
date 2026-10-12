package com.csse3200.game.perks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.win.TortoiseLedger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Tests for {@link TortoiseComponent}. Requires Mockito's inline mock maker for mockStatic. */
class TortoiseComponentTest {
  private static final String ID = "level1_a";

  private MockedStatic<TortoiseRegistry> registry;
  private MockedStatic<TortoiseLedger> ledger;
  private MockedStatic<PerkService> perkService;

  private Application previousApp;
  private Application app;
  private Entity entity;
  private EventHandler events;
  private HitboxComponent hitbox;
  private Fixture ownFixture;
  private Fixture playerFixture;

  @BeforeEach
  void setUp() {
    registry = Mockito.mockStatic(TortoiseRegistry.class);
    ledger = Mockito.mockStatic(TortoiseLedger.class);
    perkService = Mockito.mockStatic(PerkService.class);

    previousApp = Gdx.app;
    app = mock(Application.class);
    Gdx.app = app;

    ownFixture = mock(Fixture.class);
    hitbox = mock(HitboxComponent.class);
    when(hitbox.getFixture()).thenReturn(ownFixture);

    playerFixture = fixtureOnLayer(PhysicsLayer.PLAYER);

    events = new EventHandler();
    entity = mock(Entity.class);
    when(entity.getEvents()).thenReturn(events);
    when(entity.getComponent(HitboxComponent.class)).thenReturn(hitbox);
  }

  @AfterEach
  void tearDown() {
    Gdx.app = previousApp;
    perkService.close();
    ledger.close();
    registry.close();
  }

  private static Fixture fixtureOnLayer(short layer) {
    Fixture fixture = mock(Fixture.class);
    Filter filter = new Filter();
    filter.categoryBits = layer;
    when(fixture.getFilterData()).thenReturn(filter);
    return fixture;
  }

  private TortoiseComponent createComponent() {
    TortoiseComponent component = new TortoiseComponent(ID);
    component.setEntity(entity);
    component.create();
    return component;
  }

  private void collide(Fixture me, Fixture other) {
    events.trigger("collisionStart", me, other);
  }

  @Test
  void playerTouchingMarksFoundAndRecordsProgress() {
    createComponent();

    collide(ownFixture, playerFixture);

    registry.verify(() -> TortoiseRegistry.markFound(ID));
    ledger.verify(() -> TortoiseLedger.recordFound(ID));
    perkService.verify(() -> PerkService.recordEvent("Tortoise", 1));
  }

  @Test
  void playerTouchingSchedulesEntityDisposal() {
    createComponent();

    collide(ownFixture, playerFixture);

    ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
    verify(app).postRunnable(runnable.capture());
    verify(entity, never()).dispose();

    runnable.getValue().run();

    verify(entity).dispose();
  }

  @Test
  void collectingTwiceOnlyCountsOnce() {
    createComponent();

    collide(ownFixture, playerFixture);
    collide(ownFixture, playerFixture);

    registry.verify(() -> TortoiseRegistry.markFound(ID), times(1));
    perkService.verify(() -> PerkService.recordEvent("Tortoise", 1), times(1));
    verify(app, times(1)).postRunnable(any(Runnable.class));
  }

  @Test
  void nonPlayerTouchingIsIgnored() {
    createComponent();
    Fixture enemy = fixtureOnLayer(PhysicsLayer.NPC);

    collide(ownFixture, enemy);

    registry.verifyNoInteractions();
    ledger.verifyNoInteractions();
    perkService.verifyNoInteractions();
    verify(app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void collisionOnSomeOtherFixtureOfTheEntityIsIgnored() {
    createComponent();
    Fixture notTheHitbox = mock(Fixture.class);

    collide(notTheHitbox, playerFixture);

    registry.verifyNoInteractions();
    perkService.verifyNoInteractions();
  }

  @Test
  void ignoresCollisionsWhenEntityHasNoHitbox() {
    when(entity.getComponent(HitboxComponent.class)).thenReturn(null);
    createComponent();

    collide(ownFixture, playerFixture);

    registry.verifyNoInteractions();
    perkService.verifyNoInteractions();
  }

  @Test
  void nothingHappensBeforeAnyCollision() {
    createComponent();

    registry.verifyNoInteractions();
    ledger.verifyNoInteractions();
    perkService.verifyNoInteractions();
  }
}
