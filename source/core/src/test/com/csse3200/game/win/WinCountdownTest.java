package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link WinCountdown}: the wait between the final boss falling and the win screen. It is a
 * plain timer, so no game fixture is needed. Frames here are 0.5 s unless a test says otherwise,
 * and the delay is 2 s, so the numbers can be followed by hand.
 */
class WinCountdownTest {
  private static final float DELAY = 2f;
  private static final float FRAME = 0.5f;

  // ---------- construction ----------

  @Test
  void shouldStoreTheDelayAndStartIdle() {
    WinCountdown countdown = new WinCountdown(DELAY);

    assertEquals(DELAY, countdown.getDelaySeconds(), 1e-6f);
    assertFalse(countdown.isPending());
    assertFalse(countdown.isFinished());
  }

  @ParameterizedTest(name = "delay {0} is rejected")
  @ValueSource(floats = {-0.001f, -1f, -100f})
  void shouldRejectANegativeDelay(float delay) {
    assertThrows(IllegalArgumentException.class, () -> new WinCountdown(delay));
  }

  @Test
  void shouldRejectANotANumberOrInfiniteDelay() {
    assertThrows(IllegalArgumentException.class, () -> new WinCountdown(Float.NaN));
    assertThrows(IllegalArgumentException.class, () -> new WinCountdown(Float.POSITIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> new WinCountdown(Float.NEGATIVE_INFINITY));
  }

  // ---------- before the boss falls ----------

  @Test
  void shouldDoNothingWhileTheFinalBossIsAlive() {
    WinCountdown countdown = new WinCountdown(DELAY);

    for (int i = 0; i < 100; i++) {
      assertFalse(countdown.update(false, FRAME));
    }

    assertFalse(countdown.isPending(), "time only counts once the boss has fallen");
    assertFalse(countdown.isFinished());
  }

  // ---------- the wait ----------

  @Test
  void shouldStartWaitingOnTheFrameTheBossFallsWithoutShowingTheWinYet() {
    WinCountdown countdown = new WinCountdown(DELAY);

    assertFalse(countdown.update(true, FRAME));

    assertTrue(countdown.isPending());
    assertFalse(countdown.isFinished());
  }

  @Test
  void shouldNotCountTheFrameThatStartedTheWait() {
    // A huge first frame must not swallow the whole delay: the wait starts from that frame.
    WinCountdown countdown = new WinCountdown(DELAY);

    assertFalse(countdown.update(true, 1_000f));

    assertTrue(countdown.isPending());
  }

  @Test
  void shouldAskForTheWinScreenOnceTheDelayHasPassed() {
    WinCountdown countdown = new WinCountdown(DELAY);
    countdown.update(true, FRAME); // starts the wait

    assertFalse(countdown.update(true, FRAME)); // 0.5 s
    assertFalse(countdown.update(true, FRAME)); // 1.0 s
    assertFalse(countdown.update(true, FRAME)); // 1.5 s
    assertTrue(countdown.update(true, FRAME), "2.0 s have passed");

    assertTrue(countdown.isFinished());
    assertFalse(countdown.isPending());
  }

  @ParameterizedTest(name = "delay {0} s in {1} s frames takes {2} frames after the start")
  @CsvSource({"2, 0.5, 4", "2, 1, 2", "2, 2, 1", "2, 5, 1", "1, 0.25, 4", "3, 1, 3"})
  void shouldTakeAsManyFramesAsTheDelayNeeds(float delay, float frame, int framesNeeded) {
    WinCountdown countdown = new WinCountdown(delay);
    countdown.update(true, frame); // starts the wait

    int frames = 0;
    boolean shown = false;
    while (!shown && frames < 1_000) {
      shown = countdown.update(true, frame);
      frames++;
    }

    assertEquals(framesNeeded, frames);
  }

  @Test
  void shouldAskForTheWinScreenOnlyOnce() {
    WinCountdown countdown = new WinCountdown(DELAY);
    int asked = 0;

    for (int i = 0; i < 200; i++) {
      if (countdown.update(true, FRAME)) {
        asked++;
      }
    }

    assertEquals(1, asked, "the win screen must not be re-shown every frame");
  }

  @Test
  void shouldShowTheWinStraightAwayWhenTheDelayIsZero() {
    WinCountdown countdown = new WinCountdown(0f);

    assertTrue(countdown.update(true, FRAME));

    assertTrue(countdown.isFinished());
    assertFalse(countdown.update(true, FRAME), "and still only once");
  }

  // ---------- awkward frames ----------

  @Test
  void shouldTreatANegativeFrameTimeAsNoTimeAtAll() {
    WinCountdown countdown = new WinCountdown(DELAY);
    countdown.update(true, FRAME);

    assertFalse(countdown.update(true, -50f));
    assertFalse(countdown.update(true, -50f));

    assertTrue(countdown.isPending(), "a negative frame must neither finish nor lengthen the wait");
    assertFalse(countdown.update(true, 1.5f));
    assertTrue(countdown.update(true, FRAME), "exactly the normal 2 s were still needed");
  }

  @Test
  void shouldNotCountFramesOfZeroLength() {
    WinCountdown countdown = new WinCountdown(DELAY);
    countdown.update(true, FRAME);

    for (int i = 0; i < 50; i++) {
      assertFalse(countdown.update(true, 0f));
    }

    assertTrue(countdown.isPending(), "a paused game makes no progress towards the win screen");
  }

  @Test
  void shouldKeepWaitingIfTheBossIsNoLongerReportedDefeated() {
    // Leaving the boss room swaps the area, so the flag can drop. The wait simply pauses.
    WinCountdown countdown = new WinCountdown(DELAY);
    countdown.update(true, FRAME);
    countdown.update(true, 1f);

    assertFalse(countdown.update(false, 10f));

    assertTrue(countdown.isPending());
    assertTrue(countdown.update(true, 1f), "it carries on from where it was");
  }

  @Test
  void shouldStayFinishedForGood() {
    WinCountdown countdown = new WinCountdown(0f);
    countdown.update(true, FRAME);

    assertFalse(countdown.update(false, FRAME));
    assertFalse(countdown.update(true, FRAME));

    assertTrue(countdown.isFinished());
  }

  @Test
  void shouldKeepTwoCountdownsIndependent() {
    WinCountdown first = new WinCountdown(DELAY);
    WinCountdown second = new WinCountdown(DELAY);

    first.update(true, FRAME);

    assertTrue(first.isPending());
    assertFalse(second.isPending());
  }
}
