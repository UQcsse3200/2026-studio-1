package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.enemies.EnemyDeathComponent;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.components.enemies.attacks.RangedAttackComponent;
import com.csse3200.game.components.enemies.attacks.TouchAttackComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests for {@link TimeFreezeComponent}. Time is driven by stubbing {@code GameTime.getDeltaTime()}
 * at 1s per update, so a 5s freeze ends on the 5th update and the 20s cooldown on the 20th.
 */
class TimeFreezeComponentTest {
  private static final int FREEZE_UPDATES = 5;
  private static final int COOLDOWN_UPDATES = 20;

  private MockedStatic<PerkService> perkService;
  private MockedStatic<ServiceLocator> serviceLocator;

  private Perk perk;
  private GameTime time;
  private EntityService entityService;
  private List<Entity> enemies;
  private EventHandler events;
  private TimeFreezeComponent component;
  private final AtomicReference<Object> onDeactivated = new AtomicReference<>();
  private final AtomicInteger activatedCount = new AtomicInteger();
  private final AtomicInteger endedCount = new AtomicInteger();
  private final AtomicReference<Float> activatedDuration = new AtomicReference<>();

  /** A mocked enemy plus handles to the pieces the component touches. */
  private static final class EnemyFixture {
    final Entity entity = mock(Entity.class);
    final AITaskComponent ai = mock(AITaskComponent.class);
    final PhysicsMovementComponent movement = mock(PhysicsMovementComponent.class);
    final MeleeAttackComponent melee = mock(MeleeAttackComponent.class);
    final RangedAttackComponent ranged = mock(RangedAttackComponent.class);
    final TouchAttackComponent touch = mock(TouchAttackComponent.class);
    final PhysicsComponent physics = mock(PhysicsComponent.class);
    final Body body = mock(Body.class);

    EnemyFixture() {
      when(entity.isDisposed()).thenReturn(false);
      when(entity.getComponent(AITaskComponent.class)).thenReturn(ai);
      when(entity.getComponent(PhysicsMovementComponent.class)).thenReturn(movement);
      when(entity.getComponent(MeleeAttackComponent.class)).thenReturn(melee);
      when(entity.getComponent(RangedAttackComponent.class)).thenReturn(ranged);
      when(entity.getComponent(TouchAttackComponent.class)).thenReturn(touch);
      when(entity.getComponent(PhysicsComponent.class)).thenReturn(physics);
      when(physics.getBody()).thenReturn(body);
    }

    void verifyFrozen() {
      verify(ai).setEnabled(false);
      verify(movement).setEnabled(false);
      verify(melee).setEnabled(false);
      verify(ranged).setEnabled(false);
      verify(touch).setEnabled(false);
    }

    void verifyThawed() {
      verify(ai).setEnabled(true);
      verify(movement).setEnabled(true);
      verify(melee).setEnabled(true);
      verify(ranged).setEnabled(true);
      verify(touch).setEnabled(true);
    }
  }

  @BeforeEach
  void setUp() {
    perkService = Mockito.mockStatic(PerkService.class);
    serviceLocator = Mockito.mockStatic(ServiceLocator.class);

    perk = mock(Perk.class);
    when(perk.isActive()).thenReturn(true);
    doAnswer(
            inv -> {
              onDeactivated.set(inv.getArgument(0));
              return null;
            })
        .when(perk)
        .setOnDeactivated(any());
    perkService.when(() -> PerkService.getPerk("timeLord")).thenReturn(perk);

    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(1f);
    serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(time);

    enemies = new ArrayList<>();
    entityService = mock(EntityService.class);
    when(entityService.getEntitiesWithComponent(EnemyDeathComponent.class)).thenReturn(enemies);
    serviceLocator.when(ServiceLocator::getEntityService).thenReturn(entityService);

    events = new EventHandler();
    events.addListener(
        "timeFreezeActivated",
        (Float duration) -> {
          activatedCount.incrementAndGet();
          activatedDuration.set(duration);
        });
    events.addListener("timeFreezeEnded", endedCount::incrementAndGet);

    Entity owner = mock(Entity.class);
    when(owner.getEvents()).thenReturn(events);
    component = new TimeFreezeComponent();
    component.setEntity(owner);
    component.create();
  }

  @AfterEach
  void tearDown() {
    serviceLocator.close();
    perkService.close();
  }

  private EnemyFixture addEnemy() {
    EnemyFixture enemy = new EnemyFixture();
    enemies.add(enemy.entity);
    return enemy;
  }

  private void press() {
    events.trigger("activateTimeFreeze");
  }

  private void updateTimes(int n) {
    for (int i = 0; i < n; i++) {
      component.update();
    }
  }

  private void deactivatePerk() {
    ((Runnable) onDeactivated.get()).run();
  }

  @Test
  void createRegistersDeactivationCallback() {
    assertNotNull(onDeactivated.get());
  }

