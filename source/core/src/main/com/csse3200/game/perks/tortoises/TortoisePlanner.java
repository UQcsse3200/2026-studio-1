package com.csse3200.game.perks.tortoises;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Decides, once per new game, how many tortoises there are and which maps hold the random ones.
 *
 * <p>The total is rolled from {@link #FIXED_TORTOISES} to {@link #MAX_TOTAL}. The fixed tortoises
 * (the ones placed on a map plus Zeus's drop) always exist and count towards it; the rest are
 * shared between the maps by weighted draw:
 *
 * <ul>
 *   <li>each of the eight rooms has one slot, weighted {@link #ROOM_WEIGHT}, so a room is likelier
 *       to hold one than a main level;
 *   <li>each main level has up to {@link #MAX_PER_LEVEL} slots (Level 3 only {@link #LEVEL_3_MAX}),
 *       weighted less the higher the level, so the chance falls as the player climbs.
 * </ul>
 *
 * <p>Because the plan is made up front, a total of 12 can always be reached. Nothing here touches
 * the game; {@link TortoiseCarrierAssigner} turns the plan into carriers when a map is built.
 */
public final class TortoisePlanner {
  /** Level 1's placed tortoise, a second placed one, and Zeus's drop. */
  public static final int FIXED_TORTOISES = 3;

  /** The largest total a game can roll. */
  public static final int MAX_TOTAL = 12;

  /** The most random tortoises one main level can hold. */
  public static final int MAX_PER_LEVEL = 4;

  /** The most random tortoises Level 3 can hold; it is the last level, so it gets fewer. */
  public static final int LEVEL_3_MAX = 2;

  /** Weight of each room's single slot, against 1.0 for the lowest main level's slots. */
  public static final float ROOM_WEIGHT = 3.5f;

  /** Weight of each slot of Level 1, Level 2 and Level 3: the chance falls as the player climbs. */
  public static final float LEVEL_1_WEIGHT = 1f;

  public static final float LEVEL_2_WEIGHT = 0.6f;
  public static final float LEVEL_3_WEIGHT = 0.3f;

  /** The eight room maps, named as in their map files. */
  public static final List<String> ROOMS =
      List.of(
          "Hound's Den",
          "Gorgon's Gallery",
          "Minotaur's Labyrinth",
          "Shades' Barracks",
          "Cyclops Forge",
          "Centaur Pavilion",
          "Zeus's Outer Guard",
          "Zeus's Thunder Hall");

  /** Main level map name to the weight of each of its slots; lower for higher levels. */
  public static final Map<String, Float> LEVEL_WEIGHTS = levelWeights();

  /** The three main levels in order, with their slot weights. */
  private static Map<String, Float> levelWeights() {
    // LinkedHashMap keeps a fixed order, so the same seed always gives the same plan
    Map<String, Float> weighted = new LinkedHashMap<>();
    weighted.put("Level 1 - Out of the Underworld", LEVEL_1_WEIGHT);
    weighted.put("Level 2 - Climb Mount Olympus", LEVEL_2_WEIGHT);
    weighted.put("Level 3 - Zeus's Palace", LEVEL_3_WEIGHT);
    return weighted;
  }

  /**
   * Starts a new game's tortoises: rolls the total, makes the plan and stores it in {@link
   * TortoisePlan}.
   *
   * @param random the source of randomness
   * @return the total, to give to {@code TortoiseLedger.setTotal}
   */
  public static int newGame(Random random) {
    int total = rollTotal(random);
    TortoisePlan.set(plan(total, ROOMS, LEVEL_WEIGHTS, random));
    return total;
  }

  /**
   * @param random the source of randomness
   * @return a total from {@link #FIXED_TORTOISES} to {@link #MAX_TOTAL}, both included
   */
  public static int rollTotal(Random random) {
    // RETURN FIXED_TORTOISES + random.nextInt(MAX_TOTAL - FIXED_TORTOISES + 1)
    return FIXED_TORTOISES + random.nextInt(MAX_TOTAL - FIXED_TORTOISES + 1);
  }

  /**
   * Shares the random tortoises (the total minus the fixed ones) between the maps, never giving a
   * room more than one, a main level more than {@link #MAX_PER_LEVEL} or Level 3 more than {@link
   * #LEVEL_3_MAX}.
   *
   * @param total the rolled total, {@link #FIXED_TORTOISES} to {@link #MAX_TOTAL}
   * @param rooms the room map names, one slot each
   * @param levelWeights main level map name to the weight of each of its slots
   * @param random the source of randomness
   * @return map name to how many random tortoises it holds; maps with none are left out
   * @throws IllegalArgumentException if the total is out of range
   */
  public static Map<String, Integer> plan(
      int total, List<String> rooms, Map<String, Float> levelWeights, Random random) {
    if (total < FIXED_TORTOISES || total > MAX_TOTAL) {
      throw new IllegalArgumentException("total out of range");
    }
    // One entry per slot; the two lists stay in step
    List<String> slotMaps = new ArrayList<>();
    List<Float> slotWeights = new ArrayList<>();
    for (String room : rooms) {
      slotMaps.add(room);
      slotWeights.add(ROOM_WEIGHT);
    }
    for (Map.Entry<String, Float> level : levelWeights.entrySet()) {
      for (int i = 0; i < slotsFor(level.getKey()); i++) {
        slotMaps.add(level.getKey());
        slotWeights.add(level.getValue());
      }
    }

    Map<String, Integer> counts = new LinkedHashMap<>();
    int toPlace = Math.min(total - FIXED_TORTOISES, slotMaps.size());
    for (int placed = 0; placed < toPlace; placed++) {
      int pick = pickWeighted(slotWeights, random);
      counts.merge(slotMaps.get(pick), 1, Integer::sum);
      slotMaps.remove(pick); // so no slot is used twice
      slotWeights.remove(pick);
    }
    return counts;
  }

  /**
   * @param level a main level map name
   * @return how many random tortoises that level can hold
   */
  private static int slotsFor(String level) {
    if (level.equals("Level 3 - Zeus's Palace")) {
      return LEVEL_3_MAX;
    }
    return MAX_PER_LEVEL;
  }

  /**
   * Picks one index with probability proportional to its weight.
   *
   * @param weights the weight of each slot; positive
   * @param random the source of randomness
   * @return the chosen index
   */
  private static int pickWeighted(List<Float> weights, Random random) {
    float sum = 0;
    for (float weight : weights) {
      sum += weight;
    }
    int pick = 0;
    float roll = random.nextFloat() * sum;
    while (pick < weights.size() - 1 && roll >= weights.get(pick)) {
      roll = roll - weights.get(pick);
      pick += 1;
    }
    return pick;
  }

  private TortoisePlanner() {
    throw new IllegalStateException("Utility class");
  }
}
