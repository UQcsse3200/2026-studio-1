package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class OnHitEffectComponentTest {

  private static final String MELEE_EVENT_NAME = "meleeAttackHit";
  private static final String RANGED_EVENT_NAME = "rangedAttackHit";
  private static final List<String> MELEE_ONLY = List.of(MELEE_EVENT_NAME);
  private static final List<String> RANGED_ONLY = List.of(RANGED_EVENT_NAME);
  private static final float TRIGGER_CHANCE = 0.3f;
  private static final int STARTING_HEALTH = 100;

  /* testing the constructor class */
  @Test
  void shouldThrowWhenConstructedWithZeroTriggerChance() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(MELEE_ONLY, 0f, 5, -10),
        "Expected constructor to throw IllegalArgumentException for triggerChance = 0, but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithNegativeTriggerChance() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(MELEE_ONLY, -0.1f, 5, -10),
        "Expected constructor to throw IllegalArgumentException for triggerChance = "
            + -0.1f
            + ", but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithTriggerChanceAboveOne() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(MELEE_ONLY, 1.1f, 5, -10),
        "Expected constructor to throw IllegalArgumentException for triggerChance = "
            + 1.1f
            + ", but it did not.");
  }

  @Test
  void shouldAcceptTriggerChanceOfOne() {
    assertDoesNotThrow(
        () -> new OnHitEffectComponent(MELEE_ONLY, 1.0f, 5, -10),
        "Expected triggerChance = 1.0 to be accepted as a valid boundary value.");
  }

  @Test
  void shouldThrowWhenConstructedWithNegativeEffectDuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, -1, -10),
        "Expected constructor to throw IllegalArgumentException for effectDuration = "
            + -1
            + ", but it did not.");
  }

  @Test
  void shouldAcceptZeroEffectDuration() {
    assertDoesNotThrow(
        () -> new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10),
        "Expected effectDuration = 0 to be accepted (instant effect), but it threw.");
  }

  @Test
  void shouldThrowWhenConstructedWithNullEventNameList() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(null, TRIGGER_CHANCE, 5, -10),
        "Expected constructor to throw IllegalArgumentException for a null hitEventNames list, but it did not.");
  }

  @Test
  void shouldThrowWhenConstructedWithEmptyEventNameList() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OnHitEffectComponent(List.of(), TRIGGER_CHANCE, 5, -10),
        "Expected constructor to throw IllegalArgumentException for an empty hitEventNames list, but it did not.");
  }

  /* the following tests instant effects (effectDuration = 0), applied directly via
   * CombatStatsComponent.addHealth() on a successful roll, with no ticking involved
   */
  @Test
  void shouldReduceTargetHealthImmediatelyOnForcedSuccessRoll() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // < TRIGGER_CHANCE -> success

    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10, random);
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    component.onAttackHit(target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBefore - 10,
        healthAfter,
        "Expected an instant effect to reduce target's health by exactly 10 immediately, but health went from "
            + healthBefore
            + " to "
            + healthAfter);
  }

  @Test
  void shouldNotChangeTargetHealthOnForcedFailureRoll() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.9f); // >= TRIGGER_CHANCE -> failure

    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10, random);
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    component.onAttackHit(target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBefore,
        healthAfter,
        "Expected no health change after a forced-failure roll, but health went from "
            + healthBefore
            + " to "
            + healthAfter);
  }

  @Test
  void shouldAlwaysApplyWhenTriggerChanceIsOne() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.999f); // highest realistic roll, still < 1.0

    OnHitEffectComponent component = new OnHitEffectComponent(MELEE_ONLY, 1.0f, 0, -10, random);
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    component.onAttackHit(target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBefore - 10,
        healthAfter,
        "Expected triggerChance = 1.0 to always apply the effect, but health went from "
            + healthBefore
            + " to "
            + healthAfter);
  }

  @Test
  void shouldNotThrowWhenTargetIsNull() {
    Random random = mock(Random.class);
    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10, random);

    assertDoesNotThrow(() -> component.onAttackHit(null));
  }

  @Test
  void shouldNotThrowWhenTargetHasNoCombatStatsComponent() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success
    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10, random);
    Entity targetWithoutStats = new Entity();
    targetWithoutStats.create();

    assertDoesNotThrow(() -> component.onAttackHit(targetWithoutStats));
  }

  /* the following tests over-time effects (effectDuration > 0), applied gradually via update() */
  @Test
  void shouldNotApplyAnyHealthChangeImmediatelyWhenDurationGreaterThanZero() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 5, -10, random);
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    component.onAttackHit(target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBefore,
        healthAfter,
        "Expected no health change until update() ticks the effect, but health changed immediately from "
            + healthBefore
            + " to "
            + healthAfter);
  }

  @Test
  void shouldApplyEffectGraduallyAcrossUpdateCalls() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 5, -10, random); // -2 per tick
    Entity target = createTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    component.onAttackHit(target);

    component.update();
    assertEquals(
        healthBefore - 2,
        targetStats.getHealth(),
        "Expected health to drop by 2 after the first update() tick, but got "
            + targetStats.getHealth());

    component.update();
    assertEquals(
        healthBefore - 4,
        targetStats.getHealth(),
        "Expected health to drop by 4 total after the second update() tick, but got "
            + targetStats.getHealth());
  }

  @Test
  void shouldApplyExactTotalHealthChangeByFinalTick() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    OnHitEffectComponent component =
        new OnHitEffectComponent(
            MELEE_ONLY, TRIGGER_CHANCE, 5, -10, random); // -2 per tick, evenly divisible
    Entity target = createTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    component.onAttackHit(target);
    for (int i = 0; i < 5; i++) {
      component.update();
    }

    assertEquals(
        healthBefore - 10,
        targetStats.getHealth(),
        "Expected exactly -10 total health change after all 5 ticks, but got a change of "
            + (targetStats.getHealth() - healthBefore));
  }

  @Test
  void shouldApplyExactTotalHealthChangeWhenNotEvenlyDivisible() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    // -10 over 3 ticks: -3, -3, then -4 on the final tick to make the total exact
    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 3, -10, random);
    Entity target = createTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    int healthBefore = targetStats.getHealth();

    component.onAttackHit(target);
    for (int i = 0; i < 3; i++) {
      component.update();
    }

    assertEquals(
        healthBefore - 10,
        targetStats.getHealth(),
        "Expected exactly -10 total health change even when not evenly divisible across ticks, but got a change of "
            + (targetStats.getHealth() - healthBefore));
  }

  @Test
  void shouldStopApplyingEffectAfterDurationElapses() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    OnHitEffectComponent component =
        new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 5, -10, random);
    Entity target = createTarget();
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);

    component.onAttackHit(target);
    for (int i = 0; i < 5; i++) {
      component.update();
    }
    int healthAfterEffectEnds = targetStats.getHealth();

    // effect should be fully consumed by now; further updates should not change health further
    component.update();
    component.update();

    assertEquals(
        healthAfterEffectEnds,
        targetStats.getHealth(),
        "Expected no further health change once the effect's duration has elapsed, but health changed from "
            + healthAfterEffectEnds
            + " to "
            + targetStats.getHealth());
  }

  /* the following tests the create() function logic, i.e. that this component wires itself to
   * every configured hit event name on a real entity, not just one
   */
  @Test
  void shouldRespondWhenConfiguredEventFires() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success

    Entity attacker =
        new Entity()
            .addComponent(new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE, 0, -10, random));
    attacker.create();
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    attacker.getEvents().trigger(MELEE_EVENT_NAME, target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();
    assertEquals(
        healthBefore - 10,
        healthAfter,
        "Expected the configured hit event to trigger onAttackHit() and apply the effect.");
  }

  @Test
  void shouldRespondToEitherConfiguredEventWhenMultipleAreGiven() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // force success on both calls

    Entity meleeAttacker =
        new Entity()
            .addComponent(
                new OnHitEffectComponent(
                    List.of(MELEE_EVENT_NAME, RANGED_EVENT_NAME), TRIGGER_CHANCE, 0, -10, random));
    meleeAttacker.create();
    Entity meleeTarget = createTarget();
    int meleeTargetHealthBefore = meleeTarget.getComponent(CombatStatsComponent.class).getHealth();

    meleeAttacker.getEvents().trigger(MELEE_EVENT_NAME, meleeTarget);

    assertEquals(
        meleeTargetHealthBefore - 10,
        meleeTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected a component configured for both melee and ranged events to respond to the melee event.");

    Entity rangedAttacker =
        new Entity()
            .addComponent(
                new OnHitEffectComponent(
                    List.of(MELEE_EVENT_NAME, RANGED_EVENT_NAME), TRIGGER_CHANCE, 0, -10, random));
    rangedAttacker.create();
    Entity rangedTarget = createTarget();
    int rangedTargetHealthBefore =
        rangedTarget.getComponent(CombatStatsComponent.class).getHealth();

    rangedAttacker.getEvents().trigger(RANGED_EVENT_NAME, rangedTarget);

    assertEquals(
        rangedTargetHealthBefore - 10,
        rangedTarget.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected a component configured for both melee and ranged events to also respond to the ranged event.");
  }

  @Test
  void shouldNotRespondToAnUnconfiguredEventName() {
    Random random = mock(Random.class);
    when(random.nextFloat()).thenReturn(0.1f); // would force success if it were heard

    Entity attacker =
        new Entity()
            .addComponent(
                new OnHitEffectComponent(
                    List.of(RANGED_EVENT_NAME), TRIGGER_CHANCE, 0, -10, random));
    attacker.create();
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    // trigger an event name this component was NOT configured for
    attacker.getEvents().trigger(MELEE_EVENT_NAME, target);

    assertEquals(
        healthBefore,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected the component to ignore an event name it was not configured for, but health changed.");
  }

  /* the following tests multiple independent instances on the same entity */
  @Test
  void multipleInstancesShouldRollIndependently() {
    Random randomA = mock(Random.class);
    Random randomB = mock(Random.class);
    when(randomA.nextFloat()).thenReturn(0.1f); // success
    when(randomB.nextFloat()).thenReturn(0.9f); // failure

    OnHitEffectComponent instanceA = new OnHitEffectComponent(MELEE_ONLY, 0.3f, 0, -10, randomA);
    OnHitEffectComponent instanceB = new OnHitEffectComponent(MELEE_ONLY, 0.1f, 0, -5, randomB);
    Entity attacker = new Entity().addComponent(instanceA).addComponent(instanceB);
    attacker.create();
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    attacker.getEvents().trigger(MELEE_EVENT_NAME, target);

    // only instanceA's -10 should apply; instanceB's -5 should not, since its roll failed
    assertEquals(
        healthBefore - 10,
        target.getComponent(CombatStatsComponent.class).getHealth(),
        "Expected only instanceA's forced-success effect (-10) to apply, not instanceB's failed one (-5).");
  }

  /* the following tests when the ranged attack is the only component added */
  @Test
  void shouldRespondWhenConfiguredEventFiresRangedOnly() {
    Random random = mock(Random.class);
    // forces success
    when(random.nextFloat()).thenReturn(0.1f);
    Entity attacker = new Entity()
        .addComponent(new OnHitEffectComponent(RANGED_ONLY, TRIGGER_CHANCE,
            0, -10, random)
    );
    attacker.create();
    Entity target = createTarget();
    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    attacker.getEvents().trigger(RANGED_EVENT_NAME, target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();

    assertEquals(healthBefore - 10, healthAfter,
        "Expected a ranged only configured component " +
            "to respond to its own configured ranged event."
    );
  }

  @Test
      void ShouldNotRespondToRangedEventWhenOnlyMeleeConfigured() {
    Random random = mock(Random.class);
    // force success
    when(random.nextFloat()).thenReturn(0.1f);
    // create attacker and target entities
    Entity attacker = new Entity()
        .addComponent(new OnHitEffectComponent(MELEE_ONLY, TRIGGER_CHANCE,
            0, -10, random)
        );
    attacker.create();
    Entity target = createTarget();

    int healthBefore = target.getComponent(CombatStatsComponent.class).getHealth();

    attacker.getEvents().trigger(RANGED_EVENT_NAME, target);

    int healthAfter = target.getComponent(CombatStatsComponent.class).getHealth();

    assertEquals(healthBefore, healthAfter,
        "Expected a melee only configured component to ignore a ranged event, " +
            "but health changed from: " + healthBefore + healthAfter
    );
  }

  /* ---------- Helpers ---------- */

  /**
   * Builds a fully created Entity representing a target, with a {@link CombatStatsComponent} at a
   * known starting health, ready for use in a test.
   *
   * @return a target entity with CombatStatsComponent(STARTING_HEALTH, 0) attached.
   */
  Entity createTarget() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(STARTING_HEALTH, 0));
    target.create();
    return target;
  }
}
