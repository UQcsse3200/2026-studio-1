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
}
