package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Covers {@link UpgradeNode#restore(int, float, int)}, used when loading a saved game. */
class UpgradeNodeRestoreTest {
  private UpgradeNode timeNode;
  private UpgradeNode killNode;

  @BeforeEach
  void beforeEach() {
    timeNode =
        UpgradeNode.timeBased(
            "test_time_upgrade",
            "Test Time Upgrade",
            "For testing restore.",
            new int[] {10, 10, 10},
            new float[] {10f, 10f, 10f});
    killNode =
        UpgradeNode.killCountBased(
            "test_kill_upgrade",
            "Test Kill Upgrade",
            "For testing restore.",
            new int[] {10, 10, 10},
            new int[] {3, 5, 8});
  }

  @Test
  void restoringATimeUpgradeSetsItsTierAndRemainingTime() {
    assertTrue(timeNode.restore(2, 14f, 0));

    assertTrue(timeNode.isActive());
    assertEquals(2, timeNode.getCurrentTier());
    assertEquals(14f, timeNode.getRemainingSeconds());
    assertEquals(0, timeNode.getRemainingKills());
  }

  @Test
  void restoringAKillUpgradeSetsItsTierAndRemainingKills() {
    assertTrue(killNode.restore(3, 0f, 4));

    assertEquals(3, killNode.getCurrentTier());
    assertEquals(4, killNode.getRemainingKills());
    assertEquals(0f, killNode.getRemainingSeconds());
  }

  @Test
  void restoringFiresTheTierChangedCallbackOnceSoTheEffectIsReapplied() {
    AtomicInteger applied = new AtomicInteger();
    timeNode.setOnTierChanged(applied::incrementAndGet);

    timeNode.restore(1, 5f, 0);

    assertEquals(1, applied.get());
  }

  @Test
  void restoringWithATierOutsideTheValidRangeIsRejected() {
    assertFalse(timeNode.restore(0, 5f, 0));
    assertFalse(timeNode.restore(4, 5f, 0));

    assertFalse(timeNode.isActive());
  }

  @Test
  void restoringATimeUpgradeWithNoTimeLeftIsRejected() {
    assertFalse(timeNode.restore(1, 0f, 0));

    assertFalse(timeNode.isActive());
  }

  @Test
  void restoringAKillUpgradeWithNoKillsLeftIsRejected() {
    assertFalse(killNode.restore(1, 0f, 0));

    assertFalse(killNode.isActive());
  }

  @Test
  void aRejectedRestoreDoesNotFireTheCallback() {
    AtomicInteger applied = new AtomicInteger();
    timeNode.setOnTierChanged(applied::incrementAndGet);

    timeNode.restore(0, 5f, 0);

    assertEquals(0, applied.get());
  }

  @Test
  void aRestoredTimeUpgradeStillExpiresNormally() {
    AtomicInteger expired = new AtomicInteger();
    timeNode.setOnExpired(expired::incrementAndGet);
    timeNode.restore(2, 3f, 0);

    timeNode.tickTime(3f);

    assertFalse(timeNode.isActive());
    assertEquals(1, expired.get());
  }

  @Test
  void aRestoredKillUpgradeStillExpiresNormally() {
    killNode.restore(1, 0f, 2);

    killNode.onEnemyKilled();
    assertTrue(killNode.isActive());

    killNode.onEnemyKilled();
    assertFalse(killNode.isActive());
  }

  // --- isRestoring() - lets the onTierChanged callback (e.g. UpgradesDisplay's apply*Effect()
  // methods) tell a silent restore apart from a fresh purchase, so it can skip anything
  // purchase-only (the activation flash) while still applying the gameplay effect as usual. ---

  @Test
  void isRestoringIsFalseBeforeAnyRestoreOrPurchase() {
    assertFalse(timeNode.isRestoring());
  }

  @Test
  void isRestoringIsTrueOnlyWhileRestoresOnTierChangedCallbackIsRunning() {
    AtomicBoolean restoringDuringCallback = new AtomicBoolean();
    timeNode.setOnTierChanged(() -> restoringDuringCallback.set(timeNode.isRestoring()));

    timeNode.restore(1, 5f, 0);

    assertTrue(restoringDuringCallback.get()); // true while the callback itself was running
    assertFalse(timeNode.isRestoring()); // false again immediately after restore() returns
  }

  @Test
  void isRestoringStaysFalseThroughoutANormalPurchase() {
    AtomicBoolean restoringDuringCallback = new AtomicBoolean(true); // start true - must flip
    timeNode.setOnTierChanged(() -> restoringDuringCallback.set(timeNode.isRestoring()));

    timeNode.purchaseNextTier();

    assertFalse(restoringDuringCallback.get());
    assertFalse(timeNode.isRestoring());
  }

  @Test
  void isRestoringIsClearedEvenIfTheCallbackThrows() {
    timeNode.setOnTierChanged(
        () -> {
          throw new RuntimeException("effect callback blew up");
        });

    try {
      timeNode.restore(1, 5f, 0);
    } catch (RuntimeException expected) {
      // ignored - only the flag's post-throw state matters here
    }

    assertFalse(timeNode.isRestoring()); // the try/finally must still have cleared it
  }
}
