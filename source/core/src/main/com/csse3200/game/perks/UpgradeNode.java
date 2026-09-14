package com.csse3200.game.perks;

/**
 * Represents a single purchasable, temporary, tiered upgrade.
 *
 * <p>Buying an upgrade for the first time activates it at Tier 1. Buying the SAME upgrade again
 * while it's still active advances it to the next tier - a stronger effect AND a longer expiry
 * window than the previous tier gave. Once the upgrade's time/kill-count expiry fully runs out, the
 * whole stack wears off at once - it resets fully back to Tier 0 (inactive), not down by one tier -
 * and can be bought again from Tier 1 next time.
 *
 * <p>Expiry works one of two ways: - TIME: each purchase ADDS a fixed increment (in seconds) on top
 * of whatever time is currently remaining - it never resets to a flat total. E.g. buying with 4s
 * left and a +10s increment leaves 14s remaining, not a flat 10s (or 20s). (Movement upgrades, e.g.
 * Player Speed+; will apply the same way to Shield Durability/Regen on Kill once those are built) -
 * KILL_COUNT: wears off after a fixed number of kills while active; each purchase OVERWRITES the
 * remaining kill count with that tier's total threshold (Action upgrades, e.g. Sword Damage, Attack
 * Speed)
 *
 * <p>Each tier has its own cost - and, depending on expiry type, either its own additive time
 * increment (TIME) or its own total kill threshold (KILL_COUNT). The cost array and whichever
 * expiry array is used must be the same length. Higher tiers should generally grant a larger value
 * so stacking up is meaningfully rewarding, not just cosmetic.
 */
public class UpgradeNode {

  public enum ExpiryType {
    TIME,
    KILL_COUNT
  }

  private final String id;
  private final String name;
  private final String description;
  private final int[] tierCosts; // cost to advance FROM index i TO tier i+1, e.g. {40, 35, 30}
  private final ExpiryType expiryType;
  private final float[] tierIncrementsSeconds; // seconds ADDED to remainingSeconds per purchase
  // at each tier (TIME upgrades) - additive, never
  // a flat replacement
  private final int[]
      tierKillCounts; // total kill threshold granted AT each tier (KILL_COUNT upgrades)

  private int currentTier = 0; // 0 = inactive/not currently owned
  private float remainingSeconds = 0f;
  private int remainingKills = 0;

  // Optional gameplay hooks - kept as generic Runnables so this class stays free of any
  // dependency on player/combat-specific types. Registered by whoever builds the node (e.g.
  // UpgradesDisplay) and invoked whenever tier changes or the upgrade fully expires.
  private Runnable onTierChanged;
  private Runnable onExpired;

  /**
   * tierCosts and tierIncrementsSeconds must be the same length. Each tier's value is ADDED to
   * whatever time is currently remaining when purchased - not a flat replacement. E.g. buying with
   * 4s left and this tier's increment at 10s leaves 14s remaining.
   */
  public static UpgradeNode timeBased(
      String id, String name, String description, int[] tierCosts, float[] tierIncrementsSeconds) {
    return new UpgradeNode(
        id, name, description, tierCosts, ExpiryType.TIME, tierIncrementsSeconds, null);
  }

  /**
   * tierCosts and tierKillCounts must be the same length. Kill thresholds should increase per tier.
   */
  public static UpgradeNode killCountBased(
      String id, String name, String description, int[] tierCosts, int[] tierKillCounts) {
    return new UpgradeNode(
        id, name, description, tierCosts, ExpiryType.KILL_COUNT, null, tierKillCounts);
  }

