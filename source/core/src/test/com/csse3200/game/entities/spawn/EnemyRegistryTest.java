package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link EnemyRegistry}, the global record of which enemy spawns have been killed. The
 * registry is static, so it is cleared before and after every test.
 */
class EnemyRegistryTest {

  @BeforeEach
  void clearBefore() {
    EnemyRegistry.loadFrom(null);
  }

  @AfterEach
  void clearAfter() {
    EnemyRegistry.loadFrom(null);
  }

  @Test
  void shouldNotReportAnUnknownEnemyAsKilled() {
    assertFalse(EnemyRegistry.isKilled("forest:1,1"));
  }

  @Test
  void shouldReportAnEnemyAsKilledOnceMarked() {
    EnemyRegistry.markKilled("forest:1,1");

    assertTrue(EnemyRegistry.isKilled("forest:1,1"));
    assertFalse(EnemyRegistry.isKilled("forest:2,2"));
  }

  @Test
  void shouldKeepASingleEntryWhenMarkedTwice() {
    EnemyRegistry.markKilled("forest:1,1");
    EnemyRegistry.markKilled("forest:1,1");

    assertEquals(1, EnemyRegistry.exportAll().size());
  }

  @Test
  void shouldReplaceExistingEntriesWhenLoading() {
    EnemyRegistry.markKilled("old:0,0");

    EnemyRegistry.loadFrom(List.of("forest:1,1", "cave:2,2"));

    assertFalse(EnemyRegistry.isKilled("old:0,0"));
    assertTrue(EnemyRegistry.isKilled("forest:1,1"));
    assertTrue(EnemyRegistry.isKilled("cave:2,2"));
  }

  @Test
  void shouldClearEverythingWhenLoadingNull() {
    EnemyRegistry.markKilled("forest:1,1");

    EnemyRegistry.loadFrom(null);

    assertFalse(EnemyRegistry.isKilled("forest:1,1"));
    assertTrue(EnemyRegistry.exportAll().isEmpty());
  }

  @Test
  void shouldExportEveryKilledEnemy() {
    EnemyRegistry.markKilled("forest:1,1");
    EnemyRegistry.markKilled("cave:2,2");

    List<String> exported = EnemyRegistry.exportAll();

    assertEquals(2, exported.size());
    assertTrue(exported.contains("forest:1,1"));
    assertTrue(exported.contains("cave:2,2"));
  }

  @Test
  void shouldExportACopyThatDoesNotChangeTheRegistry() {
    EnemyRegistry.markKilled("forest:1,1");

    EnemyRegistry.exportAll().clear();

    assertTrue(EnemyRegistry.isKilled("forest:1,1"));
  }
}
