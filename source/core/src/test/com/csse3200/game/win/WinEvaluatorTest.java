package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.spawn.EnemyRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link WinEvaluator}, the win class: which of three tiers a player has earned.
 *
 * <p>The rules under test, in order: no final boss means no win; the final boss alone is Victory;
 * Victory plus every required mini boss is Glory; Glory plus enough quests is Legend. Quests never
 * lift a Victory.
 *
 * <p>The pure {@code evaluate} method takes all its inputs, so most tests need no game. The last
 * group tests {@code evaluateNow}, which reads the static {@code EnemyRegistry} and {@code
 * QuestLedger}; those two are reset around every test.
 *
 * <p><b>Assumes</b> {@code QUESTS_REQUIRED_FOR_LEGEND} is at least 1 (the default is 3). If you set
 * it to 0 so Legend does not need quests, delete the quest-threshold tests and keep the rest.
 */
class WinEvaluatorTest {
  private static final int QUESTS = WinEvaluator.QUESTS_REQUIRED_FOR_LEGEND;

  @BeforeEach
  void setUp() {
    QuestLedger.reset();
    EnemyRegistry.loadFrom(new ArrayList<>());
  }

  @AfterEach
  void tearDown() {
    QuestLedger.reset();
    EnemyRegistry.loadFrom(new ArrayList<>());
  }

  // ---------- no win ----------

  @Test
  void shouldNotWinWhenTheFinalBossIsAlive() {
    WinResult r = WinEvaluator.evaluate(false, allRequired(), QUESTS);

    assertEquals(WinTier.NONE, r.getTier());
    assertFalse(r.isFinalBossDefeated());
  }

  @Test
  void shouldNotWinByKillingEveryMiniBossAndFinishingEveryQuest() {
    // The design rule: mini bosses scale a win, they are never an alternative way to win.
    WinResult r = WinEvaluator.evaluate(false, allRequired(), 100);

    assertEquals(WinTier.NONE, r.getTier());
    assertEquals(
      BossRoster.getRequiredCount(), r.getMiniBossesDefeated(), "progress is still counted");
  }

  @Test
  void shouldNotWinWithNothingDone() {
    WinResult r = WinEvaluator.evaluate(false, List.of(), 0);

    assertEquals(WinTier.NONE, r.getTier());
    assertEquals(0, r.getMiniBossesDefeated());
    assertEquals(BossRoster.getRequiredCount(), r.getMissingMiniBosses().size());
  }

  // ---------- Victory ----------

  @Test
  void shouldGiveVictoryForTheFinalBossAlone() {
    WinResult r = WinEvaluator.evaluate(true, List.of(), 0);

    assertEquals(WinTier.VICTORY, r.getTier());
    assertTrue(r.isFinalBossDefeated());
    assertFalse(r.isFullyComplete());
  }

  @Test
  void shouldListEveryRequiredMiniBossAsMissingOnAPlainVictory() {
    WinResult r = WinEvaluator.evaluate(true, List.of(), 0);

    assertEquals(0, r.getMiniBossesDefeated());
    assertEquals(8, r.getMiniBossesRequired());
    assertEquals(8, r.getMissingMiniBosses().size());
  }

  @Test
  void shouldStayAtVictoryWhenOneRequiredMiniBossIsStillAlive() {
    List<String> kills = allRequired();
    String spared = kills.remove(3);

    WinResult r = WinEvaluator.evaluate(true, kills, QUESTS);

    assertEquals(
      WinTier.VICTORY, r.getTier(), "all quests done, but a guardian lives: only Victory");
    assertEquals(7, r.getMiniBossesDefeated());
    assertEquals(1, r.getMissingMiniBosses().size());
    assertTrue(labelsOf(spared).contains(r.getMissingMiniBosses().get(0)));
  }

  @ParameterizedTest(name = "{0} of 8 required defeated is still only Victory")
  @ValueSource(ints = {0, 1, 4, 7})
  void shouldStayAtVictoryUntilEveryRequiredMiniBossIsDefeated(int defeated) {
    List<String> kills = allRequired().subList(0, defeated);

    WinResult r = WinEvaluator.evaluate(true, kills, QUESTS);

    assertEquals(WinTier.VICTORY, r.getTier());
    assertEquals(defeated, r.getMiniBossesDefeated());
    assertEquals(8 - defeated, r.getMissingMiniBosses().size());
  }

  // ---------- Glory ----------

  @Test
  void shouldGiveGloryForEveryRequiredMiniBossWithNoQuests() {
    WinResult r = WinEvaluator.evaluate(true, allRequired(), 0);

    assertEquals(WinTier.GLORY, r.getTier());
    assertEquals(8, r.getMiniBossesDefeated());
    assertTrue(r.getMissingMiniBosses().isEmpty());
  }

