package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link TortoiseRegistry}: found tortoises are remembered by id, one id does not affect
 * another, and a reset forgets them all.
 *
 * <p>The registry keeps its state in libGDX Preferences, which the headless application backs with
 * a file, so every test clears it before and after.
 */
@ExtendWith(GameExtension.class)
class TortoiseRegistryTest {
  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoiseRegistry.resetAll();
  }

  @AfterEach
  void tearDown() {
    TortoiseRegistry.resetAll();
  }

  @Test
  void shouldStartWithNothingFound() {
    assertFalse(TortoiseRegistry.isFound("level1_a"));
  }

  @Test
  void shouldRememberATortoiseOnceMarkedFound() {
    TortoiseRegistry.markFound("level1_a");

    assertTrue(TortoiseRegistry.isFound("level1_a"));
  }

  @Test
  void shouldNotAffectOtherTortoises() {
    TortoiseRegistry.markFound("level1_a");

    assertFalse(TortoiseRegistry.isFound("level1_b"));
    assertFalse(TortoiseRegistry.isFound("level1_a.found"));
  }

  @Test
  void shouldAllowMarkingTheSameTortoiseTwice() {
    TortoiseRegistry.markFound("level1_a");
    TortoiseRegistry.markFound("level1_a");

    assertTrue(TortoiseRegistry.isFound("level1_a"));
  }

  @Test
  void shouldForgetEveryTortoiseOnReset() {
    TortoiseRegistry.markFound("level1_a");
    TortoiseRegistry.markFound("rnd_hound_s_den_1");

    TortoiseRegistry.resetAll();

    assertFalse(TortoiseRegistry.isFound("level1_a"));
    assertFalse(TortoiseRegistry.isFound("rnd_hound_s_den_1"));
  }

  @Test
  void shouldTrackTortoisesWithTheSameStartIndependently() {
    TortoiseRegistry.markFound("rnd_hound_s_den_1");

    assertFalse(TortoiseRegistry.isFound("rnd_hound_s_den_2"));
    assertFalse(TortoiseRegistry.isFound("rnd_hound_s_den_10"));
  }
}
