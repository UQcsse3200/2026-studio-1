package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetCombatComponentTest {
  private GameTime timeSource;
  private Entity owner;
  private Entity pet;
  private PetCombatComponent combat;
  private final List<Entity> attacks = new ArrayList<>();

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    timeSource = mock(GameTime.class);
    when(timeSource.getDeltaTime()).thenReturn(0.1f);
    owner = createCombatEntity(0f, 0f);
    combat = new PetCombatComponent(timeSource);
    pet = new Entity().addComponent(new PetComponent(owner)).addComponent(combat);
    ServiceLocator.getEntityService().register(pet);
    pet.getEvents().addListener("petAttack", (Entity target) -> attacks.add(target));
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }

  @Test
  void shouldDeferAttackUntilUpdateAndNeverRepeatWithoutAnotherHit() {
    Entity target = createCombatEntity(2f, 0f);

    pet.update();
    combat.requestAttack(target);
    assertTrue(attacks.isEmpty());

    pet.update();
    assertEquals(List.of(target), attacks);

    when(timeSource.getDeltaTime()).thenReturn(1f);
    pet.update();
    pet.update();
    assertEquals(List.of(target), attacks);
  }

  @Test
  void shouldDiscardHitsDuringCooldownAndRequireANewHitAfterwards() {
    Entity first = createCombatEntity(2f, 0f);
    Entity second = createCombatEntity(3f, 0f);
    combat.requestAttack(first);
    pet.update();

    combat.requestAttack(second);
    pet.update();
    when(timeSource.getDeltaTime()).thenReturn(1f);
    pet.update();
    assertEquals(List.of(first), attacks);

    combat.requestAttack(second);
    pet.update();
    assertEquals(List.of(first, second), attacks);
  }

  @Test
  void shouldRetargetToTheNewHitEvenWhenPreviousEnemyIsCloser() {
    Entity first = createCombatEntity(1f, 0f);
    Entity second = createCombatEntity(5f, 0f);
    combat.requestAttack(first);
    pet.update();

    when(timeSource.getDeltaTime()).thenReturn(1f);
    pet.update();
    combat.requestAttack(second);
    pet.update();

    assertEquals(List.of(first, second), attacks);
  }

  @Test
  void shouldChooseNearestHitEnemyRegardlessOfEventOrder() {
    Entity near = createCombatEntity(2f, 0f);
    Entity far = createCombatEntity(5f, 0f);
    createCombatEntity(1f, 0f); // A closer enemy without a confirmed player hit is not a candidate.

    combat.requestAttack(far);
    combat.requestAttack(near);
    combat.requestAttack(far);
    pet.update();

    when(timeSource.getDeltaTime()).thenReturn(1f);
    pet.update();
    combat.requestAttack(near);
    combat.requestAttack(far);
    pet.update();

    assertEquals(List.of(near, near), attacks);
  }

  @Test
  void shouldResolveEqualDistancesConsistently() {
    Entity first = createCombatEntity(-2f, 0f);
    Entity second = createCombatEntity(2f, 0f);

    combat.requestAttack(second);
    combat.requestAttack(first);
    combat.requestAttack(second);
    pet.update();

    assertEquals(List.of(first), attacks);
  }

  @Test
  void shouldIgnoreInvalidTargetsWithoutReplacingAValidRequest() {
    Entity target = createCombatEntity(3f, 0f);
    Entity dead = createCombatEntity(1f, 0f);
    dead.getComponent(CombatStatsComponent.class).setHealth(0);
    Entity disposed = createCombatEntity(1f, 0f);
    disposed.dispose();

    combat.requestAttack(target);
    combat.requestAttack(null);
    combat.requestAttack(owner);
    combat.requestAttack(pet);
    combat.requestAttack(new Entity());
    combat.requestAttack(dead);
    combat.requestAttack(disposed);
    pet.update();

    assertEquals(List.of(target), attacks);
  }

  @Test
  void shouldCancelAttackIfTargetDiesBeforeUpdate() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    target.getComponent(CombatStatsComponent.class).setHealth(0);

    pet.update();
    target.getComponent(CombatStatsComponent.class).setHealth(100);
    pet.update();

    assertTrue(attacks.isEmpty());
  }

  @Test
  void shouldCancelAttackIfTargetIsDisposedBeforeUpdate() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    target.dispose();

    pet.update();

    assertTrue(attacks.isEmpty());
  }

  @Test
  void shouldCancelPendingAttackAndRejectNewHitsWhileOwnerIsDead() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    owner.getComponent(CombatStatsComponent.class).setHealth(0);
    pet.update();

    combat.requestAttack(target);
    owner.getComponent(CombatStatsComponent.class).setHealth(100);
    pet.update();
    assertTrue(attacks.isEmpty());

    combat.requestAttack(target);
    pet.update();
    assertEquals(List.of(target), attacks);
  }

  @Test
  void shouldCancelAttackIfOwnerIsDisposedBeforeUpdate() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    owner.dispose();

    pet.update();
    combat.requestAttack(target);
    pet.update();

    assertTrue(attacks.isEmpty());
  }

  @Test
  void shouldDropPendingAndNewRequestsWhenDisabled() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    combat.setEnabled(false);
    combat.requestAttack(target);
    pet.update();
    combat.setEnabled(true);
    pet.update();

    assertTrue(attacks.isEmpty());
    combat.requestAttack(target);
    pet.update();
    assertEquals(List.of(target), attacks);
  }

  @Test
  void shouldNotAdvanceCooldownWhenGameTimeIsPaused() {
    Entity target = createCombatEntity(2f, 0f);
    combat.requestAttack(target);
    pet.update();

    when(timeSource.getDeltaTime()).thenReturn(0f);
    combat.requestAttack(target);
    pet.update();
    pet.update();

    assertEquals(List.of(target), attacks);
  }

  private Entity createCombatEntity(float x, float y) {
    Entity entity = new Entity().addComponent(new CombatStatsComponent(100, 10));
    entity.setPosition(x, y);
    ServiceLocator.getEntityService().register(entity);
    return entity;
  }
}
