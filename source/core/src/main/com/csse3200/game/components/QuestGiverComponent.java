package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponType;
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
  public QuestGiverComponent(Entity player){
    uniqueNPCID = Quest.giveOutUniqueNPCID();
    this.player = player;
  }
  public QuestGiverComponent(Entity player, int goldToGive) {
    uniqueNPCID = Quest.giveOutUniqueNPCID();
    this.goldToGive = goldToGive;
    this.player = player;
  }
  public QuestGiverComponent(Entity player, Item itemToGive){
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

  public void clearJumpQuest() {
    try {
      if (checkJumpQuestComplete() == 1) {
        rewardGold();
        rewardItem();
      }
    } catch (Exception e) {
      logger.error("When trying to reward the player for a quest, the following exception was raised:\n {}", e.getMessage());
    }
    Quest.clearJumpQuest(uniqueNPCID);
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

  public void clearEnemiesKilledQuest() {
    try {
      if (checkEnemiesKilledQuestComplete() == 1) {
        rewardGold();
        rewardItem();
      }
    } catch (Exception e) {
      logger.error("When trying to reward the player for a quest, the following exception was raised:\n {}", e.getMessage());
    }
    Quest.clearEnemiesKilledQuest(uniqueNPCID);
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

  public void clearGoldSpentQuest() {
    try {
      if (checkGoldSpentQuestComplete() == 1) {
        rewardGold();
        rewardItem();
      }
    }catch(Exception e){
      logger.error("When trying to reward the player for a quest, the following exception was raised:\n {}", e.getMessage());
    }
    Quest.clearGoldSpentQuest(uniqueNPCID);
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

  public void clearShieldsCollectedQuest() {
    try {
      if (checkShieldsCollectedQuestComplete() >= 100) {
        rewardGold();
        rewardItem();
      }
    } catch (Exception e) {
        logger.error("When trying to reward the player for a quest, the following exception was raised:\n {}", e.getMessage());
    }
    Quest.clearShieldsCollectedQuest(uniqueNPCID);
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
  private void rewardGold() {
    if (goldToGive >= 0&&player != null) {
        player.getComponent(InventoryComponent.class).addGold(goldToGive);
    }
    }
    private void rewardItem(){
      if(itemToGive!= null && player != null){
        //May cause issues if the inventory is full
        player.getComponent(InventoryComponent.class).addItem(itemToGive);
      }
    }
  }
