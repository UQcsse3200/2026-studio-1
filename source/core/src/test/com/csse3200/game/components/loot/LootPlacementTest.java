package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootPlacementTest {
  private static final long SEED = 42L;

  /**
   * Acceptance criterion: loot is placed on the spawn points the map declares, so it always lands
   * on reachable ground rather than inside a wall.
   */
  @Test
  void shouldPlaceOneItemOnEverySpawnPoint() {
    List<SpawnPoint> points = List.of(new SpawnPoint("loot", 3, 4), new SpawnPoint("loot", 9, 2));

    List<LootPlacement.PlacedLoot> placed =
        LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points);

    assertEquals(2, placed.size());
    assertEquals(new GridPoint2(3, 4), placed.get(0).getPosition());
    assertEquals(new GridPoint2(9, 2), placed.get(1).getPosition());
    assertNotNull(placed.get(0).getItem());
    assertNotNull(placed.get(1).getItem());
  }

  /** Acceptance criterion: the same seed puts the same loot in the same places. */
  @Test
  void shouldPlaceTheSameLootForTheSameSeed() {
    List<SpawnPoint> points = manySpawnPoints(15);

    List<String> first = names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));
    List<String> second =
        names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));

    assertEquals(first, second);
  }

  /** A different seed lays out a different run. */
  @Test
  void shouldPlaceDifferentLootForADifferentSeed() {
    List<SpawnPoint> points = manySpawnPoints(15);

    List<String> first = names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));
    List<String> second =
        names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED + 1), points));

    assertTrue(!first.equals(second), "a different seed should lay out different loot");
  }

  /** A map with no declared loot spawns plans nothing, leaving the caller to fall back. */
  @Test
  void shouldPlanNothingWhenTheMapDeclaresNoLootSpawns() {
    List<LootPlacement.PlacedLoot> placed =
        LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), List.of());

    assertTrue(placed.isEmpty());
  }

  @Test
  void shouldRejectMissingArguments() {
    assertThrows(
        IllegalArgumentException.class, () -> LootPlacement.forSpawnPoints(null, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), null));
  }

  private List<SpawnPoint> manySpawnPoints(int count) {
    List<SpawnPoint> points = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      points.add(new SpawnPoint("loot", i, i));
    }
    return points;
  }

  private List<String> names(List<LootPlacement.PlacedLoot> placed) {
    List<String> names = new ArrayList<>();
    for (LootPlacement.PlacedLoot loot : placed) {
      names.add(loot.getItem().getName());
    }
    return names;
  }
}
