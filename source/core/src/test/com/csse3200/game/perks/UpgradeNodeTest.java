package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the TIME-based expiry math in purchaseNextTier(): each purchase ADDS a fixed increment
 * to whatever time is currently remaining, rather than overwriting it with a flat per-tier total
 * - and confirms the tier-count cap is unaffected by that change.
 */
class UpgradeNodeTest {
  private UpgradeNode node;

  @BeforeEach
  void beforeEach() {
    // Mirrors Player Speed+'s shape: 3 tiers, +10s increment every tier. Cost values are
    // irrelevant here - UpgradeNode itself has no currency concept, that's UpgradesDisplay's job.
    node = UpgradeNode.timeBased(
        "test_time_upgrade", "Test Time Upgrade", "For testing additive TIME expiry.",
        new int[] {10, 10, 10},
        new float[] {10f, 10f, 10f});
  }

  @Test
  void purchasingWithZeroRemainingGivesExactlyTheIncrement() {
    node.purchaseNextTier(); // 0 + 10 = 10s

    assertEquals(1, node.getCurrentTier());
    assertEquals("10s left", node.getRemainingText());
  }

  @Test
  void purchasingAgainWithTimeStillLeftAddsToItRatherThanResettingToAFlatTotal() {
    node.purchaseNextTier(); // 0 + 10 = 10s remaining
    node.tickTime(6f); // 10 - 6 = 4s remaining, still active

    node.purchaseNextTier(); // 4 + 10 = 14s remaining, NOT a flat 10s (or 20s)

    assertEquals(2, node.getCurrentTier());
    assertEquals("14s left", node.getRemainingText());
  }

  @Test
  void aThirdPurchaseShouldAlsoAddOnTopOfWhateverIsCurrentlyLeft() {
    node.purchaseNextTier(); // 10s remaining
    node.tickTime(6f); // 4s remaining
    node.purchaseNextTier(); // 4 + 10 = 14s remaining
    node.tickTime(5f); // 9s remaining

    node.purchaseNextTier(); // 9 + 10 = 19s remaining

    assertEquals(3, node.getCurrentTier());
    assertEquals("19s left", node.getRemainingText());
  }

  @Test
  void maxTierCapShouldStillBlockAFourthPurchase() {
    node.purchaseNextTier(); // Tier 1
    node.purchaseNextTier(); // Tier 2
    node.purchaseNextTier(); // Tier 3 - at max, since tierCosts.length == 3

    assertTrue(node.isMaxTier());
    String remainingBeforeFourthAttempt = node.getRemainingText();

    node.purchaseNextTier(); // should be a no-op - already at max tier

    assertEquals(3, node.getCurrentTier());
    assertEquals(remainingBeforeFourthAttempt, node.getRemainingText());
  }

  @Test
  void fullyExpiringResetsToInactiveRegardlessOfHowManyTiersWereStacked() {
    node.purchaseNextTier(); // 10s
    node.tickTime(6f); // 4s
    node.purchaseNextTier(); // 4 + 10 = 14s

    node.tickTime(20f); // more than enough to fully expire

    assertEquals(0, node.getCurrentTier());
    assertFalse(node.isActive());
  }
}
