package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EnemiesKilledQuestTest {
  static Entity entity;
  static QuestGiverComponent questGiverComponent;

  @BeforeEach
  public void createAQuestEntity() {
    entity = new Entity();
    questGiverComponent = new QuestGiverComponent(null, 0);
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
    questGiverComponent.clearEnemiesKilledQuest();
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
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "Quest.getQuestActiveForNPCID returned true even though no "
              + "quest was created for that NPC");
    }
    if (!questGiverComponent1.logEnemiesKilledQuest(5)
        || !questGiverComponent2.logEnemiesKilledQuest(6)
        || !questGiverComponent3.logEnemiesKilledQuest(7)) {
      fail(
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "logEnemiesKilledQuest returned false when it should have returned "
              + "true");
    }
    if (questGiverComponent1.logEnemiesKilledQuest(5)) {
      fail(
          "The test testMultipleSettingOfEnemiesKilledQuest failed because "
              + "logEnemiesKilledQuest returned true when it shouldn't have "
              + "because a quest was already set");
    }
    assertEquals(
        0,
        questGiverComponent1.checkEnemiesKilledQuestComplete(),
        "checkEnemiesKilledQuest returned a value other than 0 even "
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
    if (!questGiverComponent1.logEnemiesKilledQuest(9)) {
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
    if (questGiverComponent.clearEnemiesKilledQuest()) {
      fail(
          "The function clearEnemiesKilledQuest should have returned false when there's no quest to clear but did not");
    }
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkEnemiesKilledQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkEnemiesKilledQuestComplete when it did not create a"
            + "EnemiesKilledQuest");
  }

  @Test
  public void testIfEnemiesKilledQuestChangesProgress() {
    questGiverComponent.logEnemiesKilledQuest(2);
    Quest.incrementGlobalEnemiesKilled();
    assertEquals(50, questGiverComponent.checkEnemiesKilledQuestComplete());
  }

  @Test
  public void testIfEnemiesKilledQuestGivesProgressOver100() {
    questGiverComponent.logEnemiesKilledQuest(1);
    Quest.incrementGlobalEnemiesKilled();
    Quest.incrementGlobalEnemiesKilled();
    assertEquals(100, questGiverComponent.checkEnemiesKilledQuestComplete());
  }

  @Test
  public void checkIfGivingEnemiesToKillAsZeroThrows() {
    int number = 0;
    assertThrows(
        IllegalArgumentException.class,
        () -> questGiverComponent.logEnemiesKilledQuest(number),
        "When the constructor for EnemiesKilledQuest was given "
            + number
            + " as the"
            + "parameter for enemiesToKill, the constructor didn't throw an exception");
  }
}
