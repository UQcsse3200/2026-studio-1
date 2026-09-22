package com.csse3200.game.Quests;

import java.util.ArrayList;

public class Quest {
  private static ArrayList<Integer> uniqueNPCID = new ArrayList<Integer>();
  // The unique NPCID lets us know if the NPC has a quest is active.
  private static ArrayList<Boolean> questActiveForNPCID = new ArrayList<Boolean>();

  // Quest trackers
  private static ArrayList<JumpQuest> jumpQuestTracker = new ArrayList<JumpQuest>();
  private static ArrayList<EnemiesKilledQuest> enemiesKilledQuestTracker =
      new ArrayList<EnemiesKilledQuest>();
  private static ArrayList<GoldSpentQuest> goldSpentQuestTracker = new ArrayList<GoldSpentQuest>();
  private static ArrayList<ShieldsCollectedQuest> shieldsCollectedQuestTracker = new ArrayList<ShieldsCollectedQuest>();
  // Statistics tracker
  private static float globalJumps = 0;
  private static float globalEnemiesKilled = 0;
  private static float globalGoldSpent = 0;
  private static float globalShieldsCollected = 0;

  public static int giveOutUniqueNPCID() {
    int uniqueID = uniqueNPCID.size();
    uniqueNPCID.add(uniqueID);
    questActiveForNPCID.add(false);
    // Add space for a possible jump quest later on.
    jumpQuestTracker.add(uniqueID, null);
    enemiesKilledQuestTracker.add(uniqueID, null);
    goldSpentQuestTracker.add(uniqueID,null);
    shieldsCollectedQuestTracker.add(uniqueID,null);
    return uniqueID;
  }

  // Jump quest functions start
  public static boolean logJumpQuest(int NPCId, int jumpsToDo) {
    if (questActiveForNPCID.get(NPCId)) {
      return false;
    } else {
      jumpQuestTracker.set(NPCId, new JumpQuest(jumpsToDo));
      questActiveForNPCID.set(NPCId, true);
      return true;
    }
  }

  public static int checkJumpQuestComplete(int NPCId) {
    if (jumpQuestTracker.get(NPCId) != null) {
      return jumpQuestTracker.get(NPCId).checkQuestProgress();
    } else {
      // There is no jumpQuest to check because it wasn't set up
      return -1;
    }
  }

  public static void clearJumpQuest(int NPCId) {
    jumpQuestTracker.set(NPCId, null);
    questActiveForNPCID.set(NPCId, false);
  }

  // Jump Quest functions end
  // EnemiesKilledQuest functions start
  public static boolean logEnemiesKilledQuest(int NPCId, int enemiesToKill) {
    if (questActiveForNPCID.get(NPCId)) {
      return false;
    } else {
      enemiesKilledQuestTracker.set(NPCId, new EnemiesKilledQuest(enemiesToKill));
      questActiveForNPCID.set(NPCId, true);
      return true;
    }
  }

  public static int checkEnemiesKilledQuest(int NPCId) {
    if (enemiesKilledQuestTracker.get(NPCId) != null) {
      return enemiesKilledQuestTracker.get(NPCId).checkQuestProgress();
    } else {
      // No EnemiesKilledQuest was set for the NPC in the enemiesKilledQuestTracker
      return -1;
    }
  }

  public static void clearEnemiesKilledQuest(int NPCId) {
    enemiesKilledQuestTracker.set(NPCId, null);
    questActiveForNPCID.set(NPCId, false);
  }

  // EnemiesKilledQuest functions end
  // GoldSpentQuest function start
  public static boolean logGoldSpentQuest(int NPCId, int amountOfGoldToSpend){
    if(questActiveForNPCID.get(NPCId)){
      return false;
    }else{
      goldSpentQuestTracker.set(NPCId,new GoldSpentQuest(amountOfGoldToSpend));
      questActiveForNPCID.set(NPCId,true);
      return true;
    }
  }
  public static int checkGoldSpentQuest(int NPCId){
    if(goldSpentQuestTracker.get(NPCId)!= null){
      return goldSpentQuestTracker.get(NPCId).checkQuestProgress();
    }else{
      //There is no quest created for that NPCId
      return -1;
    }
  }
  public static void clearGoldSpentQuest(int NPCId){
    goldSpentQuestTracker.set(NPCId,null);
    questActiveForNPCID.set(NPCId,false);
  }
  //GoldSpentQuest functions end
  //ShieldsCollectedQuest functions start
  public static boolean logShieldsCollectedQuest(int NPCId, int amountOfShieldsToCollect){
    if(questActiveForNPCID.get(NPCId)){
      return false;
    }else{
      shieldsCollectedQuestTracker.set(NPCId, new ShieldsCollectedQuest(amountOfShieldsToCollect));
      questActiveForNPCID.set(NPCId,true);
      return true;
    }
  }

  public static int checkShieldsCollectedQuest(int NPCId){
    if(shieldsCollectedQuestTracker.get(NPCId)!=null){
      return getShieldsCollectedQuests().get(NPCId).checkQuestProgress();
    }else{
      //No shieldCollectedQuest was created for that NPC
      return -1;
    }
  }
  public static void clearShieldsCollectedQuest(int NPCId){
    shieldsCollectedQuestTracker.set(NPCId,null);
    questActiveForNPCID.set(NPCId,false);
  }
  //ShieldsCollectedQuest functions end
  public static ArrayList<JumpQuest> getJumpQuests() {
    return jumpQuestTracker;
  }

  public static ArrayList<EnemiesKilledQuest> getEnemiesKilledQuests() {
    return enemiesKilledQuestTracker;
  }

  public static ArrayList<GoldSpentQuest> getGoldSpentQuests(){
    return goldSpentQuestTracker;
  }

  public static ArrayList<ShieldsCollectedQuest> getShieldsCollectedQuests(){
    return shieldsCollectedQuestTracker;
  }

  public static void incrementGlobalJumps() {
    globalJumps++;
  }

  public static void incrementGlobalEnemiesKilled() {
    globalEnemiesKilled++;
  }

  public static void addGlobalGoldSpent(int amount){
    globalGoldSpent+=amount;
  }
  public static void incrementGlobalShieldsCollected(){
    globalShieldsCollected++;
  }

  public static float getGlobalJumps() {
    return globalJumps;
  }

  public static float getGlobalEnemiesKilled() {
    return globalEnemiesKilled;
  }

  public static float getGlobalGoldSpent(){
    return globalGoldSpent;
  }

  public static float getGlobalShieldsCollected(){
    return globalShieldsCollected;
  }

  public static ArrayList<Integer> getUniqueNPCIDArrayList() {
    return uniqueNPCID;
  }

  public static ArrayList<Boolean> getQuestActiveForNPCID() {
    return questActiveForNPCID;
  }
}
