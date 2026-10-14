package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the shape shared by the four static holders of the win system: {@link BossRoster}, {@link
 * QuestLedger}, {@link TortoiseLedger} and {@link WinEvaluator}. Each is a utility class, like
 * {@code EnemyId} and {@code EnemyRegistry}: final, all static, and impossible to instantiate.
 *
 * <p>One parameterised case per class, so a fifth holder is covered by adding its name to the list.
 */
class WinUtilityClassesTest {

  @ParameterizedTest(name = "{0} is final")
  @ValueSource(
      classes = {BossRoster.class, QuestLedger.class, TortoiseLedger.class, WinEvaluator.class})
  void shouldBeFinalSoNothingCanExtendIt(Class<?> type) {
    assertTrue(Modifier.isFinal(type.getModifiers()));
  }

  @ParameterizedTest(name = "{0} has one private constructor")
  @ValueSource(
      classes = {BossRoster.class, QuestLedger.class, TortoiseLedger.class, WinEvaluator.class})
  void shouldHaveOnlyAPrivateNoArgumentConstructor(Class<?> type) throws Exception {
    assertEquals(1, type.getDeclaredConstructors().length);

    Constructor<?> constructor = type.getDeclaredConstructor();

    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  @ParameterizedTest(name = "{0} refuses to be instantiated")
  @ValueSource(
      classes = {BossRoster.class, QuestLedger.class, TortoiseLedger.class, WinEvaluator.class})
  void shouldRefuseToBeInstantiatedEvenThroughReflection(Class<?> type) throws Exception {
    Constructor<?> constructor = type.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
    assertEquals("Utility class", thrown.getCause().getMessage());
  }

  @ParameterizedTest(name = "{0} keeps no per-instance state")
  @ValueSource(
      classes = {BossRoster.class, QuestLedger.class, TortoiseLedger.class, WinEvaluator.class})
  void shouldKeepAllOfItsStateStatic(Class<?> type) {
    for (Field field : type.getDeclaredFields()) {
      if (field.isSynthetic()) {
        continue; // added by the coverage tool, not by the class
      }
      assertTrue(Modifier.isStatic(field.getModifiers()), field.getName() + " must be static");
    }
  }
}