  @Test
  void shouldStayAtGloryWhenOneQuestShortOfLegend() {
    WinResult r = WinEvaluator.evaluate(true, allRequired(), QUESTS - 1);

    assertEquals(WinTier.GLORY, r.getTier());
    assertEquals(QUESTS - 1, r.getQuestsCompleted());
  }

  @Test
  void shouldNotNeedTheEscortsForGlory() {
    // Only the required bosses are in the kill list; the three escorts are alive.
    List<String> kills = allRequired();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      assertFalse(boss.isRequired() && !kills.contains(boss.getId()));
      if (!boss.isRequired()) {
        assertFalse(kills.contains(boss.getId()), "the escorts are not in this kill list");
      }
    }

    assertEquals(WinTier.GLORY, WinEvaluator.evaluate(true, kills, 0).getTier());
  }

  @Test
  void shouldNotCountTheEscortsTowardsTheRequiredBosses() {
    List<String> kills = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (!boss.isRequired()) {
        kills.add(boss.getId());
      }
    }

    WinResult r = WinEvaluator.evaluate(true, kills, QUESTS);

    assertEquals(WinTier.VICTORY, r.getTier());
    assertEquals(0, r.getMiniBossesDefeated());
  }

  // ---------- Legend ----------

  @Test
  void shouldGiveLegendForGloryPlusEnoughQuests() {
    WinResult r = WinEvaluator.evaluate(true, allRequired(), QUESTS);

    assertEquals(WinTier.LEGEND, r.getTier());
    assertTrue(r.isFullyComplete());
  }

  @Test
  void shouldGiveLegendForMoreQuestsThanNeeded() {
    assertEquals(WinTier.LEGEND, WinEvaluator.evaluate(true, allRequired(), QUESTS + 10).getTier());
  }

  @Test
  void shouldNeverLiftAVictoryToLegendOnQuestsAlone() {
    WinResult r = WinEvaluator.evaluate(true, List.of(), QUESTS + 10);

    assertEquals(WinTier.VICTORY, r.getTier(), "quests need Glory behind them");
  }

  @ParameterizedTest(name = "{0} quests: tier {1}")
  @CsvSource({"0, GLORY", "1, GLORY", "2, GLORY"})
  void shouldGiveGloryBelowTheQuestThreshold(int quests, WinTier expected) {
    // Written for the default threshold of 3; the first assertion makes that assumption explicit.
    assertEquals(
      3, WinEvaluator.QUESTS_REQUIRED_FOR_LEGEND, "update this table if the threshold changes");

    assertEquals(expected, WinEvaluator.evaluate(true, allRequired(), quests).getTier());
  }

  @Test
  void shouldReportTheQuestThresholdInTheResult() {
    WinResult r = WinEvaluator.evaluate(true, allRequired(), 1);

    assertEquals(WinEvaluator.QUESTS_REQUIRED_FOR_LEGEND, r.getQuestsRequired());
    assertEquals(1, r.getQuestsCompleted());
  }

  // ---------- inputs ----------

  @Test
  void shouldTreatANullKillListAsEmpty() {
    WinResult r = assertDoesNotThrowValue(() -> WinEvaluator.evaluate(true, null, 0));

    assertEquals(WinTier.VICTORY, r.getTier());
    assertEquals(0, r.getMiniBossesDefeated());
  }

  @Test
  void shouldIgnoreDuplicateKillIds() {
    List<String> kills = allRequired();
    kills.addAll(allRequired());
    kills.addAll(allRequired());

    WinResult r = WinEvaluator.evaluate(true, kills, 0);

    assertEquals(WinTier.GLORY, r.getTier());
    assertEquals(8, r.getMiniBossesDefeated(), "three copies of each id still count once");
  }

  @Test
  void shouldIgnoreKillIdsThatAreNotInTheRoster() {
    List<String> kills = allRequired();
    kills.add("Level 1 - Out of the Underworld:23,3");
    kills.add("some other map:1,1");
    kills.add("");

    WinResult r = WinEvaluator.evaluate(true, kills, 0);

    assertEquals(WinTier.GLORY, r.getTier());
    assertEquals(8, r.getMiniBossesDefeated());
  }

  @Test
  void shouldIgnoreANullEntryInsideTheKillList() {
    List<String> kills = allRequired();
    kills.add(null);

    assertDoesNotThrow(() -> WinEvaluator.evaluate(true, kills, 0));
  }

  @Test
  void shouldMatchIdsExactlyIncludingCase() {
    List<String> kills = new ArrayList<>();
    for (String id : allRequired()) {
      kills.add(id.toUpperCase());
    }

    assertEquals(0, WinEvaluator.evaluate(true, kills, 0).getMiniBossesDefeated());
  }

  @Test
  void shouldAcceptAnyCollectionOfKills() {
    Collection<String> asSet = new HashSet<>(allRequired());

    assertEquals(WinTier.GLORY, WinEvaluator.evaluate(true, asSet, 0).getTier());
  }

  @Test
  void shouldNotCountTheFinalBossIdAsAMiniBoss() {
    List<String> kills = new ArrayList<>();
    kills.add(BossRoster.FINAL_BOSS_ID);

    WinResult r = WinEvaluator.evaluate(true, kills, 0);

    assertEquals(0, r.getMiniBossesDefeated());
  }

  @ParameterizedTest(name = "{0} quests is rejected")
  @ValueSource(ints = {-1, -5, Integer.MIN_VALUE})
  void shouldRejectANegativeQuestCount(int quests) {
    assertThrows(
      IllegalArgumentException.class, () -> WinEvaluator.evaluate(true, allRequired(), quests));
  }

  @Test
  void shouldNotChangeTheKillListItWasGiven() {
    List<String> kills = allRequired();
    List<String> before = new ArrayList<>(kills);

    WinEvaluator.evaluate(true, kills, QUESTS);

    assertEquals(before, kills);
  }

  // ---------- the missing list ----------

  @Test
  void shouldListMissingBossesInRosterOrder() {
    List<String> requiredInOrder = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (boss.isRequired()) {
        requiredInOrder.add(boss.getLabel());
      }
    }

    WinResult r = WinEvaluator.evaluate(true, List.of(), 0);

    assertEquals(requiredInOrder, r.getMissingMiniBosses());
  }

  @Test
  void shouldListOnlyTheBossesThatAreStillAlive() {
    List<String> kills = allRequired();
    String first = kills.remove(0);
    String last = kills.remove(kills.size() - 1);

    WinResult r = WinEvaluator.evaluate(true, kills, 0);

    assertEquals(2, r.getMissingMiniBosses().size());
    assertTrue(r.getMissingMiniBosses().contains(labelsOf(first).get(0)));
    assertTrue(r.getMissingMiniBosses().contains(labelsOf(last).get(0)));
  }

  @Test
  void shouldKeepTheCountsConsistentWithTheMissingList() {
    for (int defeated = 0; defeated <= 8; defeated++) {
      WinResult r = WinEvaluator.evaluate(true, allRequired().subList(0, defeated), 0);

      assertEquals(
        r.getMiniBossesRequired() - r.getMiniBossesDefeated(), r.getMissingMiniBosses().size());
    }
  }

  // ---------- evaluateNow reads the live registries ----------

  @Test
  void shouldReadKillsFromTheEnemyRegistryWhenEvaluatingNow() {
    EnemyRegistry.loadFrom(allRequired());

    WinResult r = WinEvaluator.evaluateNow(true);

    assertEquals(WinTier.GLORY, r.getTier());
  }

  @Test
  void shouldReadCompletedQuestsFromTheLedgerWhenEvaluatingNow() {
    EnemyRegistry.loadFrom(allRequired());
    for (int i = 0; i < QUESTS; i++) {
      QuestLedger.recordCompleted(QuestLedger.JUMP);
    }

    assertEquals(WinTier.LEGEND, WinEvaluator.evaluateNow(true).getTier());
  }

  @Test
  void shouldGiveNoWinWhenEvaluatingNowWithTheFinalBossAlive() {
    EnemyRegistry.loadFrom(allRequired());

    assertEquals(WinTier.NONE, WinEvaluator.evaluateNow(false).getTier());
  }

  @Test
  void shouldGiveVictoryWhenEvaluatingNowWithAnEmptyGame() {
    assertEquals(WinTier.VICTORY, WinEvaluator.evaluateNow(true).getTier());
  }

  @Test
  void shouldNeedNoFinalBossIdInTheRegistryBecauseTheCallerSaysHeFell() {
    // Zeus's own id is marked killed only when his entity is disposed, just after he dies, so the
    // evaluator must trust the flag and not look for the id.
    assertFalse(EnemyRegistry.isKilled(BossRoster.FINAL_BOSS_ID));

    assertEquals(WinTier.VICTORY, WinEvaluator.evaluateNow(true).getTier());
  }

  // ---------- helpers ----------

  /** The ids of the eight required mini bosses, in roster order, as a list the test can edit. */
  private static List<String> allRequired() {
    List<String> ids = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (boss.isRequired()) {
        ids.add(boss.getId());
      }
    }
    return ids;
  }

  private static List<String> labelsOf(String... ids) {
    Set<String> wanted = new HashSet<>(Arrays.asList(ids));
    List<String> labels = new ArrayList<>();
    for (BossRoster.MiniBoss boss : BossRoster.getMiniBosses()) {
      if (wanted.contains(boss.getId())) {
        labels.add(boss.getLabel());
      }
    }
    return labels;
  }

  private static WinResult assertDoesNotThrowValue(
    java.util.function.Supplier<WinResult> supplier) {
    return assertDoesNotThrow(supplier::get);
  }
}
