package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LootRegistry}, which remembers collected loot spawn points so they are saved and
 * do not respawn. The registry is static, so it is cleared before and after every test.
 */
class LootRegistryTest {
  private static final String CHEST = "Level 1 - Out of the Underworld:12,7";
  private static final String POTION = "Level 2 - Climb Mount Olympus:3,40";

  @BeforeEach
  void setUp() {
    LootRegistry.loadFrom(new ArrayList<>());
  }

  @AfterEach
  void tearDown() {
    LootRegistry.loadFrom(new ArrayList<>());
  }

  @Test
  void nothingIsCollectedAtFirst() {
    assertFalse(LootRegistry.isCollected(CHEST));
    assertTrue(LootRegistry.exportAll().isEmpty());
  }

  @Test
  void markCollectedIsRemembered() {
    LootRegistry.markCollected(CHEST);

    assertTrue(LootRegistry.isCollected(CHEST));
    assertFalse(LootRegistry.isCollected(POTION));
  }

  @Test
  void markingTheSameLootTwiceIsStoredOnce() {
    LootRegistry.markCollected(CHEST);
    LootRegistry.markCollected(CHEST);

    assertEquals(1, LootRegistry.exportAll().size());
  }

  @Test
  void exportAllReturnsEveryCollectedId() {
    LootRegistry.markCollected(CHEST);
    LootRegistry.markCollected(POTION);

    List<String> exported = LootRegistry.exportAll();

    assertEquals(2, exported.size());
    assertTrue(exported.contains(CHEST));
    assertTrue(exported.contains(POTION));
  }

  @Test
  void changingTheExportedListDoesNotChangeTheRegistry() {
    LootRegistry.markCollected(CHEST);

    List<String> exported = LootRegistry.exportAll();
    exported.clear();

    assertTrue(LootRegistry.isCollected(CHEST));
  }

  @Test
  void loadFromReplacesTheCurrentIds() {
    LootRegistry.markCollected(CHEST);

    LootRegistry.loadFrom(List.of(POTION));

    assertFalse(LootRegistry.isCollected(CHEST));
    assertTrue(LootRegistry.isCollected(POTION));
  }

  @Test
  void loadFromNullClearsTheRegistry() {
    LootRegistry.markCollected(CHEST);

    LootRegistry.loadFrom(null);

    assertFalse(LootRegistry.isCollected(CHEST));
    assertTrue(LootRegistry.exportAll().isEmpty());
  }

  @Test
  void exportThenLoadKeepsTheSameIds() {
    LootRegistry.markCollected(CHEST);
    LootRegistry.markCollected(POTION);
    List<String> saved = LootRegistry.exportAll();

    LootRegistry.loadFrom(new ArrayList<>());
    LootRegistry.loadFrom(saved);

    assertTrue(LootRegistry.isCollected(CHEST));
    assertTrue(LootRegistry.isCollected(POTION));
  }

  @Test
  void cannotBeInstantiated() throws NoSuchMethodException {
    Constructor<LootRegistry> constructor = LootRegistry.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
