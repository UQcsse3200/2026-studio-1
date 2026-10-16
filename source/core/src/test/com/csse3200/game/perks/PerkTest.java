package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerkTest {
  private Perk perk;

  @BeforeEach
  void setUp() {
    perk = new Perk("dash", "Dash Master", "Dash further", "dashUsed", 10);
  }

  @Test
  void constructorSetsFieldsAndStartsLockedAtZero() {
    assertEquals("dash", perk.getId());
    assertEquals("Dash Master", perk.getName());
    assertEquals("Dash further", perk.getDescription());
    assertEquals("dashUsed", perk.getEventKey());
    assertEquals(10, perk.getThreshold());
    assertEquals(0, perk.getProgress());
    assertFalse(perk.isUnlocked());
    assertFalse(perk.isActive());
  }

  @Test
  void recordProgressAccumulatesWithoutUnlockingBelowThreshold() {
    assertFalse(perk.recordProgress(3));
    assertFalse(perk.recordProgress(4));
    assertEquals(7, perk.getProgress());
    assertFalse(perk.isUnlocked());
  }

  @Test
  void recordProgressUnlocksExactlyAtThresholdAndReturnsTrue() {
    assertTrue(perk.recordProgress(10));
    assertTrue(perk.isUnlocked());
    assertEquals(10, perk.getProgress());
  }

  @Test
  void recordProgressClampsOvershootToThreshold() {
    assertTrue(perk.recordProgress(500));
    assertEquals(10, perk.getProgress());
  }

  @Test
  void recordProgressAfterUnlockIsNoOp() {
    perk.recordProgress(10);
    assertFalse(perk.recordProgress(5));
    assertEquals(10, perk.getProgress());
    assertTrue(perk.isUnlocked());
  }

  @Test
  void onUnlockedRunsExactlyOnce() {
    AtomicInteger calls = new AtomicInteger();
    perk.setOnUnlocked(calls::incrementAndGet);

    perk.recordProgress(9);
    assertEquals(0, calls.get());
    perk.recordProgress(1);
    perk.recordProgress(1);
    assertEquals(1, calls.get());
  }

  @Test
  void unlockingWithoutCallbackDoesNotThrow() {
    assertTrue(perk.recordProgress(10));
  }

  @Test
  void setActiveTogglesActiveFlag() {
    perk.setActive(true);
    assertTrue(perk.isActive());
    perk.setActive(false);
    assertFalse(perk.isActive());
  }

  @Test
  void notifyActivatedRunsCallback() {
    AtomicInteger calls = new AtomicInteger();
    perk.setOnActivated(calls::incrementAndGet);
    perk.notifyActivated();
    assertEquals(1, calls.get());
  }

  @Test
  void notifyDeactivatedRunsCallback() {
    AtomicInteger calls = new AtomicInteger();
    perk.setOnDeactivated(calls::incrementAndGet);
    perk.notifyDeactivated();
    assertEquals(1, calls.get());
  }

  @Test
  void notifyWithoutCallbacksDoesNotThrow() {
    perk.notifyActivated();
    perk.notifyDeactivated();
  }

  @Test
  void restoreStateSetsAllValues() {
    perk.restoreState(4, false, false);
    assertEquals(4, perk.getProgress());
    assertFalse(perk.isUnlocked());

    perk.restoreState(10, true, true);
    assertTrue(perk.isUnlocked());
    assertTrue(perk.isActive());
  }

  @Test
  void restoreStateClampsProgressToThreshold() {
    perk.restoreState(999, true, false);
    assertEquals(10, perk.getProgress());
  }

  @Test
  void restoreStateCannotActivateALockedPerk() {
    perk.restoreState(3, false, true);
    assertFalse(perk.isActive());
  }

  @Test
  void restoreStateDoesNotFireCallbacks() {
    AtomicInteger calls = new AtomicInteger();
    perk.setOnUnlocked(calls::incrementAndGet);
    perk.setOnActivated(calls::incrementAndGet);
    perk.restoreState(10, true, true);
    assertEquals(0, calls.get());
  }

  @Test
  void progressTextShowsFractionWhileLocked() {
    perk.recordProgress(3);
    assertEquals("3/10", perk.getProgressText());
  }

  @Test
  void progressTextShowsUnlockedOnceUnlocked() {
    perk.recordProgress(10);
    assertEquals("Unlocked", perk.getProgressText());
  }
}
