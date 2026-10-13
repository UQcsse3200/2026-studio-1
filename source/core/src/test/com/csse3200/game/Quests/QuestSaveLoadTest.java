package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class QuestSaveLoadTest {

  @BeforeEach
  void setUp() {
    Quest.resetAll();
  }

  @AfterEach
  void tearDown() {
    Quest.resetAll();
  }

  @Test
  void resetAllClearsIdsQuestsAndCounters() {
    int id = Quest.giveOutUniqueNPCID();
    Quest.logJumpQuest(id, 5);
    Quest.incrementGlobalJumps();

    Quest.resetAll();

    assertTrue(Quest.getUniqueNPCIDArrayList().isEmpty());
    assertTrue(Quest.getJumpQuests().isEmpty());
    assertTrue(Quest.getQuestActiveForNPCID().isEmpty());
    assertEquals(0f, Quest.getGlobalJumps());
    assertFalse(Quest.isValidNPCID(id));
  }

  @Test
  void countersExportAndRestore() {
    Quest.restoreCounters(3, 4, 50, 2);
    assertArrayEquals(new int[] {3, 4, 50, 2}, Quest.exportCounters());
  }

  @Test
  void negativeCountersBecomeZero() {
    Quest.restoreCounters(-1, -2, -3, -4);
    assertArrayEquals(new int[] {0, 0, 0, 0}, Quest.exportCounters());
  }

  @Test
  void exportQuestIsNullWithoutQuestOrValidId() {
    int id = Quest.giveOutUniqueNPCID();
    assertNull(Quest.exportQuest(id));
    assertNull(Quest.exportQuest(-1));
    assertNull(Quest.exportQuest(id + 1));
  }

  @Test
  void exportQuestKeepsTypeTargetAndSnapshot() {
    Quest.restoreCounters(7, 0, 0, 0);
    int id = Quest.giveOutUniqueNPCID();
    Quest.logJumpQuest(id, 10);

    Quest.ActiveQuest quest = Quest.exportQuest(id);

    assertEquals(Quest.JUMP_QUEST, quest.type());
    assertEquals(10, quest.amountToDo());
    assertEquals(7f, quest.snapshot());
  }

  @Test
  void restoredJumpQuestKeepsProgress() {
    Quest.restoreCounters(10, 0, 0, 0);
    int id = Quest.giveOutUniqueNPCID();

    assertTrue(Quest.restoreQuest(id, Quest.JUMP_QUEST, 10, 5f));
    assertEquals(50, Quest.checkJumpQuestComplete(id));
    assertTrue(Quest.getQuestActiveForNPCID().get(id));
  }

  @Test
  void restoredEnemiesQuestKeepsProgress() {
    Quest.restoreCounters(0, 3, 0, 0);
    int id = Quest.giveOutUniqueNPCID();

    assertTrue(Quest.restoreQuest(id, Quest.ENEMIES_QUEST, 4, 0f));
    assertEquals(75, Quest.checkEnemiesKilledQuest(id));
  }

  @Test
  void restoredGoldSpentQuestKeepsProgress() {
    Quest.restoreCounters(0, 0, 30, 0);
    int id = Quest.giveOutUniqueNPCID();

    assertTrue(Quest.restoreQuest(id, Quest.GOLD_SPENT_QUEST, 60, 0f));
    assertEquals(50, Quest.checkGoldSpentQuest(id));
  }

  @Test
  void restoredShieldsQuestKeepsProgress() {
    Quest.restoreCounters(0, 0, 0, 1);
    int id = Quest.giveOutUniqueNPCID();

    assertTrue(Quest.restoreQuest(id, Quest.SHIELDS_COLLECTED_QUEST, 2, 0f));
    assertEquals(50, Quest.checkShieldsCollectedQuest(id));
  }

  @Test
  void restoreQuestRejectsBadInputWithoutChangingAnything() {
    int id = Quest.giveOutUniqueNPCID();

    assertFalse(Quest.restoreQuest(-1, Quest.JUMP_QUEST, 5, 0f));
    assertFalse(Quest.restoreQuest(id + 1, Quest.JUMP_QUEST, 5, 0f));
    assertFalse(Quest.restoreQuest(id, null, 5, 0f));
    assertFalse(Quest.restoreQuest(id, "unknownquest", 5, 0f));
    assertFalse(Quest.restoreQuest(id, Quest.JUMP_QUEST, 0, 0f));
    assertFalse(Quest.getQuestActiveForNPCID().get(id));
    assertNull(Quest.exportQuest(id));
  }

  @Test
  void restoreQuestRefusesWhenNpcAlreadyHasQuest() {
    int id = Quest.giveOutUniqueNPCID();
    assertTrue(Quest.restoreQuest(id, Quest.JUMP_QUEST, 5, 0f));

    assertFalse(Quest.restoreQuest(id, Quest.GOLD_SPENT_QUEST, 5, 0f));
    assertEquals(Quest.JUMP_QUEST, Quest.exportQuest(id).type());
  }
}
