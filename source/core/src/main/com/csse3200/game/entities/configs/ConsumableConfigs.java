package com.csse3200.game.entities.configs;

import com.csse3200.game.components.loot.ConsumableType;

/**
 * Defines all consumable configs to be loaded by the consumable generator.
 *
 * <p>Mirrors the {@link NPCConfigs} pattern: one public field per variant, populated from {@code
 * configs/consumables.json} via {@code FileLoader}.
 */
public class ConsumableConfigs {
  public ConsumableConfig healthPotion = new ConsumableConfig();
  public ConsumableConfig damageBuff = new ConsumableConfig();
  public ConsumableConfig speedBuff = new ConsumableConfig();

  /**
   * Returns the config backing the given consumable type.
   *
   * @param type consumable type to look up
   * @return the matching config
   * @throws IllegalArgumentException if {@code type} is null or has no config
   */
  public ConsumableConfig get(ConsumableType type) {
    if (type == null) {
      throw new IllegalArgumentException("ConsumableType must not be null.");
    }
    switch (type) {
      case HEALTH_POTION:
        return healthPotion;
      case DAMAGE_BUFF:
        return damageBuff;
      case SPEED_BUFF:
        return speedBuff;
      default:
        throw new IllegalArgumentException("No config for consumable type " + type);
    }
  }
}
