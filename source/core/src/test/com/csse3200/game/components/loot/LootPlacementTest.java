package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.areas.terrain.map.SpawnPoint;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    assertEquals(2, placed.size(), "each spawn point should get exactly one item");
    assertEquals(
        new GridPoint2(3, 4),
        placed.get(0).getPosition(),
        "the first item should sit on the first spawn point");
    assertEquals(
        new GridPoint2(9, 2),
        placed.get(1).getPosition(),
        "the second item should sit on the second spawn point");
    assertNotNull(placed.get(0).getItem(), "the first spawn point should be given an item");
    assertNotNull(placed.get(1).getItem(), "the second spawn point should be given an item");
  }

  /** Acceptance criterion: the same seed puts the same loot in the same places. */
  @Test
  void shouldPlaceTheSameLootForTheSameSeed() {
    List<SpawnPoint> points = manySpawnPoints(15);

    List<String> first = names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));
    List<String> second =
        names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));

    assertEquals(first, second, "the same seed should lay out the same loot in the same places");
  }

  /** A different seed lays out a different run. */
  @Test
  void shouldPlaceDifferentLootForADifferentSeed() {
    List<SpawnPoint> points = manySpawnPoints(15);

    List<String> first = names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), points));
    List<String> second =
        names(LootPlacement.forSpawnPoints(LootTable.createDefault(SEED + 1), points));

    assertNotEquals(first, second, "a different seed should lay out different loot");
  }

  /** A map with no declared loot spawns plans nothing, leaving the caller to fall back. */
  @Test
  void shouldPlanNothingWhenTheMapDeclaresNoLootSpawns() {
    List<LootPlacement.PlacedLoot> placed =
        LootPlacement.forSpawnPoints(LootTable.createDefault(SEED), List.of());

    assertTrue(placed.isEmpty(), "a map with no loot spawn points should get no loot");
  }

  @Test
  void shouldRejectAMissingLootTable() {
    List<SpawnPoint> noSpawns = List.of();

    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.forSpawnPoints(null, noSpawns),
        "placing loot without a loot table should be rejected");
  }

  @Test
  void shouldRejectMissingSpawnPoints() {
    LootTable table = LootTable.createDefault(SEED);

    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.forSpawnPoints(table, null),
        "placing loot without a list of spawn points should be rejected");
  }

  /**
   * Acceptance criterion: when a map declares no loot spawns, loot is spread over distinct spots
   * instead of stacking up in one row.
   */
  @Test
  void shouldPickTheRequestedNumberOfDistinctSpots() {
    List<SpawnPoint> candidates = manySpawnPoints(20);

    List<SpawnPoint> picked = LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED));

    assertEquals(8, picked.size(), "exactly the requested number of spots should be picked");
    assertEquals(
        8,
        positions(picked).stream().distinct().count(),
        "no two pieces of loot should share a spot");
    for (SpawnPoint spot : picked) {
      assertTrue(candidates.contains(spot), spot + " should come from the candidate list");
    }
  }

  /** A small map with fewer usable spots than requested uses every one of them. */
  @Test
  void shouldPickEveryCandidateWhenThereAreFewerThanRequested() {
    List<SpawnPoint> candidates = manySpawnPoints(3);

    List<SpawnPoint> picked = LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED));

    assertEquals(3, picked.size(), "all three candidates should be used when 8 are requested");
  }

  /** Acceptance criterion: the same seed picks the same spots, so a run can be reproduced. */
  @Test
  void shouldPickTheSameSpotsForTheSameSeed() {
    List<SpawnPoint> candidates = manySpawnPoints(20);

    List<GridPoint2> first =
        positions(LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED)));
    List<GridPoint2> second =
        positions(LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED)));

    assertEquals(first, second, "the same seed should pick the same spots in the same order");
  }

  /** Acceptance criterion: a different seed picks different spots, so each run varies. */
  @Test
  void shouldPickDifferentSpotsForADifferentSeed() {
    List<SpawnPoint> candidates = manySpawnPoints(20);

    List<GridPoint2> first =
        positions(LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED)));
    List<GridPoint2> second =
        positions(LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED + 1)));

    assertNotEquals(first, second, "a different seed should pick different spots");
  }

  /** Picking must not reorder the caller's list, which may be the map's own spawn data. */
  @Test
  void shouldLeaveTheCandidateListUnchanged() {
    List<SpawnPoint> candidates = manySpawnPoints(20);
    List<GridPoint2> before = positions(candidates);

    LootPlacement.pickRandomSpots(candidates, 8, new Random(SEED));

    assertEquals(before, positions(candidates), "the candidate list should not be shuffled");
  }

  @Test
  void shouldRejectMissingCandidates() {
    Random random = new Random(SEED);

    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.pickRandomSpots(null, 8, random),
        "picking spots without candidates should be rejected");
  }

  @Test
  void shouldRejectANegativeCount() {
    List<SpawnPoint> candidates = manySpawnPoints(5);
    Random random = new Random(SEED);

    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.pickRandomSpots(candidates, -1, random),
        "a negative number of spots should be rejected");
  }

  @Test
  void shouldRejectAMissingRandomSource() {
    List<SpawnPoint> candidates = manySpawnPoints(5);

    assertThrows(
        IllegalArgumentException.class,
        () -> LootPlacement.pickRandomSpots(candidates, 8, null),
        "picking spots without a random source should be rejected");
  }

  private List<GridPoint2> positions(List<SpawnPoint> spots) {
    List<GridPoint2> positions = new ArrayList<>();
    for (SpawnPoint spot : spots) {
      positions.add(spot.getPosition());
    }
    return positions;
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
