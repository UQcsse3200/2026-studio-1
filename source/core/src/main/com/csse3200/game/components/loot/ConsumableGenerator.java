package com.csse3200.game.components.loot;

import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.entities.configs.ConsumableConfig;
import com.csse3200.game.entities.configs.ConsumableConfigs;
import com.csse3200.game.files.FileLoader;

/**
 * Generates consumable items with properties scaled by loot tier.
 *
 * <p>Base values are data driven, loaded from {@code configs/consumables.json}, so rarities produce
 * stronger variants without changing code. Healing scales linearly with tier, and a buff's
 * magnitude scales by its distance above 1.0, so a tier 1 buff of 1.5 becomes 2.0 at tier 2.
 *
 * <p>Items above tier 1 are given a tier-qualified name. Inventory stacking matches on name, type
 * and stack size, so this keeps a weak potion from stacking with a strong one.
 */
public class ConsumableGenerator {
  private static final String CONFIG_PATH = "configs/consumables.json";

  private final ConsumableConfigs configs;

  /** Creates a generator using the consumable configs on disk. */
  public ConsumableGenerator() {
    this(FileLoader.readClass(ConsumableConfigs.class, CONFIG_PATH));
  }

  /**
   * Creates a generator using explicitly supplied configs.
   *
   * @param configs consumable configs to generate from
   * @throws IllegalArgumentException if {@code configs} is null
   */
  public ConsumableGenerator(ConsumableConfigs configs) {
    if (configs == null) {
      throw new IllegalArgumentException("ConsumableConfigs must not be null.");
    }
    this.configs = configs;
  }

  /**
   * Generates a consumable of the given type at the given loot tier.
   *
   * @param type consumable to generate
   * @param tier loot tier; must be {@code > 0}
   * @return the generated item with a stack quantity of 1
   * @throws IllegalArgumentException if {@code type} is null or {@code tier} is not positive
   */
  public ConsumableItem generateConsumable(ConsumableType type, int tier) {
    if (type == null) {
      throw new IllegalArgumentException("ConsumableType must not be null.");
    }

    if (tier <= 0) {
      throw new IllegalArgumentException("Tier must be greater than 0.");
    }

    ConsumableConfig config = configs.get(type);
    String name = buildName(config.name, tier);

    switch (type) {
      case HEALTH_POTION:
        return new ConsumableItem(
            name, type, new HealEffect(config.healAmount * tier), 1, config.maxQuantity);

      case DAMAGE_BUFF:
        return new ConsumableItem(
            name, type, buildBuff(BuffStat.DAMAGE, config, tier), 1, config.maxQuantity);

      case SPEED_BUFF:
        return new ConsumableItem(
            name, type, buildBuff(BuffStat.SPEED, config, tier), 1, config.maxQuantity);

      default:
        throw new IllegalArgumentException("Unsupported consumable type.");
    }
  }

  /**
   * Builds a tier-scaled buff effect.
   *
   * @param stat stat the buff modifies
   * @param config base values for the buff
   * @param tier loot tier
   * @return the scaled effect
   */
  private BuffEffect buildBuff(BuffStat stat, ConsumableConfig config, int tier) {
    // The part of the multiplier above 1.0 is the bonus, and the bonus grows with tier.
    // A configured 1.5 gives 1.5 at tier 1, 2.0 at tier 2, and so on.
    float bonus = config.magnitude - 1f;
    float magnitude = 1f + bonus * tier;
    return new BuffEffect(stat, magnitude, config.durationSeconds);
  }

  /**
   * Qualifies an item name with its tier so tiers do not stack together.
   *
   * @param baseName configured display name
   * @param tier loot tier
   * @return the base name at tier 1, otherwise a tier-qualified name
   */
  private String buildName(String baseName, int tier) {
    if (tier == 1) {
      return baseName;
    }
    return String.format("%s (Tier %d)", baseName, tier);
  }
}
