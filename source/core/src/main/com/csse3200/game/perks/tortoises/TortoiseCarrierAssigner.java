package com.csse3200.game.perks.tortoises;

import com.csse3200.game.components.enemies.EnemyTypeComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Turns {@link TortoisePlan} into actual carriers when a map is built: picks easy enemies on the
 * map and gives each a {@link TortoiseDropComponent}.
 *
 * <p>Only the easy enemies can carry one (the Skeleton, Ranged Skeleton, Harpy and Ranged Harpy),
 * so a mini boss, an escort or Zeus is never picked. Tortoises whose carrier cannot be found, for
 * example because the enemies on the map were already killed in an earlier visit, are handed back
 * so the caller can place them on the ground instead. That is what stops a total from becoming
 * unreachable when a carrier died before its tortoise was picked up.
 *
 * <p>The enemies must not have been created yet: {@code Entity.addComponent} ignores a component
 * added after creation. If one was, its tortoise is handed back like any other without a carrier.
 */
public final class TortoiseCarrierAssigner {
  /** Start of every random tortoise's id. */
  private static final String ID_PREFIX = "rnd_";

  /**
   * The type labels of the enemies allowed to carry a tortoise, as EnemyTypeComponent spells them.
   */
  private static final Set<String> EASY_ENEMY_LABELS =
      Set.of("Skeleton", "Ranged Skeleton", "Harpy", "Ranged Harpy");

  /**
   * Gives this map's planned random tortoises to carriers.
   *
   * @param mapName the map's name, as in {@code MapData.getName()}
   * @param enemies every enemy built for the map, before any of them is registered
   * @param random the source of randomness
   * @return the ids of planned tortoises that were not found yet and got no carrier; the caller
   *     should place each on the ground. Empty if everything was assigned.
   */
  public static List<String> assign(String mapName, List<Entity> enemies, Random random) {
    int planned = TortoisePlan.countFor(mapName);
    List<String> needGround = new ArrayList<>();
    if (planned == 0) {
      return needGround;
    }

    List<Entity> candidates = new ArrayList<>();
    for (Entity enemy : enemies) {
      if (isEligible(enemy)) {
        candidates.add(enemy);
      }
    }
    Collections.shuffle(candidates, random);

    int next = 0;
    for (int slot = 1; slot <= planned; slot++) {
      String id = tortoiseId(mapName, slot);
      if (TortoiseFactory.isFound(id)) {
        continue; // already collected, nothing to place
      }
      if (next < candidates.size()) {
        Entity carrier = candidates.get(next);
        next++;
        carrier.addComponent(new TortoiseDropComponent(id));
        if (carrier.getComponent(TortoiseDropComponent.class) == null) {
          needGround.add(id); // the enemy was already created, so the component was ignored
        }
      } else {
        needGround.add(id); // no carrier left: the caller puts it on the ground
      }
    }
    return needGround;
  }

  /**
   * @param mapName the map's name
   * @param slot the slot number, from 1 to the map's planned count
   * @return the stable id of that slot's tortoise, for example {@code "rnd_hound_s_den_1"}
   */
  public static String tortoiseId(String mapName, int slot) {
    String slug = mapName.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    return ID_PREFIX + slug + "_" + slot;
  }

  /**
   * @param enemy an enemy on the map
   * @return true if it is an easy enemy that does not already carry a tortoise
   */
  static boolean isEligible(Entity enemy) {
    EnemyTypeComponent type = enemy.getComponent(EnemyTypeComponent.class);
    return type != null
        && EASY_ENEMY_LABELS.contains(type.getEnemyLabel())
        && enemy.getComponent(TortoiseDropComponent.class) == null;
  }

  private TortoiseCarrierAssigner() {
    throw new IllegalStateException("Utility class");
  }
}
