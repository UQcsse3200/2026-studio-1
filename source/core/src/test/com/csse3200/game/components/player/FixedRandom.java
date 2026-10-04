package com.csse3200.game.components.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Random stub for gambling tests: always draws the same value and records every bound asked. */
class FixedRandom extends Random {
  /** Bounds passed to {@link #nextInt(int)}, in call order. Empty means nothing was rolled. */
  final List<Integer> bounds = new ArrayList<>();

  private final int value;

  FixedRandom(int value) {
    this.value = value;
  }

  @Override
  public int nextInt(int bound) {
    bounds.add(bound);
    return value;
  }
}
