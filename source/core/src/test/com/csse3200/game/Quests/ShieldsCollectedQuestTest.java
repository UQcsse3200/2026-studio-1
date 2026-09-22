package com.csse3200.game.Quests;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShieldsCollectedQuestTest {
    static Entity entity;
    static QuestGiverComponent questGiverComponent;

    @BeforeAll
    public static void createAQuestEntity() {
        entity = new Entity();
        questGiverComponent = new QuestGiverComponent();
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
        if (!Quest.logShieldsCollectedQuest(questGiverComponent.uniqueNPCID, 5)) {
            fail(
                    "The test testIfCompletingAShieldsCollectedQuestWorks failed because "
                            + "Quest.logShieldsCollectedQuest returned false when it shouldn't have");
        }
        if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
            fail(
                    "The test testIfCompletingAShieldsCollectedQuestWorks failed because Quest.getQuestActiveForNPCID() "
                            + "returned false when it should have returned true");
        }
        Quest.clearShieldsCollectedQuest(questGiverComponent.uniqueNPCID);
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
                    "The test testMultipleSettingOfShieldsCollectedQuest failed because "
                            + "Quest.getQuestActiveForNPCID returned true even though no "
                            + "quest was created for that NPC");
        }
        if (!Quest.logShieldsCollectedQuest(questGiverComponent1.uniqueNPCID, 5)
                || !Quest.logShieldsCollectedQuest(questGiverComponent2.uniqueNPCID, 6)
                || !Quest.logShieldsCollectedQuest(questGiverComponent3.uniqueNPCID, 7)) {
            fail(
                    "The test testMultipleSettingOfShieldsCollectedQuest failed because "
                            + "Quest.logShieldsCollectedQuest returned false when it should have returned "
                            + "true");
        }
        if (Quest.logShieldsCollectedQuest(questGiverComponent1.uniqueNPCID, 5)) {
            fail(
                    "The test testMultipleSettingOfShieldsCollectedQuest failed because "
                            + "Quest.logShieldsCollectedQuest returned true when it shouldn't have "
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
        if (!questGiverComponent1.logShieldsCollectedQuest("", 9)) {
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
        questGiverComponent.clearShieldsCollectedQuest();
        assertThrows(
                NullPointerException.class,
                () -> questGiverComponent.checkShieldsCollectedQuestComplete(),
                "The questGiverComponent did not throw a NullPointerException when it "
                        + "called checkShieldsCollectedQuestComplete when it did not create a"
                        + "GoldSpentQuest");
    }
}
