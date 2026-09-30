package com.csse3200.game.perks;

/**
 * To add a new perk tied to an event that's already reported somewhere in the game (e.g.
 * "dashUsed", "enemyKilled"): add one new {@code PerkService.register(new Perk(...))} line below.
 * Nothing else needs to change.
 *
 * <p>To add a perk for a brand-new milestone (e.g. once wall jumps are implemented): add one {@code
 * PerkService.recordEvent("wallJumpUsed", 1);} call at the point in the game where a wall jump
 * actually happens, plus one new line here. Still nothing else needs to change.
 */
public final class PerkDefinitions {
  private PerkDefinitions() {}

  // Dont judge me for the perk IDs :D
  public static void registerAll() {
    PerkService.register(
        new Perk(
            "shieldMaster",
            "Shield Enhancement",
            "Activate your shield 5 times",
            "shieldActivated",
            5));

    PerkService.register(new Perk("thickSkin", "Max Health", "Kill 10 enemies", "enemyKilled", 10));

    PerkService.register(
        new Perk("timeLord", "Time Freeze", "Find the 5 times turtle", "Tortoise", 5));

    PerkService.register(
        new Perk("snatcher", "Loot Drop", "Spend x amount of gold", "GoldSpent", 120));
  }
}
