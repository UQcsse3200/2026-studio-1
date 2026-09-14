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

  public static void registerAll() {
    PerkService.register(
        new Perk("dash_novice", "Dash Novice", "Use dash 10 times", "dashUsed", 10));
    PerkService.register(
        new Perk("dash_master", "Dash Master", "Use dash 100 times", "dashUsed", 100));

    PerkService.register(
        new Perk("wall_jumper", "Wall Jumper", "Perform 25 wall jumps", "wallJumpUsed", 25));

    PerkService.register(new Perk("slayer", "Slayer", "Defeat 50 enemies", "enemyKilled", 50));
    PerkService.register(
        new Perk("boss_hunter", "Boss Hunter", "Defeat 5 bosses", "bossDefeated", 5));
  }
}
