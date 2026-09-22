package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EnemiesKilledQuestTest {
  static Entity entity;
  static QuestGiverComponent questGiverComponent;

  @BeforeAll
  public static void createAQuestEntity() {
    entity = new Entity();
    questGiverComponent = new QuestGiverComponent();
    entity.addComponent(questGiverComponent);
  }

  @Test
  public void testIfCompletingAEnemiesKilledQuestWorks() {
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAEnemiesKilledQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since no quest was created "
              + "for that entity");
    }
    // Creates the quest in the conditional
    if (Quest.getEnemiesKilledQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail(
          "The test testIfCompletingAEnemiesKilledQuestWorks did not return null"
              + "when it checked the enemiesKilledQuestTracker even though no "
              + "quest was created");
    }
    if (!Quest.logEnemiesKilledQuest(questGiverComponent.uniqueNPCID, 5)) {
      fail(
          "The test testIfCompletingAEnemiesKilledQuestWorks failed because "
              + "Quest.logEnemiesKilledQuest returned false when it shouldn't have");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAEnemiesKilledQuestWorks failed because Quest.getQuestActiveForNPCID() "
              + "returned false when it should have returned true");
    }
    Quest.clearEnemiesKilledQuest(questGiverComponent.uniqueNPCID);
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAEnemiesKilledQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since the quest was cleared "
              + "for that entity");
    }
  }

  @Test
  public void testMultipleSettingOfEnemiesKilledQuest() {
    Entity entity1 = new Entity();
    Entity entity2 = new Entity();
    Entity entity3 = new Entity();
    QuestGiverComponent questGiverComponent1 = new QuestGiverComponent();
    QuestGiverComponent questGiverComponent2 = new QuestGiverComponent();
    QuestGiverComponent questGiverComponent3 = new QuestGiverComponent();
    entity1.addComponent(questGiverComponent1);
    entity2.addComponent(questGiverComponent2);
    entity3.addComponent(questGiverComponent3);
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "Quest.getQuestActiveForNPCID returned true even though no "
              + "quest was created for that NPC");
    }
    if (!Quest.logEnemiesKilledQuest(questGiverComponent1.uniqueNPCID, 5)
        || !Quest.logEnemiesKilledQuest(questGiverComponent2.uniqueNPCID, 6)
        || !Quest.logEnemiesKilledQuest(questGiverComponent3.uniqueNPCID, 7)) {
      fail(
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "Quest.logEnemiesKilledQuest returned false when it should have returned "
              + "true");
    }
    if (Quest.logEnemiesKilledQuest(questGiverComponent1.uniqueNPCID, 5)) {
      fail(
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "Quest.logEnemiesKilledQuest returned true when it shouldn't have "
              + "because a quest was already set");
    }
    assertEquals(
        0,
        Quest.checkEnemiesKilledQuest(questGiverComponent1.uniqueNPCID),
        "Quest.checkEnemiesKilledQuest returned a value other than 0 even "
            + "though no progress was made");
    questGiverComponent1.clearEnemiesKilledQuest();
    questGiverComponent2.clearEnemiesKilledQuest();
    questGiverComponent3.clearEnemiesKilledQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned true when it should have returned "
              + "false since the quest was cleared");
    }
    // Logging the EnemiesKilledQuest
    if (!questGiverComponent1.logEnemiesKilledQuest("", 9)) {
      fail(
          "questGiverComponent1.logEnemiesKilledQuest returned false when it should "
              + "have returned true");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned false when it should have "
              + "returned true since a quest was logged");
    }
    if (Quest.getEnemiesKilledQuests().get(questGiverComponent1.uniqueNPCID) == null) {
      fail(
          "Quest.getEnemiesKilledQuests() returned null even though a quest was "
              + "logged so it should have returned the quest");
    }
    questGiverComponent1.clearEnemiesKilledQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail("Quest.getQuestActiveForNPCID() returned true even though the quest was cleared");
    }
    if (Quest.getEnemiesKilledQuests().get(questGiverComponent1.uniqueNPCID) != null) {
      fail("Quest.getEnemiesKilledQuests() returned a quest even though the quest was cleared");
    }
  }

  @Test
  public void testCheckGoldSpentCompleteWhenNoQuestWasMade() {
    questGiverComponent.clearEnemiesKilledQuest();
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkEnemiesKilledQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkEnemiesKilledQuestComplete when it did not create a"
            + "EnemiesKilledQuest");
  }
}
