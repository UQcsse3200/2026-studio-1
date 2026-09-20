package com.csse3200.game.components.npc;

import java.util.Random;

/**
 * Generates procedural names for NPCs.
 *
 * <p>Names are created by combining a randomly selected prefix with a randomly selected suffix.
 */
public class NameGenerator {
  private static final String[] PREFIXES = {
    "Grim", "Ash", "Dread", "Blood", "Night", "Iron", "Dark", "Bone"
  };

  private static final String[] SUFFIXES = {
    "fang", "bane", "claw", "crusher", "walker", "shade", "heart", "skull"
  };

  private final Random random;

  /** Creates a name generator using a new random number generator. */
  public NameGenerator() {
    this.random = new Random();
  }

  /**
   * Creates a name generator using the supplied random number generator.
   *
   * @param random random number generator to use
   */
  public NameGenerator(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("Random must not be null.");
    }
    this.random = random;
  }

  /**
   * Generates a procedural NPC name.
   *
   * @return a generated NPC name
   */
  public String generateName() {
    String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
    String suffix = SUFFIXES[random.nextInt(SUFFIXES.length)];

    return prefix + suffix;
  }
}
