package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TortoisePlan}: the stored plan is replaced as a whole, a bad plan is rejected
 * without changing the old one, and what is exported can be set back, as a save and load do.
 *
 * <p>The plan is static state, so every test starts and finishes with it cleared.
 */
class TortoisePlanTest {
  private static final String DEN = "Hound's Den";
  private static final String FORGE = "Cyclops Forge";
  private static final String LEVEL_1 = "Level 1 - Out of the Underworld";

  @BeforeEach
  void setUp() {
    TortoisePlan.reset();
  }

  @AfterEach
  void tearDown() {
    TortoisePlan.reset();
  }

  @Test
  void shouldStartEmpty() {
    assertEquals(0, TortoisePlan.countFor(DEN));
    assertTrue(TortoisePlan.exportAll().isEmpty());
  }

  @Test
  void shouldReturnWhatWasSet() {
    TortoisePlan.set(Map.of(DEN, 1, LEVEL_1, 3));

    assertEquals(1, TortoisePlan.countFor(DEN));
    assertEquals(3, TortoisePlan.countFor(LEVEL_1));
  }

  @Test
  void shouldReturnZeroForAMapNotInThePlan() {
    TortoisePlan.set(Map.of(DEN, 1));

    assertEquals(0, TortoisePlan.countFor(FORGE));
  }

  @Test
  void shouldReplaceTheOldPlanWhenSetAgain() {
    TortoisePlan.set(Map.of(DEN, 1));
    TortoisePlan.set(Map.of(FORGE, 2));

    assertEquals(0, TortoisePlan.countFor(DEN));
    assertEquals(2, TortoisePlan.countFor(FORGE));
  }

  @Test
  void shouldTreatANullPlanAsEmpty() {
    TortoisePlan.set(Map.of(DEN, 1));

    TortoisePlan.set(null);

    assertTrue(TortoisePlan.exportAll().isEmpty());
  }

  @Test
  void shouldAcceptAZeroCount() {
    TortoisePlan.set(Map.of(DEN, 0));

    assertEquals(0, TortoisePlan.countFor(DEN));
  }

  @Test
  void shouldRejectANegativeCount() {
    Map<String, Integer> plan = Map.of(DEN, -1);

    assertThrows(IllegalArgumentException.class, () -> TortoisePlan.set(plan));
  }

  @Test
  void shouldRejectANullCount() {
    Map<String, Integer> plan = new HashMap<>();
    plan.put(DEN, null);

    assertThrows(IllegalArgumentException.class, () -> TortoisePlan.set(plan));
  }

  @Test
  void shouldKeepTheOldPlanWhenSetRejectsABadPlan() {
    TortoisePlan.set(Map.of(DEN, 1));
    Map<String, Integer> bad = Map.of(FORGE, -1);

    assertThrows(IllegalArgumentException.class, () -> TortoisePlan.set(bad));

    assertEquals(1, TortoisePlan.countFor(DEN));
    assertEquals(0, TortoisePlan.countFor(FORGE));
  }

  @Test
  void shouldExportACopy() {
    TortoisePlan.set(Map.of(DEN, 1));

    Map<String, Integer> copy = TortoisePlan.exportAll();
    copy.put(FORGE, 5);

    assertEquals(0, TortoisePlan.countFor(FORGE));
  }

  @Test
  void shouldNotShareTheCallersMap() {
    Map<String, Integer> plan = new HashMap<>();
    plan.put(DEN, 1);
    TortoisePlan.set(plan);

    plan.put(FORGE, 4);

    assertEquals(0, TortoisePlan.countFor(FORGE));
    assertNotSame(plan, TortoisePlan.exportAll());
  }

  @Test
  void shouldRoundTripThroughExportAndSet() {
    Map<String, Integer> plan = Map.of(DEN, 1, LEVEL_1, 3);
    TortoisePlan.set(plan);

    Map<String, Integer> saved = TortoisePlan.exportAll();
    TortoisePlan.reset();
    TortoisePlan.set(saved);

    assertEquals(plan, TortoisePlan.exportAll());
  }

  @Test
  void shouldClearOnReset() {
    TortoisePlan.set(Map.of(DEN, 1));

    TortoisePlan.reset();

    assertTrue(TortoisePlan.exportAll().isEmpty());
  }

  @Test
  void shouldNotBeInstantiable() throws ReflectiveOperationException {
    Constructor<TortoisePlan> constructor = TortoisePlan.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
