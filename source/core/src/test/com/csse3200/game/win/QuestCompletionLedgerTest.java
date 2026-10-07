package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.files.GameSaveData;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the link between the quest system and the win screen: a quest turned in through {@link
 * QuestGiverComponent} is counted in {@link QuestLedger} only when it was really finished, and the
 * save file has somewhere to keep that count.
 *
 * <p>Each of the four quest kinds is run through the same three cases from one table. A row says
 * how to start the quest with a target of one, how to make one unit of progress, how to turn it in
 * and which ledger kind it should land under. The quest giver has a real player with an inventory,
 * because a finished quest cannot be turned in without someone to reward.
 */
class QuestCompletionLedgerTest {

  /** One quest kind: how to start it, advance it, turn it in, and where it is counted. */
  private record Kind(
      String ledgerKind,
      Consumer<QuestGiverComponent> start,
      Runnable progress,
      Function<QuestGiverComponent, Boolean> turnIn) {
    @Override
    public String toString() {
      return ledgerKind;
    }
  }

  private static Stream<Arguments> kinds() {
    return Stream.of(
        Arguments.of(
            new Kind(
                QuestLedger.JUMP,
                giver -> giver.logJumpQuest(1),
                Quest::incrementGlobalJumps,
                QuestGiverComponent::clearJumpQuest)),
        Arguments.of(
            new Kind(
                QuestLedger.ENEMIES_KILLED,
                giver -> giver.logEnemiesKilledQuest(1),
                Quest::incrementGlobalEnemiesKilled,
                QuestGiverComponent::clearEnemiesKilledQuest)),
        Arguments.of(
            new Kind(
                QuestLedger.GOLD_SPENT,
                giver -> giver.logGoldSpentQuest(1),
                () -> Quest.addGlobalGoldSpent(1),
                QuestGiverComponent::clearGoldSpentQuest)),
        Arguments.of(
            new Kind(
                QuestLedger.SHIELDS_COLLECTED,
                giver -> giver.logShieldsCollectedQuest(1),
                Quest::incrementGlobalShieldsCollected,
                QuestGiverComponent::clearShieldsCollectedQuest)));
  }

  @BeforeEach
  void setUp() {
    QuestLedger.reset();
  }

  @AfterEach
  void tearDown() {
    QuestLedger.reset();
  }

  // ---------- one row per quest kind ----------

  @ParameterizedTest(name = "a finished {0} quest is counted once")
  @MethodSource("kinds")
  void shouldCountAFinishedQuestOnceWhenItIsTurnedIn(Kind kind) {
    QuestGiverComponent giver = newGiver();
    kind.start().accept(giver);
    kind.progress().run();

    assertTrue(kind.turnIn().apply(giver), "a finished quest can be turned in");

    assertEquals(1, QuestLedger.getCompleted(kind.ledgerKind()));
    assertEquals(1, QuestLedger.getCompletedCount(), "and under no other kind");
  }

  @ParameterizedTest(name = "an abandoned {0} quest is not counted")
  @MethodSource("kinds")
  void shouldNotCountAQuestThatIsClearedBeforeItIsFinished(Kind kind) {
    QuestGiverComponent giver = newGiver();
    kind.start().accept(giver);

    assertTrue(kind.turnIn().apply(giver), "an unfinished quest can still be cleared");

    assertEquals(0, QuestLedger.getCompletedCount(), "but abandoning it is not completing it");
  }

  @ParameterizedTest(name = "turning in a {0} quest that was never given counts nothing")
  @MethodSource("kinds")
  void shouldNotCountAnythingWhenThereIsNoQuestToTurnIn(Kind kind) {
    QuestGiverComponent giver = newGiver();

    kind.turnIn().apply(giver);

    assertEquals(0, QuestLedger.getCompletedCount());
  }

  @ParameterizedTest(name = "the same {0} quest finished twice counts twice")
  @MethodSource("kinds")
  void shouldCountTheSameKindAgainWhenItIsFinishedAgain(Kind kind) {
    QuestGiverComponent giver = newGiver();
    for (int round = 0; round < 2; round++) {
      kind.start().accept(giver);
      kind.progress().run();
      kind.turnIn().apply(giver);
    }

    assertEquals(2, QuestLedger.getCompleted(kind.ledgerKind()));
  }

  @ParameterizedTest(name = "a finished {0} quest whose reward fails is not counted")
  @MethodSource("kinds")
  void shouldNotCountAFinishedQuestWhoseRewardCouldNotBeGiven(Kind kind) {
    // With no player there is nobody to reward, so the turn-in fails and the quest stays open.
    QuestGiverComponent giver = new QuestGiverComponent(null, 0);
    new Entity().addComponent(giver);
    kind.start().accept(giver);
    kind.progress().run();

    assertFalse(kind.turnIn().apply(giver), "the turn-in is refused");

    assertEquals(0, QuestLedger.getCompletedCount(), "a refused turn-in completes nothing");
  }

  // ---------- all together ----------

  @Test
  void shouldAddUpDifferentKindsFinishedByDifferentQuestGivers() {
    kinds()
        .forEach(
            arguments -> {
              Kind kind = (Kind) arguments.get()[0];
              QuestGiverComponent giver = newGiver();
              kind.start().accept(giver);
              kind.progress().run();
              kind.turnIn().apply(giver);
            });

    assertEquals(4, QuestLedger.getCompletedCount());
    assertEquals(4, QuestLedger.getCompletedKinds().size());
  }

  // ---------- the save file ----------

  @Test
  void shouldGiveANewSaveSomewhereEmptyToKeepQuestsAndTortoises() {
    GameSaveData data = new GameSaveData();

    assertTrue(data.completedQuestsByKind.isEmpty());
    assertTrue(data.foundTortoiseIds.isEmpty());
  }

  @Test
  void shouldCarryTheLedgerThroughASaveAndBack() {
    QuestLedger.recordCompleted(QuestLedger.JUMP);
    QuestLedger.recordCompleted(QuestLedger.GOLD_SPENT);
    GameSaveData data = new GameSaveData();
    data.completedQuestsByKind = QuestLedger.exportAll();
    QuestLedger.reset();

    QuestLedger.loadFrom(data.completedQuestsByKind);

    assertEquals(2, QuestLedger.getCompletedCount());
  }

  /** A quest giver with a player to reward, built the way the existing quest tests build one. */
  private static QuestGiverComponent newGiver() {
    Entity player = new Entity().addComponent(new InventoryComponent(0));
    QuestGiverComponent giver = new QuestGiverComponent(player, 0);
    new Entity().addComponent(giver);
    return giver;
  }
}