  private UpgradeNode(
      String id,
      String name,
      String description,
      int[] tierCosts,
      ExpiryType expiryType,
      float[] tierIncrementsSeconds,
      int[] tierKillCounts) {
    if (expiryType == ExpiryType.TIME && tierCosts.length != tierIncrementsSeconds.length) {
      throw new IllegalArgumentException(
          "tierCosts and tierIncrementsSeconds must be the same length");
    }
    if (expiryType == ExpiryType.KILL_COUNT && tierCosts.length != tierKillCounts.length) {
      throw new IllegalArgumentException("tierCosts and tierKillCounts must be the same length");
    }
    this.id = id;
    this.name = name;
    this.description = description;
    this.tierCosts = tierCosts;
    this.expiryType = expiryType;
    this.tierIncrementsSeconds = tierIncrementsSeconds;
    this.tierKillCounts = tierKillCounts;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getMaxTier() {
    return tierCosts.length;
  }

  public int getCurrentTier() {
    return currentTier;
  }

  public boolean isActive() {
    return currentTier > 0;
  }

  public boolean isMaxTier() {
    return currentTier >= tierCosts.length;
  }

  public ExpiryType getExpiryType() {
    return expiryType;
  }

  /**
   * Registers a callback fired every time {@link #purchaseNextTier()} successfully changes tier
   * (covers both first activation and later tier advances - inspect {@link #getCurrentTier()}
   * inside the callback to tell them apart).
   */
  public void setOnTierChanged(Runnable onTierChanged) {
    this.onTierChanged = onTierChanged;
  }

  /**
   * Registers a callback fired once the upgrade fully expires and resets to Tier 0 (via {@link
   * #tickTime(float)} or {@link #onEnemyKilled()} running out).
   */
  public void setOnExpired(Runnable onExpired) {
    this.onExpired = onExpired;
  }

  /** Cost to advance to the NEXT tier. Returns -1 if already at max tier. */
  public int getNextTierCost() {
    if (isMaxTier()) {
      return -1;
    }
    return tierCosts[currentTier];
  }

  /**
   * Human-readable description of what the NEXT tier purchase would grant, e.g. "+10s" (added to
   * whatever time is currently remaining) or "5 kills" (the new total). Empty string if already at
   * max tier.
   */
  public String getNextTierGrantText() {
    if (isMaxTier()) {
      return "";
    }
    return expiryType == ExpiryType.TIME
        ? "+" + (int) tierIncrementsSeconds[currentTier] + "s"
        : tierKillCounts[currentTier] + " kills";
  }

  /**
   * Purchases the next tier (Tier 1 if currently inactive, Tier+1 otherwise). Only the tier COUNT
   * cap (getMaxTier(), i.e. tierCosts.length) limits how many times this can be bought before it
   * must fully expire and reset - the duration/kill-count math below is otherwise independent of
   * that cap.
   *
   * <p>TIME-based upgrades ADD this tier's increment on top of whatever time is currently remaining
   * - buying again while time is still left always extends it further, it never resets to a flat
   * total (e.g. 4s left + a 10s increment = 14s, not a flat 10s).
   *
   * <p>KILL_COUNT-based upgrades still work the old way: the remaining kill count is REPLACED with
   * this tier's total threshold.
   */
  public void purchaseNextTier() {
    if (isMaxTier()) {
      return;
    }
    currentTier++;
    int tierIndex = currentTier - 1; // tier 1 -> array index 0
    if (expiryType == ExpiryType.TIME) {
      remainingSeconds += tierIncrementsSeconds[tierIndex];
    } else {
      remainingKills = tierKillCounts[tierIndex];
    }
    if (onTierChanged != null) {
      onTierChanged.run();
    }
  }

  /** Call every frame with the time elapsed. No-op for kill-count-based upgrades. */
  public void tickTime(float deltaSeconds) {
    if (!isActive() || expiryType != ExpiryType.TIME) {
      return;
    }
    remainingSeconds -= deltaSeconds;
    if (remainingSeconds <= 0) {
      resetToInactive();
    }
  }

  /** Call whenever the player gets a kill. No-op for time-based upgrades. */
  public void onEnemyKilled() {
    if (!isActive() || expiryType != ExpiryType.KILL_COUNT) {
      return;
    }
    remainingKills--;
    if (remainingKills <= 0) {
      resetToInactive();
    }
  }

  /** The whole stack wears off at once - full reset to Tier 0, not a step down. */
  private void resetToInactive() {
    currentTier = 0;
    remainingSeconds = 0;
    remainingKills = 0;
    if (onExpired != null) {
      onExpired.run();
    }
  }

  /** Human-readable remaining-effect text for the UI, e.g. "7s left" or "3 kills left". */
  public String getRemainingText() {
    if (!isActive()) {
      return "";
    }
    return expiryType == ExpiryType.TIME
        ? (int) Math.ceil(remainingSeconds) + "s left"
        : remainingKills + " kills left";
  }
}
