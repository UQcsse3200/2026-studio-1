package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;

public class QuestGiverComponent extends Component {
  public int uniqueNPCID;

  public QuestGiverComponent() {
    uniqueNPCID = Quest.giveOutUniqueNPCID();
  }

  // Jump quest functions start here
  public boolean logJumpQuest(String Reward, int jumpsToDo) {
    if (Quest.logJumpQuest(uniqueNPCID, jumpsToDo)) {
      return true;
    } else {
      return false;
    }
  }

  public void clearJumpQuest() {
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
  public boolean logEnemiesKilledQuest(String Reward, int enemiesToKill) {
    if (Quest.logEnemiesKilledQuest(uniqueNPCID, enemiesToKill)) {
      return true;
    } else {
      return false;
    }
  }

  public void clearEnemiesKilledQuest() {
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
  //EnemiesKilledQuest functions end here
  //GoldSpentQuest functions start here
  public boolean logGoldSpentQuest(String reward, int amountToSpend){
    if(Quest.logGoldSpentQuest(uniqueNPCID,amountToSpend)){
      return true;
    }else{
      return false;
    }
  }
  public int checkGoldSpentQuestComplete(){
    if (Quest.checkGoldSpentQuest(uniqueNPCID) == -1) {
      throw new NullPointerException(
              "A QuestGiver component tried to call checkGoldSpentQuest when "
                      + "there isn't a quest to check the progress of i.e. it returned null");
    } else {
      return Quest.checkGoldSpentQuest(uniqueNPCID);
    }
  }
  public void clearGoldSpentQuest(){
    Quest.clearGoldSpentQuest(uniqueNPCID);
  }
  //GoldSpentQuest functions end here
  //ShieldsCollectedQuest functions start here
  public boolean logShieldsCollectedQuest(String reward, int shieldsToCollect){
    if(Quest.logShieldsCollectedQuest(uniqueNPCID,shieldsToCollect)){
      return true;
    }else{
      return false;
    }
  }
  public void clearShieldsCollectedQuest(){
    Quest.clearShieldsCollectedQuest(uniqueNPCID);
  }
  public int checkShieldsCollectedQuestComplete(){
    if(Quest.checkShieldsCollectedQuest(uniqueNPCID)==-1){
      throw new NullPointerException(
              "A QuestGiver component tried to call checkShieldsCollectedQuest when "
                      + "there isn't a quest to check the progress of i.e. it returned null");
    }else{
      return Quest.checkShieldsCollectedQuest(uniqueNPCID);
    }
  }
  //ShieldsCollectedQuest functions end here
}
