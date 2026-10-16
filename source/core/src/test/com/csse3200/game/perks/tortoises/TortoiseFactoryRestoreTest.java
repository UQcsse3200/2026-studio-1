package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@code TortoiseFactory.restoreFound}, the save/load helper from pull request #377.
 *
 * <p>Keep this file out of the commit until #377 has merged (or {@code restoreFound} is on the
 * branch), or the test sources will not compile.
 */
@ExtendWith(GameExtension.class)
class TortoiseFactoryRestoreTest {
  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoiseFactory.resetAll();
  }

  @AfterEach
  void tearDown() {
    TortoiseFactory.resetAll();
  }

  @Test
  void shouldMarkEverySavedTortoiseAsFound() {
    TortoiseFactory.restoreFound(List.of("level1_a", "rnd_hound_s_den_1"));

    assertTrue(TortoiseFactory.isFound("level1_a"));
    assertTrue(TortoiseFactory.isFound("rnd_hound_s_den_1"));
  }

  @Test
  void shouldReplaceWhatWasFoundBefore() {
    TortoiseRegistry.markFound("old_one");

    TortoiseFactory.restoreFound(List.of("level1_a"));

    assertFalse(TortoiseFactory.isFound("old_one"));
    assertTrue(TortoiseFactory.isFound("level1_a"));
  }

  @Test
  void shouldClearEverythingForANullList() {
    TortoiseRegistry.markFound("level1_a");

    TortoiseFactory.restoreFound(null);

    assertFalse(TortoiseFactory.isFound("level1_a"));
  }

  @Test
  void shouldIgnoreNullAndBlankIds() {
    TortoiseFactory.restoreFound(Arrays.asList(null, "", "   ", "level1_a"));

    assertTrue(TortoiseFactory.isFound("level1_a"));
    assertFalse(TortoiseFactory.isFound(""));
  }
}