  @Test
  void createToleratesMissingPerk() {
    perkService.when(() -> PerkService.getPerk("timeLord")).thenReturn(null);
    Entity owner = mock(Entity.class);
    when(owner.getEvents()).thenReturn(new EventHandler());
    TimeFreezeComponent other = new TimeFreezeComponent();
    other.setEntity(owner);

    assertDoesNotThrow(other::create);
  }

  @Test
  void pressIsIgnoredWhenPerkMissing() {
    EnemyFixture enemy = addEnemy();
    perkService.when(() -> PerkService.getPerk("timeLord")).thenReturn(null);

    press();

    verify(enemy.ai, never()).setEnabled(false);
    assertEquals(0, activatedCount.get());
  }

  @Test
  void pressIsIgnoredWhenPerkInactive() {
    EnemyFixture enemy = addEnemy();
    when(perk.isActive()).thenReturn(false);

    press();

    verify(enemy.ai, never()).setEnabled(false);
    assertEquals(0, activatedCount.get());
  }

  @Test
  void pressFreezesEveryEnemyBehaviourAndStopsMomentum() {
    EnemyFixture a = addEnemy();
    EnemyFixture b = addEnemy();

    press();

    a.verifyFrozen();
    b.verifyFrozen();
    verify(a.body).setLinearVelocity(0f, 0f);
    verify(b.body).setLinearVelocity(0f, 0f);
  }

  @Test
  void pressFiresActivatedEventWithDuration() {
    press();

    assertEquals(1, activatedCount.get());
    assertEquals(5f, activatedDuration.get());
  }

  @Test
  void freezeToleratesEnemiesMissingSomeComponents() {
    Entity bare = mock(Entity.class);
    when(bare.isDisposed()).thenReturn(false);
    enemies.add(bare);

    assertDoesNotThrow(this::press);
    assertEquals(1, activatedCount.get());
  }

  @Test
  void pressWhileFreezingDoesNothing() {
    addEnemy();

    press();
    press();

    assertEquals(1, activatedCount.get());
  }

  @Test
  void disposedEnemiesAreNotFrozen() {
    EnemyFixture enemy = addEnemy();
    when(enemy.entity.isDisposed()).thenReturn(true);

    press();

    verify(enemy.ai, never()).setEnabled(false);
  }

  @Test
  void alreadyFrozenEnemiesAreNotReprocessedEachFrame() {
    EnemyFixture enemy = addEnemy();

    press();
    updateTimes(3);

    verify(enemy.body, times(1)).setLinearVelocity(0f, 0f);
    verify(enemy.ai, times(1)).setEnabled(false);
  }

  @Test
  void enemyAppearingMidFreezeIsFrozenOnNextUpdate() {
    press();
    EnemyFixture late = addEnemy();

    component.update();

    late.verifyFrozen();
  }

  @Test
  void freezeEndsAfterDurationAndThawsEnemies() {
    EnemyFixture enemy = addEnemy();
    press();

    updateTimes(FREEZE_UPDATES - 1);
    assertEquals(0, endedCount.get());
    verify(enemy.ai, never()).setEnabled(true);

    component.update();

    assertEquals(1, endedCount.get());
    enemy.verifyThawed();
  }

  @Test
  void cooldownBlocksReactivationUntilItExpires() {
    addEnemy();
    press();
    updateTimes(FREEZE_UPDATES);
    assertEquals(1, activatedCount.get());

    updateTimes(COOLDOWN_UPDATES - 1);
    press();
    assertEquals(1, activatedCount.get(), "still on cooldown");

    component.update();
    press();
    assertEquals(2, activatedCount.get(), "cooldown finished");
  }

  @Test
  void deactivatingPerkMidFreezeEndsItEarly() {
    EnemyFixture enemy = addEnemy();
    press();

    deactivatePerk();

    enemy.verifyThawed();
    assertEquals(1, endedCount.get());
  }

  @Test
  void deactivatingPerkWhenNotFreezingDoesNothing() {
    deactivatePerk();

    assertEquals(0, endedCount.get());
  }

  @Test
  void endedFreezeStartsCooldownEvenWhenEndedByDeactivation() {
    addEnemy();
    press();
    deactivatePerk();

    press();

    assertEquals(1, activatedCount.get());
  }

  @Test
  void enemyDisposedDuringFreezeIsNotThawed() {
    EnemyFixture enemy = addEnemy();
    press();
    when(enemy.entity.isDisposed()).thenReturn(true);

    updateTimes(FREEZE_UPDATES);

    verify(enemy.ai, never()).setEnabled(true);
    assertEquals(1, endedCount.get());
  }

  @Test
  void disposeThawsFrozenEnemies() {
    EnemyFixture enemy = addEnemy();
    press();

    component.dispose();

    enemy.verifyThawed();
  }

  @Test
  void updateWithoutTimeSourceDoesNothing() {
    serviceLocator.when(ServiceLocator::getTimeSource).thenReturn(null);

    assertDoesNotThrow(component::update);
  }
}
