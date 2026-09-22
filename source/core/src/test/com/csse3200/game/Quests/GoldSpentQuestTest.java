package com.csse3200.game.Quests;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoldSpentQuestTest {
    static Entity entity;
    static QuestGiverComponent questGiverComponent;

    @BeforeAll
    public static void createAQuestEntity() {
        entity = new Entity();
        questGiverComponent = new QuestGiverComponent();
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
        if (!Quest.logGoldSpentQuest(questGiverComponent.uniqueNPCID, 5)) {
            fail(
                    "The test testIfCompletingAGoldSpentQuestWorks failed because "
                            + "Quest.logGoldSpentQuest returned false when it shouldn't have");
        }
        if (!Quest.getQuestActiveForNPCID().get(questGiverComponent.uniqueNPCID)) {
            fail(
                    "The test testIfCompletingAGoldSpentQuestWorks failed because Quest.getQuestActiveForNPCID() "
                            + "returned false when it should have returned true");
        }
        Quest.clearGoldSpentQuest(questGiverComponent.uniqueNPCID);
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
                    "The test testMultipleSettingOfGoldSpentQuest failed because "
                            + "Quest.getQuestActiveForNPCID returned true even though no "
                            + "quest was created for that NPC");
        }
        if (!Quest.logGoldSpentQuest(questGiverComponent1.uniqueNPCID, 5)
                || !Quest.logGoldSpentQuest(questGiverComponent2.uniqueNPCID, 6)
                || !Quest.logGoldSpentQuest(questGiverComponent3.uniqueNPCID, 7)) {
            fail(
                    "The test testMultipleSettingOfGoldSpentQuest failed because "
                            + "Quest.logGoldSpentQuest returned false when it should have returned "
                            + "true");
        }
        if (Quest.logGoldSpentQuest(questGiverComponent1.uniqueNPCID, 5)) {
            fail(
                    "The test testMultipleSettingOfGoldSpentQuest failed because "
                            + "Quest.logGoldSpentQuest returned true when it shouldn't have "
                            + "because a quest was already set");
        }
        assertEquals(
                0,
                Quest.checkGoldSpentQuest(questGiverComponent1.uniqueNPCID),
                "Quest.checkGoldSpentQuest returned a value other than 0 even "
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
        if (!questGiverComponent1.logGoldSpentQuest("", 9)) {
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
        questGiverComponent.clearGoldSpentQuest();
        assertThrows(
                NullPointerException.class,
                () -> questGiverComponent.checkGoldSpentQuestComplete(),
                "The questGiverComponent did not throw a NullPointerException when it "
                        + "called checkGoldSpentQuestComplete when it did not create a"
                        + "GoldSpentQuest");
    }
}
