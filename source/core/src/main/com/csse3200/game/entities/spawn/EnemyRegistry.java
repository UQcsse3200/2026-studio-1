package com.csse3200.game.entities.spawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Tracks which enemy spawn points have already been killed, independent of the current
// session. Populated from saved data on load, and read back into saved data when saving.
public class EnemyRegistry {

  private static final Set<String> killed = new HashSet<>();

  public static void markKilled(String id) {
    killed.add(id);
  }

  public static boolean isKilled(String id) {
    return killed.contains(id);
  }

  public static void loadFrom(List<String> ids) {
    killed.clear();
    if (ids != null) {
      killed.addAll(ids);
    }
  }

  public static List<String> exportAll() {
    return new ArrayList<>(killed);
  }

  private EnemyRegistry() {
    throw new IllegalStateException("Utility class");
  }
}
