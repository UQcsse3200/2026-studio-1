package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GoldSpentQuestTest {
  static Entity entity;
  static QuestGiverComponent questGiverComponent;

  @BeforeAll
  public static void createAQuestEntity() {
    entity = new Entity();
    questGiverComponent = new QuestGiverComponent(null, 0);
    entity.addComponent(questGiverComponent);
  }

  @Test
  public void testIfCompletingAGoldSpentQuestWorks() {
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since no quest was created "
              + "for that entity");
    }
    // Creates the quest in the conditional
    if (Quest.getGoldSpentQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks did not return null"
              + "when it checked the goldSpentQuestTracker even though no "
              + "quest was created");
    }
    if (!questGiverComponent.logGoldSpentQuest(10)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because "
              + "logGoldSpentQuest returned false when it shouldn't have");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID() "
              + "returned false when it should have returned true");
    }
    questGiverComponent.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since the quest was cleared "
              + "for that entity");
    }
  }

  @Test
  public void testMultipleSettingOfGoldSpentQuest() {
    Entity entity1 = new Entity();
    Entity entity2 = new Entity();
    Entity entity3 = new Entity();
    QuestGiverComponent questGiverComponent1 = new QuestGiverComponent(null, 0);
    QuestGiverComponent questGiverComponent2 = new QuestGiverComponent(null, 0);
    QuestGiverComponent questGiverComponent3 = new QuestGiverComponent(null, 0);
    entity1.addComponent(questGiverComponent1);
    entity2.addComponent(questGiverComponent2);
    entity3.addComponent(questGiverComponent3);
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "Quest.getQuestActiveForNPCID returned true even though no "
              + "quest was created for that NPC");
    }
    if (!questGiverComponent1.logGoldSpentQuest(5)
        || !questGiverComponent2.logGoldSpentQuest(6)
        || !questGiverComponent3.logGoldSpentQuest(7)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "logGoldSpentQuest returned false when it should have returned "
              + "true");
    }
    if (questGiverComponent3.logGoldSpentQuest(6)) {
      fail(
          "The test testMultipleSettingOfGoldSpentQuest failed because "
              + "Quest.logGoldSpentQuest returned true when it shouldn't have "
              + "because a quest was already set");
    }
    assertEquals(
        0,
        questGiverComponent1.checkGoldSpentQuestComplete(),
        "checkGoldSpentQuestComplete() returned a value other than 0 even "
            + "though no progress was made");
    questGiverComponent1.clearGoldSpentQuest();
    questGiverComponent2.clearGoldSpentQuest();
    questGiverComponent3.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned true when it should have returned "
              + "false since the quest was cleared");
    }
    // Logging the EnemiesKilledQuest
    if (!questGiverComponent1.logGoldSpentQuest(9)) {
      fail(
          "questGiverComponent1.logGoldSpentQuest returned false when it should "
              + "have returned true");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned false when it should have "
              + "returned true since a quest was logged");
    }
    if (Quest.getGoldSpentQuests().get(questGiverComponent1.uniqueNPCID) == null) {
      fail(
          "Quest.getGoldSpentQuests() returned null even though a quest was "
              + "logged so it should have returned the quest");
    }
    questGiverComponent1.clearGoldSpentQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail("Quest.getQuestActiveForNPCID() returned true even though the quest was cleared");
    }
    if (Quest.getGoldSpentQuests().get(questGiverComponent1.uniqueNPCID) != null) {
      fail("Quest.getGoldSpentQuests() returned a quest even though the quest was cleared");
    }
  }

  @Test
  public void testCheckGoldSpentCompleteWhenNoQuestWasMade() {
    if (questGiverComponent.clearGoldSpentQuest()) {
      fail(
          "The function clearGoldSpentQuest() should have returned false when there's no quest to clear but didn't");
    }
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkGoldSpentQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkGoldSpentQuestComplete when it did not create a"
            + "GoldSpentQuest");
  }
}
