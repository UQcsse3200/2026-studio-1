package com.csse3200.game.perks.tortoises;

import static com.csse3200.game.perks.tortoises.TortoisePlanner.FIXED_TORTOISES;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.LEVEL_1_WEIGHT;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.LEVEL_2_WEIGHT;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.LEVEL_3_MAX;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.LEVEL_3_WEIGHT;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.LEVEL_WEIGHTS;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.MAX_PER_LEVEL;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.MAX_TOTAL;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.ROOMS;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.ROOM_WEIGHT;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.newGame;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.plan;
import static com.csse3200.game.perks.tortoises.TortoisePlanner.rollTotal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link TortoisePlanner}: the rolled total stays in range, the random tortoises are shared
 * between the maps within each map's limit, rooms are likelier than the high levels, and the same
 * seed always gives the same answer. Every test seeds its own {@link Random}.
 */
@ExtendWith(GameExtension.class)
class TortoisePlannerTest {
  // Taken from the planner itself so the dash characters in the map names cannot drift apart;
  // shouldMatchTheMapNamesInTheMapFiles checks the names against the real map files.
  private static final List<String> LEVEL_NAMES = new ArrayList<>(LEVEL_WEIGHTS.keySet());
  private static final String LEVEL_1 = LEVEL_NAMES.get(0);
  private static final String LEVEL_2 = LEVEL_NAMES.get(1);
  private static final String LEVEL_3 = LEVEL_NAMES.get(2);

  @BeforeEach
  void setUp() {
    TortoisePlan.reset();
  }

  @AfterEach
  void tearDown() {
    TortoisePlan.reset();
  }

  private static int sum(Map<String, Integer> counts) {
    return counts.values().stream().mapToInt(Integer::intValue).sum();
  }

  @Nested
  @DisplayName("rolling the total")
  class RollTotal {

    @Test
    void shouldAlwaysRollWithinTheTotalRange() {
      Random random = new Random(1);
      for (int i = 0; i < 5000; i++) {
        int total = rollTotal(random);
        assertTrue(total >= FIXED_TORTOISES && total <= MAX_TOTAL, "rolled " + total);
      }
    }

    @Test
    void shouldReachBothEndsOfTheRange() {
      Random random = new Random(2);
      Set<Integer> seen = new HashSet<>();
      for (int i = 0; i < 5000; i++) {
        seen.add(rollTotal(random));
      }

      assertTrue(seen.contains(FIXED_TORTOISES));
      assertTrue(seen.contains(MAX_TOTAL));
    }

    @Test
    void shouldRollTheSameTotalForTheSameSeed() {
      assertEquals(rollTotal(new Random(7)), rollTotal(new Random(7)));
    }
  }

  @Nested
  @DisplayName("sharing the random tortoises")
  class Plan {

    @Test
    void shouldRejectATotalBelowTheFixedCount() {
      Random random = new Random(1);

      assertThrows(
          IllegalArgumentException.class,
          () -> plan(FIXED_TORTOISES - 1, ROOMS, LEVEL_WEIGHTS, random));
    }

    @Test
    void shouldRejectATotalAboveTheMaximum() {
      Random random = new Random(1);

      assertThrows(
          IllegalArgumentException.class, () -> plan(MAX_TOTAL + 1, ROOMS, LEVEL_WEIGHTS, random));
    }

    @Test
    void shouldPlaceNothingWhenOnlyTheFixedOnesExist() {
      assertTrue(plan(FIXED_TORTOISES, ROOMS, LEVEL_WEIGHTS, new Random(1)).isEmpty());
    }

    @Test
    void shouldPlaceExactlyTheRandomShareForEveryTotal() {
      for (int total = FIXED_TORTOISES; total <= MAX_TOTAL; total++) {
        Map<String, Integer> counts = plan(total, ROOMS, LEVEL_WEIGHTS, new Random(total));

        assertEquals(total - FIXED_TORTOISES, sum(counts), "total " + total);
      }
    }

