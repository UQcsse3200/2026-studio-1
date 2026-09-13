package com.csse3200.game.components.loot;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.entities.Entity;

/**
 * Restores health, capped at the entity's maximum health.
 *
 * <p>The cap is read from the entity's {@link ConsumableUseComponent} rather than from {@code
 * CombatStatsComponent}, which has no maximum health field and is shared with other teams. When no
 * cap is available the heal is left unbounded, matching current engine behaviour.
 */
public class HealEffect implements ConsumableEffect {
  private final int healAmount;

  /**
   * Creates a healing effect.
   *
   * @param healAmount health to restore; must be {@code > 0}
   * @throws IllegalArgumentException if {@code healAmount} is not positive
   */
  public HealEffect(int healAmount) {
    if (healAmount <= 0) {
      throw new IllegalArgumentException("healAmount must be greater than 0.");
    }
    this.healAmount = healAmount;
  }

  /**
   * Returns the health this effect restores.
   *
   * @return heal amount
   */
  public int getHealAmount() {
    return healAmount;
  }

  /**
   * Heals the entity, clamping to its maximum health.
   *
   * @param entity entity to heal
   * @return {@code false} if the entity has no combat stats or is already at full health
   */
  @Override
  public boolean apply(Entity entity) {
    if (entity == null) {
      return false;
    }
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return false;
    }

    int maxHealth = resolveMaxHealth(entity);
    int current = stats.getHealth();
    if (current >= maxHealth) {
      return false;
    }

    stats.setHealth(Math.min(current + healAmount, maxHealth));
    return true;
  }

  /**
   * Resolves the entity's maximum health.
   *
   * @param entity entity being healed
   * @return the configured cap, or {@link Integer#MAX_VALUE} when the entity declares none
   */
  private int resolveMaxHealth(Entity entity) {
    ConsumableUseComponent useComponent = entity.getComponent(ConsumableUseComponent.class);
    return useComponent == null ? Integer.MAX_VALUE : useComponent.getMaxHealth();
  }
}
