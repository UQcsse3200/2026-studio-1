package com.csse3200.game.components.loot;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Works out what loot goes where before anything is spawned.
 *
 * <p>Loot goes on a list of spawn points: the ones a map declares when it has any, or spots picked
 * at random from the open ground by {@link #pickRandomSpots} when it does not. Each spot then gets
 * one item rolled from the loot table. Keeping the decision separate from the spawning also means
 * the layout can be tested without standing up a game area.
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

  /**
   * Picks distinct spots at random from a list of candidates.
   *
   * <p>A copy of the candidates is shuffled and the first {@code count} are taken, so no two pieces
   * of loot share a tile and the caller's list is left in its original order. With the same seed
   * the same spots come out every time.
   *
   * @param candidates tiles loot could go on
   * @param count how many spots to pick; must be {@code >= 0}
   * @param random random source to pick with
   * @return up to {@code count} distinct spots, or every candidate when there are fewer
   * @throws IllegalArgumentException if {@code candidates} or {@code random} is null, or {@code
   *     count} is negative
   */
  public static List<SpawnPoint> pickRandomSpots(
      List<SpawnPoint> candidates, int count, Random random) {
    if (candidates == null) {
      throw new IllegalArgumentException("Candidates must not be null.");
    }

    if (count < 0) {
      throw new IllegalArgumentException("Count must not be negative.");
    }

    if (random == null) {
      throw new IllegalArgumentException("Random must not be null.");
    }

    List<SpawnPoint> shuffled = new ArrayList<>(candidates);
    Collections.shuffle(shuffled, random);

    int picked = Math.min(count, shuffled.size());
    return new ArrayList<>(shuffled.subList(0, picked));
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
