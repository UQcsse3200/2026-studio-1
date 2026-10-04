package com.csse3200.game.difficulty;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;

/**
 * Applies the currently selected {@link Difficulty}'s multipliers to a single map-spawned enemy.
 *
 * <p>Sits behind {@link com.csse3200.game.entities.spawn.EntitySpawnRegistry#create}, the one place
 * every map-spawned enemy passes through regardless of which team's factory built it, so no
 * individual factory needs to know difficulty exists.
 */
public final class DifficultyScaler {

  private DifficultyScaler() {}

  /**
   * Scales {@code enemy}'s combat stats, attack cooldown, and gold drop by the current difficulty's
   * multipliers. Each of the four scaled components is optional and handled independently, so an
   * enemy missing any of them is simply left alone for that stat.
   *
   * <p>Only hostile entities are scaled - those with a {@link MeleeAttackComponent}, {@link
   * RangedAttackComponent}, or {@link TouchAttackComponent}. The registry also builds non-enemies
   * (e.g. an NPC with a {@link CombatStatsComponent} but no way to attack), which must not be
   * touched by difficulty at all.
   *
   * @param enemy the entity to scale, or {@code null} - a no-op, since {@code
   *     EntitySpawnRegistry.create()} can return null when a spawn name isn't registered
   */
  public static void apply(Entity enemy) {
    if (enemy == null) {
      return;
    }

    boolean isHostile =
        enemy.getComponent(MeleeAttackComponent.class) != null
            || enemy.getComponent(RangedAttackComponent.class) != null
            || enemy.getComponent(TouchAttackComponent.class) != null;
    if (!isHostile) {
      return;
    }

    Difficulty current = DifficultyService.getCurrent();

    CombatStatsComponent combatStats = enemy.getComponent(CombatStatsComponent.class);
    if (combatStats != null) {
      combatStats.setHealth(scale(combatStats.getHealth(), current.getEnemyHealthMultiplier(), 1));
      combatStats.setBaseAttack(
          scale(combatStats.getBaseAttack(), current.getEnemyDamageMultiplier(), 1));
    }

    MeleeAttackComponent meleeAttack = enemy.getComponent(MeleeAttackComponent.class);
    if (meleeAttack != null) {
      meleeAttack.setCooldown(meleeAttack.getCooldown() * current.getAttackCooldownMultiplier());
      meleeAttack.setDamageMultiplier(current.getEnemyDamageMultiplier());
      meleeAttack.setWindupMultiplier(current.getAttackWindupMultiplier());
    }

    // Ranged attackers are deliberately not scaled here: RangedAttackComponent stores a
    // windupDuration but never counts it down, so there is no wind-up for difficulty to change.
    RangedAttackComponent rangedAttack = enemy.getComponent(RangedAttackComponent.class);
    if (rangedAttack != null) {
      rangedAttack.setCooldown(rangedAttack.getCooldown() * current.getAttackCooldownMultiplier());
    }

    InventoryComponent inventory = enemy.getComponent(InventoryComponent.class);
    if (inventory != null) {
      inventory.setGold(scale(inventory.getGold(), current.getGoldMultiplier(), 0));
    }
  }

  /**
   * Scales {@code value} by {@code multiplier}, rounded to the nearest int, floored at {@code
   * minimum} - except {@code 0} itself, which always stays {@code 0} (e.g. a stationary enemy's 0
   * base attack must not be floored up to 1 just because it has a positive minimum).
   */
  private static int scale(int value, float multiplier, int minimum) {
    if (value == 0) {
      return 0;
    }
    return Math.max(minimum, Math.round(value * multiplier));
  }
}