    @Test
    void shouldNeverGiveARoomMoreThanOne() {
      for (int seed = 1; seed <= 2000; seed++) {
        Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, LEVEL_WEIGHTS, new Random(seed));
        for (String room : ROOMS) {
          assertTrue(counts.getOrDefault(room, 0) <= 1, room + " seed " + seed);
        }
      }
    }

    @Test
    void shouldNeverGiveAMainLevelMoreThanTheLimit() {
      for (int seed = 1; seed <= 2000; seed++) {
        Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, LEVEL_WEIGHTS, new Random(seed));
        for (String level : LEVEL_WEIGHTS.keySet()) {
          assertTrue(counts.getOrDefault(level, 0) <= MAX_PER_LEVEL, level + " seed " + seed);
        }
      }
    }

    @Test
    void shouldNeverGiveLevelThreeMoreThanItsLimit() {
      assertEquals(2, LEVEL_3_MAX);
      for (int seed = 1; seed <= 2000; seed++) {
        Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, LEVEL_WEIGHTS, new Random(seed));

        assertTrue(counts.getOrDefault(LEVEL_3, 0) <= LEVEL_3_MAX, "seed " + seed);
      }
    }

    @Test
    void shouldFillALevelUpToTheLimitWhenItIsTheOnlyMap() {
      Map<String, Integer> counts =
          plan(MAX_TOTAL, List.of(), Map.of("Other level", 1f), new Random(1));

      assertEquals(Map.of("Other level", MAX_PER_LEVEL), counts);
    }

    @Test
    void shouldFillLevelThreeUpToItsOwnLimitWhenItIsTheOnlyMap() {
      Map<String, Integer> counts = plan(MAX_TOTAL, List.of(), Map.of(LEVEL_3, 1f), new Random(1));

      assertEquals(Map.of(LEVEL_3, LEVEL_3_MAX), counts);
    }

    @Test
    void shouldOnlyUseKnownMaps() {
      Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, LEVEL_WEIGHTS, new Random(3));

      for (String map : counts.keySet()) {
        assertTrue(ROOMS.contains(map) || LEVEL_WEIGHTS.containsKey(map), map);
      }
    }

    @Test
    void shouldFavourRoomsOverTheHighestLevel() {
      int roomHits = 0;
      int levelThreeHits = 0;
      for (int seed = 1; seed <= 5000; seed++) {
        Map<String, Integer> counts = plan(6, ROOMS, LEVEL_WEIGHTS, new Random(seed));
        for (String room : ROOMS) {
          roomHits += counts.getOrDefault(room, 0);
        }
        levelThreeHits += counts.getOrDefault(LEVEL_3, 0);
      }

      double roomRate = roomHits / (5000.0 * ROOMS.size());
      double levelThreeRate = levelThreeHits / (5000.0 * LEVEL_3_MAX);
      assertTrue(roomRate > levelThreeRate, roomRate + " vs " + levelThreeRate);
    }

    @Test
    void shouldFavourLowerLevelsOverHigherOnes() {
      int one = 0;
      int two = 0;
      int three = 0;
      for (int seed = 1; seed <= 5000; seed++) {
        Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, LEVEL_WEIGHTS, new Random(seed));
        one += counts.getOrDefault(LEVEL_1, 0);
        two += counts.getOrDefault(LEVEL_2, 0);
        three += counts.getOrDefault(LEVEL_3, 0);
      }

      assertTrue(one > two, one + " vs " + two);
      assertTrue(two > three, two + " vs " + three);
    }

    @Test
    void shouldGiveTheSamePlanForTheSameSeed() {
      assertEquals(
          plan(9, ROOMS, LEVEL_WEIGHTS, new Random(4)),
          plan(9, ROOMS, LEVEL_WEIGHTS, new Random(4)));
    }

    @Test
    void shouldPlaceEverySlotWhenTheTotalIsAsLargeAsTheSlotsAllow() {
      Map<String, Integer> counts = plan(MAX_TOTAL, ROOMS, Map.of(), new Random(1));

      assertEquals(ROOMS.size(), sum(counts));
    }
  }

  @Nested
  @DisplayName("new game, constants and structure")
  class NewGameAndConstants {

    @Test
    void shouldReturnTheRolledTotalAndStoreAPlanThatMatchesIt() {
      int total = newGame(new Random(11));

      assertTrue(total >= FIXED_TORTOISES && total <= MAX_TOTAL);
      assertEquals(total - FIXED_TORTOISES, sum(TortoisePlan.exportAll()));
    }

    @Test
    void shouldStoreTheSamePlanThatPlanWouldMake() {
      Random expectedRandom = new Random(12);
      int expectedTotal = rollTotal(expectedRandom);
      Map<String, Integer> expected = plan(expectedTotal, ROOMS, LEVEL_WEIGHTS, expectedRandom);

      int total = newGame(new Random(12));

      assertEquals(expectedTotal, total);
      assertEquals(expected, TortoisePlan.exportAll());
    }

    @Test
    void shouldReplaceAnEarlierPlan() {
      TortoisePlan.set(Map.of("Hound's Den", 1));
      Random expectedRandom = new Random(12);
      int expectedTotal = rollTotal(expectedRandom);
      Map<String, Integer> expected = plan(expectedTotal, ROOMS, LEVEL_WEIGHTS, expectedRandom);

      newGame(new Random(12));

      assertEquals(expected, TortoisePlan.exportAll());
    }

    @Test
    void shouldListEightDistinctRooms() {
      assertEquals(8, ROOMS.size());
      assertEquals(8, new HashSet<>(ROOMS).size());
    }

    @Test
    void shouldListThreeLevelsInOrder() {
      assertEquals(3, LEVEL_NAMES.size());
      assertEquals(3, new HashSet<>(LEVEL_NAMES).size());
      assertTrue(LEVEL_1.startsWith("Level 1"));
      assertTrue(LEVEL_2.startsWith("Level 2"));
      assertTrue(LEVEL_3.startsWith("Level 3"));
    }

    @Test
    void shouldWeightLevelsDownwards() {
      assertTrue(LEVEL_1_WEIGHT > LEVEL_2_WEIGHT);
      assertTrue(LEVEL_2_WEIGHT > LEVEL_3_WEIGHT);
      assertEquals(LEVEL_1_WEIGHT, LEVEL_WEIGHTS.get(LEVEL_1));
      assertEquals(LEVEL_2_WEIGHT, LEVEL_WEIGHTS.get(LEVEL_2));
      assertEquals(LEVEL_3_WEIGHT, LEVEL_WEIGHTS.get(LEVEL_3));
    }

    @Test
    void shouldWeightARoomAboveEveryLevel() {
      assertTrue(ROOM_WEIGHT > LEVEL_1_WEIGHT);
    }

    @Test
    void shouldMatchTheMapNamesInTheMapFiles() {
      Set<String> namesInFiles = new HashSet<>();
      for (FileHandle file : Gdx.files.internal("maps").list(".json")) {
        namesInFiles.add(new JsonReader().parse(file).getString("name", ""));
      }

      for (String name : ROOMS) {
        assertTrue(namesInFiles.contains(name), "no map file is named " + name);
      }
      for (String name : LEVEL_WEIGHTS.keySet()) {
        assertTrue(namesInFiles.contains(name), "no map file is named " + name);
      }
    }

    @Test
    void shouldNotBeInstantiable() throws ReflectiveOperationException {
      Constructor<TortoisePlanner> constructor = TortoisePlanner.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      InvocationTargetException thrown =
          assertThrows(InvocationTargetException.class, constructor::newInstance);

      assertInstanceOf(IllegalStateException.class, thrown.getCause());
    }
  }
}
