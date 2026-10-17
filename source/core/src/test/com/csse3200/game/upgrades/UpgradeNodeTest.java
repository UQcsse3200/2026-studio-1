package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the TIME-based expiry math in purchaseNextTier(): each purchase ADDS a fixed increment to
 * whatever time is currently remaining, rather than overwriting it with a flat per-tier total - and
 * confirms the tier-count cap is unaffected by that change.
 */
class UpgradeNodeTest {
  private UpgradeNode node;

  @BeforeEach
  void beforeEach() {
    // Mirrors Player Speed+'s shape: 3 tiers, +10s increment every tier. Cost values are
    // irrelevant here - UpgradeNode itself has no currency concept, that's UpgradesDisplay's job.
    node =
        UpgradeNode.timeBased(
            "test_time_upgrade",
            "Test Time Upgrade",
            "For testing additive TIME expiry.",
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

  /** The tierCosts/tierIncrementsSeconds arrays must be the same length for a TIME upgrade. */
  @Test
  void timeBasedConstructionRejectsMismatchedArrayLengths() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            UpgradeNode.timeBased(
                "bad_time_upgrade",
                "Bad Time Upgrade",
                "Mismatched arrays.",
                new int[] {10, 10},
                new float[] {10f, 10f, 10f}));
  }

  /** The tierCosts/tierKillCounts arrays must be the same length for a KILL_COUNT upgrade. */
  @Test
  void killCountBasedConstructionRejectsMismatchedArrayLengths() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            UpgradeNode.killCountBased(
                "bad_kill_upgrade",
                "Bad Kill Upgrade",
                "Mismatched arrays.",
                new int[] {10, 10},
                new int[] {3, 5, 8}));
  }

  /** getExpiryType() reports whichever expiry rule the node was built with. */
  @Test
  void getExpiryTypeReportsTimeOrKillCountAccordingToHowTheNodeWasBuilt() {
    UpgradeNode killNode =
        UpgradeNode.killCountBased(
            "test_kill_upgrade",
            "Test Kill Upgrade",
            "For testing getExpiryType().",
            new int[] {10, 10, 10},
            new int[] {3, 5, 8});

    assertEquals(UpgradeNode.ExpiryType.TIME, node.getExpiryType());
    assertEquals(UpgradeNode.ExpiryType.KILL_COUNT, killNode.getExpiryType());
  }

  /** getNextTierCost() reports the next tier's cost, then -1 once fully stacked. */
  @Test
  void getNextTierCostReportsTheCostOfTheNextPurchaseAndMinusOneAtMaxTier() {
    assertEquals(10, node.getNextTierCost()); // Tier 0 -> 1

    node.purchaseNextTier();
    node.purchaseNextTier();
    node.purchaseNextTier(); // now at max tier (3)

    assertEquals(-1, node.getNextTierCost());
  }

  /** getNextTierGrantText() describes a TIME upgrade's next purchase as an added-seconds amount. */
  @Test
  void getNextTierGrantTextDescribesATimeUpgradesNextIncrementAndIsEmptyAtMaxTier() {
    assertEquals("+10s", node.getNextTierGrantText());

    node.purchaseNextTier();
    node.purchaseNextTier();
    node.purchaseNextTier(); // max tier

    assertEquals("", node.getNextTierGrantText());
  }

  /**
   * getNextTierGrantText() describes a KILL_COUNT upgrade's next purchase as a total kill
   * threshold.
   */
  @Test
  void getNextTierGrantTextDescribesAKillCountUpgradesNextThresholdAndIsEmptyAtMaxTier() {
    UpgradeNode killNode =
        UpgradeNode.killCountBased(
            "test_kill_upgrade",
            "Test Kill Upgrade",
            "For testing getNextTierGrantText().",
            new int[] {10, 10, 10},
            new int[] {3, 5, 8});

    assertEquals("3 kills", killNode.getNextTierGrantText());

    killNode.purchaseNextTier();
    killNode.purchaseNextTier();
    killNode.purchaseNextTier(); // max tier

    assertEquals("", killNode.getNextTierGrantText());
  }

  /** tickTime() is a documented no-op while the upgrade is inactive (Tier 0). */
  @Test
  void tickTimeOnAnInactiveUpgradeIsANoOp() {
    node.tickTime(100f);

    assertEquals(0, node.getCurrentTier());
    assertFalse(node.isActive());
  }

  /** tickTime() is also a documented no-op for an active KILL_COUNT-based upgrade. */
  @Test
  void tickTimeOnAnActiveKillCountUpgradeIsANoOp() {
    UpgradeNode killNode =
        UpgradeNode.killCountBased(
            "test_kill_upgrade",
            "Test Kill Upgrade",
            "For testing tickTime() on a KILL_COUNT upgrade.",
            new int[] {10, 10, 10},
            new int[] {3, 5, 8});
    killNode.purchaseNextTier(); // Tier 1: 3 kills remaining

    killNode.tickTime(1000f); // should have no effect at all - KILL_COUNT ignores time

    assertEquals(1, killNode.getCurrentTier());
    assertEquals(3, killNode.getRemainingKills());
  }

  /** getRemainingText() is empty while the upgrade is inactive. */
  @Test
  void getRemainingTextIsEmptyWhileInactive() {
    assertEquals("", node.getRemainingText());
  }
}
