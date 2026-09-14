package com.csse3200.game.components.loot;

import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Picks a random item from a weighted list of potions and weapons.
 *
 * <p>Each entry has a weight, and an entry is chosen with a probability of its weight over the
 * total weight of the table. Rarity is therefore just a smaller weight: the standard table built by
 * {@link #createDefault(long)} gives tier 1 a weight of 60, tier 2 a weight of 30 and tier 3 a
 * weight of 10, so a tier 3 item turns up roughly once every ten rolls.
 *
 * <p>The random numbers come from a seeded {@link Random}, so the same seed always produces the
 * same run. That makes a bug found while playing reproducible instead of a one-off.
 *
 * <p>The tier weights here are a placeholder until the weapon tier system lands; once tier values
 * are published, {@link #createDefault(long)} should be built from those instead of the constants
 * in this class.
 */
public class LootTable {
  /** Weight of each tier in the standard table, from tier 1 upwards. */
  private static final int[] DEFAULT_TIER_WEIGHTS = {60, 30, 10};

  private final List<LootEntry> entries = new ArrayList<>();
  private final Random random;
  private int totalWeight;

  /**
   * Creates an empty table whose rolls are driven by the given seed.
   *
   * @param seed seed for the table's random number generator
   */
  public LootTable(long seed) {
    this(new Random(seed));
  }

  /**
   * Creates an empty table with an explicitly supplied random source.
   *
   * @param random random source to roll with
   * @throws IllegalArgumentException if {@code random} is null
   */
  public LootTable(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("Random must not be null.");
    }
    this.random = random;
  }

  /**
   * Builds the standard loot table: every potion and every weapon, at tiers 1 to 3, with higher
   * tiers weighted to be rarer.
   *
   * @param seed seed for the table's random number generator
   * @return a table ready to roll
   */
  public static LootTable createDefault(long seed) {
    LootTable table = new LootTable(seed);

    for (int tier = 1; tier <= DEFAULT_TIER_WEIGHTS.length; tier++) {
      int weight = DEFAULT_TIER_WEIGHTS[tier - 1];

      for (ConsumableType type : ConsumableType.values()) {
        table.addConsumable(type, tier, weight);
      }

      for (WeaponType type : WeaponType.values()) {
        table.addWeapon(type, tier, weight);
      }
    }

    return table;
  }

  /**
   * Adds a potion to the table.
   *
   * @param type potion to add
   * @param tier tier the potion is generated at; must be {@code > 0}
   * @param weight how often it is picked relative to other entries; must be {@code > 0}
   * @return this table, so entries can be chained
   * @throws IllegalArgumentException if any argument is invalid
   */
  public LootTable addConsumable(ConsumableType type, int tier, int weight) {
    if (type == null) {
      throw new IllegalArgumentException("ConsumableType must not be null.");
    }
    validateEntry(tier, weight);

    ConsumableGenerator generator = new ConsumableGenerator();
    return addEntry(weight, () -> generator.generateConsumable(type, tier));
  }

  /**
   * Adds a weapon to the table.
   *
   * @param type weapon to add
   * @param tier tier the weapon is generated at; must be {@code > 0}
   * @param weight how often it is picked relative to other entries; must be {@code > 0}
   * @return this table, so entries can be chained
   * @throws IllegalArgumentException if any argument is invalid
   */
  public LootTable addWeapon(WeaponType type, int tier, int weight) {
    if (type == null) {
      throw new IllegalArgumentException("WeaponType must not be null.");
    }
    validateEntry(tier, weight);

    WeaponGenerator generator = new WeaponGenerator();
    return addEntry(weight, () -> generator.generateWeapon(type, tier));
  }

  /**
   * Rolls a single item from the table, with its tier already applied.
   *
   * @return the generated item
   * @throws IllegalStateException if the table has no entries
   */
  public Item rollItem() {
    if (entries.isEmpty()) {
      throw new IllegalStateException("Cannot roll on an empty loot table.");
    }

    // Walk the entries subtracting their weights, so an entry twice as heavy covers twice as much
    // of the range and is picked twice as often.
    int pick = random.nextInt(totalWeight);
    for (LootEntry entry : entries) {
      pick -= entry.weight;
      if (pick < 0) {
        return entry.generator.get();
      }
    }

    // Only reachable if the weights and total disagree, which the add methods prevent.
    return entries.get(entries.size() - 1).generator.get();
  }

  /**
   * Rolls a single item and wraps it in a pickup entity ready to be placed in the world.
   *
   * @return the loot entity
   * @throws IllegalStateException if the table has no entries
   */
  public Entity roll() {
    return LootFactory.createLoot(rollItem());
  }

  /**
   * Returns whether the table has anything to roll.
   *
   * @return {@code true} when no entries have been added
   */
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  /**
   * Adds an entry and keeps the running total of weights up to date.
   *
   * @param weight weight of the entry
   * @param generator supplies the item when the entry is rolled
   * @return this table
   */
  private LootTable addEntry(int weight, Supplier<Item> generator) {
    entries.add(new LootEntry(weight, generator));
    totalWeight += weight;
    return this;
  }

  /**
   * Checks the values shared by every entry.
   *
   * @param tier tier the item is generated at
   * @param weight weight of the entry
   * @throws IllegalArgumentException if either value is not positive
   */
  private void validateEntry(int tier, int weight) {
    if (tier <= 0) {
      throw new IllegalArgumentException("Tier must be greater than 0.");
    }

    if (weight <= 0) {
      throw new IllegalArgumentException("Weight must be greater than 0.");
    }
  }

  /** One weighted entry in the table, and how to generate its item when it is rolled. */
  private static class LootEntry {
    private final int weight;
    private final Supplier<Item> generator;

    private LootEntry(int weight, Supplier<Item> generator) {
      this.weight = weight;
      this.generator = generator;
    }
  }
}
