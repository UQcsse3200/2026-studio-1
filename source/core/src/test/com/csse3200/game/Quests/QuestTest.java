package com.csse3200.game.Quests;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class QuestTest {
  @Test
  public void testIfNPCIDWorks() {
    Entity entity = new Entity();
    boolean foundID = false;
    QuestGiverComponent questGiverComponent = new QuestGiverComponent();
    entity.addComponent(questGiverComponent);
    int NPCID = questGiverComponent.uniqueNPCID;
    ArrayList<Integer> arrayList = Quest.getUniqueNPCIDArrayList();
    for (int i = 0; i < arrayList.size(); i++) {
      if (arrayList.get(i) == NPCID) {
        // We found our NPCID in the arrayList.
        foundID = true;
      }
    }
    if (!foundID) {
      fail(
          "The test testIfNPCIDWorks did not have our unique Quest NPCID even "
              + "though it should have generated one");
    }
  }

  @Test
  public void testIfQuestActiveForNPCWorks() {
    Entity entity = new Entity();
    boolean foundBoolean = false;
    QuestGiverComponent questGiverComponent = new QuestGiverComponent();
    entity.addComponent(questGiverComponent);
    questGiverComponent.logJumpQuest("", 5);

    ArrayList<Boolean> arrayList = Quest.getQuestActiveForNPCID();
    if (arrayList.get(questGiverComponent.uniqueNPCID)) {
      // We found our NPCID in the arrayList.
      foundBoolean = true;
    }
    if (!foundBoolean) {
      fail(
          "The test testIfQuestActiveForNPCWorks did not have our unique Quest NPCID even "
              + "though it should have generated one");
    }
  }

  @Test
  public void testIfCompletingAJumpQuestWorks() {
    Entity entity = new Entity();
    QuestGiverComponent questGiverComponent = new QuestGiverComponent();
    entity.addComponent(questGiverComponent);
    if (Quest.getJumpQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail("The test testIfCompletingAJumpQuestWorks did not return null when it should have");
    }
    questGiverComponent.logJumpQuest("", 5);
    if (Quest.getJumpQuests().get(questGiverComponent.uniqueNPCID) == null) {
      fail("The test testIfCompletingAJumpQuestWorks returned null when it shouldn't have");
    }
    questGiverComponent.clearJumpQuest();
    if (Quest.getJumpQuests().get(questGiverComponent.uniqueNPCID) != null) {
      fail("The test testIfCompletingAJumpQuestWorks did not return null when it should have");
    }
  }

  @Test
  public void testMultipleSettingAndClearingOfJumpQuests() {
    Entity entity1 = new Entity();
    Entity entity2 = new Entity();
    Entity entity3 = new Entity();
    QuestGiverComponent questGiverComponent1 = new QuestGiverComponent();
    entity1.addComponent(questGiverComponent1);
    QuestGiverComponent questGiverComponent2 = new QuestGiverComponent();
    entity2.addComponent(questGiverComponent2);
    QuestGiverComponent questGiverComponent3 = new QuestGiverComponent();
    entity3.addComponent(questGiverComponent3);
    questGiverComponent1.logJumpQuest("", 5);
    questGiverComponent2.logJumpQuest("", 6);
    questGiverComponent3.logJumpQuest("", 7);
    if (Quest.getJumpQuests().get(questGiverComponent1.uniqueNPCID) == null
        || Quest.getJumpQuests().get(questGiverComponent2.uniqueNPCID) == null
        || Quest.getJumpQuests().get(questGiverComponent3.uniqueNPCID) == null) {
      fail(
          "The quest class returns null on the JumpQuest arraylist when "
              + "it should have returned the quest");
    }
    if (questGiverComponent1.logJumpQuest("", 5)) {
      fail(
          "The Quest class doesn't recognise that questGiverComponent1 has already "
              + "logged a quest");
    }
    questGiverComponent3.clearJumpQuest();
    questGiverComponent2.clearJumpQuest();
    questGiverComponent1.clearJumpQuest();
    questGiverComponent1.logJumpQuest("", 5);
    if (Quest.getJumpQuests().get(questGiverComponent1.uniqueNPCID) == null) {
      fail(
          "The Quest class doesn't allow a questGiverComponent to log another quest "
              + "after it's cleared it's previous quest");
    }
    questGiverComponent1.clearJumpQuest();
    if (Quest.getJumpQuests().get(questGiverComponent1.uniqueNPCID) != null
        || Quest.getJumpQuests().get(questGiverComponent2.uniqueNPCID) != null
        || Quest.getJumpQuests().get(questGiverComponent3.uniqueNPCID) != null) {
      fail(
          "The quest class returns a JumpQuest on the JumpQuest arraylist when "
              + "it should have returned null");
    }
  }

  @Test
  public void testCheckJumpQuestCompleteWhenNoQuestWasMade() {
    Entity entity = new Entity();
    QuestGiverComponent questGiverComponent = new QuestGiverComponent();
    entity.addComponent(questGiverComponent);
    questGiverComponent.clearJumpQuest();
    assertThrows(
        NullPointerException.class,
        () -> questGiverComponent.checkJumpQuestComplete(),
        "The questGiverComponent did not throw a NullPointerException when it "
            + "called checkJumpQuestComplete when it did not create a jump quest");
  }
}
