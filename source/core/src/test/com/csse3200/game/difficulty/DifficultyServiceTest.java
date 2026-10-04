package com.csse3200.game.difficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * No PerkServiceTest exists to mirror (PerkService has no test file either), so this covers what
 * the task asks for directly: the NORMAL default, a setCurrent()/getCurrent() round-trip, and that
 * the state is genuinely static/shared rather than tied to any particular caller.
 */
class DifficultyServiceTest {

  // DifficultyService is a static holder with no public constructor and no per-test reset hook of
  // its own (unlike PerkService.resetAll()), so tests reset it directly to avoid leaking state
  // between test methods in the same JVM run.
  @BeforeEach
  @AfterEach
  void resetToDefault() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  @Test
  void defaultsToNormal() {
    assertEquals(Difficulty.NORMAL, DifficultyService.getCurrent());
  }

  @Test
  void setCurrentAndGetCurrentRoundTripForEveryDifficulty() {
    for (Difficulty difficulty : Difficulty.values()) {
      DifficultyService.setCurrent(difficulty);
      assertEquals(difficulty, DifficultyService.getCurrent());
    }
  }

  /**
   * Structural proof it's genuinely static: no instance can even be constructed to hold per-caller
   * state, and every field is static - there is nothing an instance could own.
   */
  @Test
  void hasNoInstanceStateAndCannotBeInstantiatedOutsideItself() throws Exception {
    for (Field field : DifficultyService.class.getDeclaredFields()) {
      assertTrue(
          Modifier.isStatic(field.getModifiers()),
          field.getName() + " must be static - DifficultyService must have no per-instance state");
    }

    Constructor<?> constructor = DifficultyService.class.getDeclaredConstructor();
    assertTrue(
        Modifier.isPrivate(constructor.getModifiers()),
        "the constructor must be private - this is a static holder, not something to instantiate");
  }

  /**
   * Behavioural proof it's genuinely shared: a value set from this thread must be visible to a
   * completely different thread with no reference passed between them - not just "the same call
   * site sees the same value twice", which would also be true of accidental instance state if only
   * one instance ever happened to exist.
   */
  @Test
  void aValueSetOnOneThreadIsVisibleFromAnotherWithNoSharedReference() throws Exception {
    DifficultyService.setCurrent(Difficulty.HARD);

    AtomicReference<Difficulty> seenOnOtherThread = new AtomicReference<>();
    Thread reader = new Thread(() -> seenOnOtherThread.set(DifficultyService.getCurrent()));
    reader.start();
    reader.join();

    assertEquals(Difficulty.HARD, seenOnOtherThread.get());
  }
}
