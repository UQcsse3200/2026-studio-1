package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ShieldsCollectedQuestTest {
  static Entity entity;
  static QuestGiverComponent questGiverComponent;
  static Entity player;
  ShieldComponent shieldComponent;

  @BeforeEach
  public void createAQuestEntity() {
    entity = new Entity();
    player = new Entity();
    shieldComponent = new ShieldComponent();
    player.addComponent(shieldComponent);
    questGiverComponent = new QuestGiverComponent(player, 0);
    entity.addComponent(questGiverComponent);
  }

  @Test
  public void testIfCompletingAShieldsCollectedQuestWorks() {
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAShieldsCollectedQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since no quest was created "
              + "for that entity");
    }
    // Creates the quest in the conditional
    if (Quest.getShieldsCollectedQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail(
          "The test testIfCompletingAShieldsCollectedQuestWorks did not return null"
              + "when it checked the goldSpentQuestTracker even though no "
              + "quest was created");
    }
    if (!questGiverComponent.logShieldsCollectedQuest(6)) {
      fail(
          "The test testIfCompletingAShieldsCollectedQuestWorks failed because "
              + "logShieldsCollectedQuest returned false when it shouldn't have");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAShieldsCollectedQuestWorks failed because Quest.getQuestActiveForNPCID() "
              + "returned false when it should have returned true");
    }
    questGiverComponent.clearShieldsCollectedQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
      fail(
          "The test testIfCompletingAShieldsCollectedQuestWorks failed because Quest.getQuestActiveForNPCID()"
              + "returned true even though it should have returned false since the quest was cleared "
              + "for that entity");
    }
  }

  @Test
  public void testMultipleSettingOfShieldsCollectedQuest() {
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
          "The test testMultipleSettingOfShieldsCollectedQuest failed because "
              + "Quest.getQuestActiveForNPCID returned true even though no "
              + "quest was created for that NPC");
    }
    if (!questGiverComponent1.logShieldsCollectedQuest(6)
        || !questGiverComponent2.logShieldsCollectedQuest(7)
        || !questGiverComponent3.logShieldsCollectedQuest(8)) {
      fail(
          "The test testMultipleSettingOfShieldsCollectedQuest failed because "
              + "logShieldsCollectedQuest returned false when it should have returned "
              + "true");
    }
    if (questGiverComponent1.logShieldsCollectedQuest(6)) {
      fail(
          "The test testMultipleSettingOfShieldsCollectedQuest failed because "
              + "logShieldsCollectedQuest returned true when it shouldn't have "
              + "because a quest was already set");
    }
    assertEquals(
        0,
        Quest.checkShieldsCollectedQuest(questGiverComponent1.uniqueNPCID),
        "Quest.checkShieldsCollectedQuest returned a value other than 0 even "
            + "though no progress was made");
    questGiverComponent1.clearShieldsCollectedQuest();
    questGiverComponent2.clearShieldsCollectedQuest();
    questGiverComponent3.clearShieldsCollectedQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent2.uniqueNPCID)
        || Quest.getQuestActiveForNPCID().get(questGiverComponent3.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned true when it should have returned "
              + "false since the quest was cleared");
    }
    // Logging the ShieldsCollectedQuest
    if (!questGiverComponent1.logShieldsCollectedQuest(9)) {
      fail(
          "questGiverComponent1.logShieldsCollectedQuest returned false when it should "
              + "have returned true");
    }
    if (!Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail(
          "Quest.getQuestActiveForNPCID() returned false when it should have "
              + "returned true since a quest was logged");
    }
    if (Quest.getShieldsCollectedQuests().get(questGiverComponent1.uniqueNPCID) == null) {
      fail(
          "Quest.getShieldsCollectedQuests() returned null even though a quest was "
              + "logged so it should have returned the quest");
    }
    questGiverComponent1.clearShieldsCollectedQuest();
    if (Quest.getQuestActiveForNPCID().get(questGiverComponent1.uniqueNPCID)) {
      fail("Quest.getQuestActiveForNPCID() returned true even though the quest was cleared");
    }
    if (Quest.getShieldsCollectedQuests().get(questGiverComponent1.uniqueNPCID) != null) {
      fail("Quest.getShieldsCollectedQuests() returned a quest even though the quest was cleared");
    }
  }

  @Test
  public void testCheckShieldsCollectedCompleteWhenNoQuestWasMade() {
    if (questGiverComponent.clearShieldsCollectedQuest()) {
      fail(
          "The function clearShieldsCollectedQuest() should have returned false when there was no quest to clear but didn't");
    }
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkShieldsCollectedQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkShieldsCollectedQuestComplete when it did not create a"
            + "GoldSpentQuest");
  }

  @Test
  public void testIfShieldsCollectedChangesQuestProgress() {
    questGiverComponent.logShieldsCollectedQuest(2);
    shieldComponent.grantShield();
    assertEquals(50, questGiverComponent.checkShieldsCollectedQuestComplete());
  }

  @Test
  public void testIfShieldsCollectedGiveProgressOver100() {
    questGiverComponent.logShieldsCollectedQuest(1);
    shieldComponent.grantShield();
    shieldComponent.grantShield();
    assertEquals(100, questGiverComponent.checkShieldsCollectedQuestComplete());
  }

  @Test
  public void checkIfGivingShieldsToCollectAsZeroThrows() {
    int number = 0;
    assertThrows(
        IllegalArgumentException.class,
        () -> questGiverComponent.logShieldsCollectedQuest(number),
        "When the constructor for ShieldsCollectedQuest was given "
            + number
            + " as the "
            + "parameter for shieldsToCollect, the constructor didn't throw an exception");
  }
}
