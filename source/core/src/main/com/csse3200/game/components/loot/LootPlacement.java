package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out what loot goes where before anything is spawned.
 *
 * <p>Maps declare their own loot spawn points, which are already on reachable ground, so placement
 * is a matter of rolling the loot table once per declared point rather than searching the terrain
 * for a legal position. Keeping the decision separate from the spawning also means the layout can
 * be tested without standing up a game area.
 */
public class LootPlacement {

  /**
   * Rolls one item for each loot spawn point the map declares.
   *
   * @param table loot table to roll from
   * @param lootSpawns loot spawn points declared by the map
   * @return the planned loot, in spawn point order; empty when the map declares none
   * @throws IllegalArgumentException if either argument is null
   */
  public static List<PlacedLoot> forSpawnPoints(LootTable table, List<SpawnPoint> lootSpawns) {
    if (table == null) {
      throw new IllegalArgumentException("LootTable must not be null.");
    }

    if (lootSpawns == null) {
      throw new IllegalArgumentException("Loot spawns must not be null.");
    }

    List<PlacedLoot> placed = new ArrayList<>();
    for (SpawnPoint spawn : lootSpawns) {
      placed.add(new PlacedLoot(spawn.getPosition(), table.rollItem()));
    }
    return placed;
  }

  private LootPlacement() {
    throw new IllegalStateException("Instantiating static util class");
  }

  /** One item and the tile it should be spawned on. */
  public static class PlacedLoot {
    private final GridPoint2 position;
    private final Item item;

    /**
     * Creates a planned piece of loot.
     *
     * @param position tile the item spawns on
     * @param item item to spawn
     */
    public PlacedLoot(GridPoint2 position, Item item) {
      this.position = position;
      this.item = item;
    }

    /**
     * Returns the tile this item spawns on.
     *
     * @return spawn position in tile coordinates
     */
    public GridPoint2 getPosition() {
      return position;
    }

    /**
     * Returns the item to spawn.
     *
     * @return the rolled item, with its tier already applied
     */
    public Item getItem() {
      return item;
    }
  }
}
