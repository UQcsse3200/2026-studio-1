package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestGiverComponent extends Component {
  public int uniqueNPCID;
  public int goldToGive = 0;
  public Item itemToGive = null;
  private static Logger logger = LoggerFactory.getLogger(QuestGiverComponent.class);
  private Entity player;

  public QuestGiverComponent(Entity player) {
    uniqueNPCID = Quest.giveOutUniqueNPCID();
    this.player = player;
  }

  public QuestGiverComponent(Entity player, int goldToGive) {
    uniqueNPCID = Quest.giveOutUniqueNPCID();
    this.goldToGive = goldToGive;
    this.player = player;
  }

  public QuestGiverComponent(Entity player, Item itemToGive) {
    uniqueNPCID = Quest.giveOutUniqueNPCID();
    this.itemToGive = itemToGive;
    this.player = player;
  }

  // Jump quest functions start here
  public boolean logJumpQuest(int jumpsToDo) {
    if (Quest.logJumpQuest(uniqueNPCID, jumpsToDo)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean clearJumpQuest() {
    try {
      checkJumpQuestComplete();
    } catch (Exception e) {
      logger.error(
          "When trying to clear a quest, check*QuestProgress returned a exception."
              + " Here's the details "
              + e.getMessage());
      return false;
    }
    if (!giveOutQuestRewards(checkJumpQuestComplete())) return false;
    Quest.clearJumpQuest(uniqueNPCID);
    return true;
  }

  public int checkJumpQuestComplete() {
    if (Quest.checkJumpQuestComplete(uniqueNPCID) == -1) {
      throw new NullPointerException(
          "A QuestGiver component tried to call checkJumpQuestComplete when "
              + "there isn't a quest to check the progress of i.e. it returned null");
    } else {
      return Quest.checkJumpQuestComplete(uniqueNPCID);
    }
  }

  // Jump quest functions end here
  // EnemiesKilledQuest functions start here
  public boolean logEnemiesKilledQuest(int enemiesToKill) {
    if (Quest.logEnemiesKilledQuest(uniqueNPCID, enemiesToKill)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean clearEnemiesKilledQuest() {
    try {
      checkEnemiesKilledQuestComplete();
    } catch (Exception e) {
      logger.error(
          "When trying to clear a quest, check*QuestProgress returned a exception."
              + " Here's the details "
              + e.getMessage());
      return false;
    }
    if (!giveOutQuestRewards(checkEnemiesKilledQuestComplete())) return false;
    Quest.clearEnemiesKilledQuest(uniqueNPCID);
    return true;
  }

  public int checkEnemiesKilledQuestComplete() {
    if (Quest.checkEnemiesKilledQuest(uniqueNPCID) == -1) {
      throw new NullPointerException(
          "A QuestGiver component tried to call checkEnemiesKilledQuest when "
              + "there isn't a quest to check the progress of i.e. it returned null");
    } else {
      return Quest.checkEnemiesKilledQuest(uniqueNPCID);
    }
  }

  // EnemiesKilledQuest functions end here
  // GoldSpentQuest functions start here
  public boolean logGoldSpentQuest(int amountToSpend) {
    if (Quest.logGoldSpentQuest(uniqueNPCID, amountToSpend)) {
      return true;
    } else {
      return false;
    }
  }

  public int checkGoldSpentQuestComplete() {
    if (Quest.checkGoldSpentQuest(uniqueNPCID) == -1) {
      throw new NullPointerException(
          "A QuestGiver component tried to call checkGoldSpentQuest when "
              + "there isn't a quest to check the progress of i.e. it returned null");
    } else {
      return Quest.checkGoldSpentQuest(uniqueNPCID);
    }
  }

  public boolean clearGoldSpentQuest() {
    try {
      checkGoldSpentQuestComplete();
    } catch (Exception e) {
      logger.error(
          "When trying to clear a quest, check*QuestProgress returned a exception."
              + " Here's the details "
              + e.getMessage());
      return false;
    }
    if (!giveOutQuestRewards(checkGoldSpentQuestComplete())) return false;
    Quest.clearGoldSpentQuest(uniqueNPCID);
    return true;
  }

  // GoldSpentQuest functions end here
  // ShieldsCollectedQuest functions start here
  public boolean logShieldsCollectedQuest(int shieldsToCollect) {
    if (Quest.logShieldsCollectedQuest(uniqueNPCID, shieldsToCollect)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean clearShieldsCollectedQuest() {
    try {
      checkShieldsCollectedQuestComplete();
    } catch (Exception e) {
      logger.error(
          "When trying to clear a quest, check*QuestProgress returned a exception."
              + " Here's the details "
              + e.getMessage());
      return false;
    }
    if (!giveOutQuestRewards(checkShieldsCollectedQuestComplete())) return false;
    Quest.clearShieldsCollectedQuest(uniqueNPCID);
    return true;
  }

  public int checkShieldsCollectedQuestComplete() {
    if (Quest.checkShieldsCollectedQuest(uniqueNPCID) == -1) {
      throw new NullPointerException(
          "A QuestGiver component tried to call checkShieldsCollectedQuest when "
              + "there isn't a quest to check the progress of i.e. it returned null");
    } else {
      return Quest.checkShieldsCollectedQuest(uniqueNPCID);
    }
  }

  // ShieldsCollectedQuest functions end here
  private boolean rewardGold() {
    if (goldToGive >= 0 && player != null) {
      player.getComponent(InventoryComponent.class).addGold(goldToGive);
      return true;
    }else{
      return false;
    }
  }

  private boolean rewardItem() {
    if (itemToGive != null && player != null) {
      // If the inventory is full, then don't end the quest
      if (player.getComponent(InventoryComponent.class).isFull()) {
        logger.info("Player's inventory was full so the quest could not be completed");
        return false;
      }
      player.getComponent(InventoryComponent.class).addItem(itemToGive);
      return true;
    }
    return false;
  }

  private boolean giveOutQuestRewards(int questProgress) {
    try {
      if (questProgress >= 100) {
        if(!rewardGold())return false;
        if (!rewardItem()) return false;
      }
    } catch (Exception e) {
      logger.error(
          "When trying to reward the player for a quest, the following exception was raised:\n {}",
          e.getMessage());
      return false;
    }
    return true;
  }
}
