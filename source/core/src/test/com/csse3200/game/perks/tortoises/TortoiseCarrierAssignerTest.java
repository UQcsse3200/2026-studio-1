package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.EnemyType;
import com.csse3200.game.components.enemies.EnemyTypeComponent;
import com.csse3200.game.entities.Entity;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link TortoiseCarrierAssigner}: only easy enemies carry a tortoise, each planned tortoise
 * gets its own carrier, an already-found tortoise is skipped, and any tortoise without a carrier is
 * handed back for the caller to place on the ground.
 *
 * <p>Enemies are bare entities with an {@code EnemyTypeComponent} and are never created, as in the
 * real wiring where the assigner runs before the enemies are registered.
 */
@ExtendWith(GameExtension.class)
class TortoiseCarrierAssignerTest {
  private static final String MAP = "Hound's Den";

  @BeforeEach
  void setUp() {
    TortoiseTestSupport.assumePreferencesAvailable();
    TortoisePlan.reset();
    TortoiseFactory.resetAll();
  }

  @AfterEach
  void tearDown() {
    TortoisePlan.reset();
    TortoiseFactory.resetAll();
  }

  private static Entity enemy(EnemyType type) {
    return new Entity().addComponent(new EnemyTypeComponent(type));
  }

  private static List<Entity> skeletons(int count) {
    List<Entity> enemies = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      enemies.add(enemy(EnemyType.SKELETON));
    }
    return enemies;
  }

  private static long carriers(List<Entity> enemies) {
    return enemies.stream()
        .filter(e -> e.getComponent(TortoiseDropComponent.class) != null)
        .count();
  }

  private static String carriedId(Entity enemy) {
    return enemy.getComponent(TortoiseDropComponent.class).getTortoiseId();
  }

  // ---------- ids ----------

  @Test
  void shouldBuildStableIdsFromTheMapName() {
    assertEquals("rnd_hound_s_den_1", TortoiseCarrierAssigner.tortoiseId("Hound's Den", 1));
    assertEquals(
        "rnd_level_1_out_of_the_underworld_2",
        TortoiseCarrierAssigner.tortoiseId("Level 1 - Out of the Underworld", 2));
    assertEquals(
        "rnd_level_2_climb_mount_olympus_1",
        TortoiseCarrierAssigner.tortoiseId("Level 2 — Climb Mount Olympus", 1));
    assertEquals(
        "rnd_zeus_s_outer_guard_3", TortoiseCarrierAssigner.tortoiseId("Zeus's Outer Guard", 3));
  }

  @Test
  void shouldTrimPunctuationAtTheEndsOfTheName() {
    assertEquals("rnd_den_1", TortoiseCarrierAssigner.tortoiseId("  --Den!!  ", 1));
  }

  @Test
  void shouldGiveEveryMapAndSlotItsOwnId() {
    Set<String> ids = new HashSet<>();
    int expected = 0;
    for (String room : TortoisePlanner.ROOMS) {
      ids.add(TortoiseCarrierAssigner.tortoiseId(room, 1));
      expected++;
    }
    for (String level : TortoisePlanner.LEVEL_WEIGHTS.keySet()) {
      for (int slot = 1; slot <= TortoisePlanner.MAX_PER_LEVEL; slot++) {
        ids.add(TortoiseCarrierAssigner.tortoiseId(level, slot));
        expected++;
      }
    }

    assertEquals(expected, ids.size());
  }

  // ---------- eligibility ----------

  @ParameterizedTest
  @EnumSource(
      value = EnemyType.class,
      names = {"SKELETON", "RANGED_SKELETON", "HARPY", "RANGED_HARPY"})
  void shouldTreatEasyEnemiesAsEligible(EnemyType type) {
    assertTrue(TortoiseCarrierAssigner.isEligible(enemy(type)));
  }

  @ParameterizedTest
  @EnumSource(
      value = EnemyType.class,
      names = {"MEDUSA", "CERBERUS", "ZEUS", "MINOTAUR", "CENTAUR", "CYCLOPS"})
  void shouldNotTreatBossesAsEligible(EnemyType type) {
    assertFalse(TortoiseCarrierAssigner.isEligible(enemy(type)));
  }

  @Test
  void shouldNotTreatAnEntityWithoutATypeAsEligible() {
    assertFalse(TortoiseCarrierAssigner.isEligible(new Entity()));
  }

  @Test
  void shouldNotTreatAnEnemyThatAlreadyCarriesOneAsEligible() {
    Entity carrier = enemy(EnemyType.SKELETON).addComponent(new TortoiseDropComponent("taken"));

    assertFalse(TortoiseCarrierAssigner.isEligible(carrier));
  }

  // ---------- assigning ----------

  @Test
  void shouldAssignNothingWhenTheMapHasNoPlannedTortoises() {
    List<Entity> enemies = skeletons(3);

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertTrue(needGround.isEmpty());
    assertEquals(0, carriers(enemies));
  }

  @Test
  void shouldGiveEachPlannedTortoiseItsOwnCarrier() {
    TortoisePlan.set(Map.of(MAP, 3));
    List<Entity> enemies = skeletons(5);

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertTrue(needGround.isEmpty());
    assertEquals(3, carriers(enemies));
    Set<String> ids = new HashSet<>();
    for (Entity e : enemies) {
      if (e.getComponent(TortoiseDropComponent.class) != null) {
        ids.add(carriedId(e));
      }
    }
    assertEquals(
        Set.of(
            TortoiseCarrierAssigner.tortoiseId(MAP, 1),
            TortoiseCarrierAssigner.tortoiseId(MAP, 2),
            TortoiseCarrierAssigner.tortoiseId(MAP, 3)),
        ids);
  }

  @Test
  void shouldHandBackTortoisesThatGotNoCarrier() {
    TortoisePlan.set(Map.of(MAP, 3));
    List<Entity> enemies = skeletons(1);

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertEquals(1, carriers(enemies));
    assertEquals(
        List.of(
            TortoiseCarrierAssigner.tortoiseId(MAP, 2), TortoiseCarrierAssigner.tortoiseId(MAP, 3)),
        needGround);
  }

  @Test
  void shouldHandBackEveryTortoiseWhenNoEnemyCanCarryOne() {
    TortoisePlan.set(Map.of(MAP, 2));
    List<Entity> enemies = List.of(enemy(EnemyType.ZEUS), enemy(EnemyType.MEDUSA));

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertEquals(2, needGround.size());
    assertEquals(0, carriers(enemies));
  }

  @Test
  void shouldHandBackEveryTortoiseWhenThereAreNoEnemies() {
    TortoisePlan.set(Map.of(MAP, 2));

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, List.of(), new Random(1));

    assertEquals(2, needGround.size());
  }

  @Test
  void shouldSkipATortoiseThatWasAlreadyFound() {
    TortoisePlan.set(Map.of(MAP, 2));
    TortoiseRegistry.markFound(TortoiseCarrierAssigner.tortoiseId(MAP, 1));
    List<Entity> enemies = skeletons(3);

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertTrue(needGround.isEmpty());
    assertEquals(1, carriers(enemies));
    for (Entity e : enemies) {
      if (e.getComponent(TortoiseDropComponent.class) != null) {
        assertEquals(TortoiseCarrierAssigner.tortoiseId(MAP, 2), carriedId(e));
      }
    }
  }

  @Test
  void shouldNotHandBackAFoundTortoiseEvenWithoutCarriers() {
    TortoisePlan.set(Map.of(MAP, 1));
    TortoiseRegistry.markFound(TortoiseCarrierAssigner.tortoiseId(MAP, 1));

    assertTrue(TortoiseCarrierAssigner.assign(MAP, List.of(), new Random(1)).isEmpty());
  }

  @Test
  void shouldNotChooseAnEnemyThatAlreadyCarriesOne() {
    TortoisePlan.set(Map.of(MAP, 1));
    Entity busy = enemy(EnemyType.SKELETON).addComponent(new TortoiseDropComponent("taken"));
    Entity free = enemy(EnemyType.HARPY);

    TortoiseCarrierAssigner.assign(MAP, List.of(busy, free), new Random(1));

    assertEquals("taken", carriedId(busy));
    assertNotNull(free.getComponent(TortoiseDropComponent.class));
  }

  @Test
  void shouldOnlyPlanForTheMapItWasAskedAbout() {
    TortoisePlan.set(Map.of("Cyclops Forge", 2));
    List<Entity> enemies = skeletons(3);

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, enemies, new Random(1));

    assertTrue(needGround.isEmpty());
    assertEquals(0, carriers(enemies));
  }

  @Test
  void shouldHandBackATortoiseWhenTheCarrierWasAlreadyCreated() {
    TortoisePlan.set(Map.of(MAP, 1));
    Entity created = enemy(EnemyType.SKELETON);
    created.create(); // Entity.addComponent ignores a component added after this

    List<String> needGround = TortoiseCarrierAssigner.assign(MAP, List.of(created), new Random(1));

    assertNull(created.getComponent(TortoiseDropComponent.class));
    assertEquals(List.of(TortoiseCarrierAssigner.tortoiseId(MAP, 1)), needGround);
  }

  @Test
  void shouldPickDifferentCarriersForDifferentSeeds() {
    TortoisePlan.set(Map.of(MAP, 1));
    Set<Integer> chosen = new HashSet<>();
    for (int seed = 1; seed <= 40; seed++) {
      List<Entity> enemies = skeletons(5);
      TortoiseCarrierAssigner.assign(MAP, enemies, new Random(seed));
      for (int i = 0; i < enemies.size(); i++) {
        if (enemies.get(i).getComponent(TortoiseDropComponent.class) != null) {
          chosen.add(i);
        }
      }
    }

    assertTrue(chosen.size() > 1, "the first enemies must not always be chosen");
  }

  @Test
  void shouldPickTheSameCarrierForTheSameSeed() {
    TortoisePlan.set(Map.of(MAP, 1));
    List<Entity> first = skeletons(5);
    List<Entity> second = skeletons(5);

    TortoiseCarrierAssigner.assign(MAP, first, new Random(9));
    TortoiseCarrierAssigner.assign(MAP, second, new Random(9));

    for (int i = 0; i < first.size(); i++) {
      assertEquals(
          first.get(i).getComponent(TortoiseDropComponent.class) == null,
          second.get(i).getComponent(TortoiseDropComponent.class) == null);
    }
  }

  // ---------- structure ----------

  @Test
  void shouldNotBeInstantiable() throws ReflectiveOperationException {
    Constructor<TortoiseCarrierAssigner> constructor =
        TortoiseCarrierAssigner.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(InvocationTargetException.class, constructor::newInstance);

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
  }
}
