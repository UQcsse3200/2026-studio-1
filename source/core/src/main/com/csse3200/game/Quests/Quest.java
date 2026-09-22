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
  // Statistics tracker
  private static float globalJumps = 0;
  private static float globalEnemiesKilled = 0;

  public static int giveOutUniqueNPCID() {
    int uniqueID = uniqueNPCID.size();
    uniqueNPCID.add(uniqueID);
    questActiveForNPCID.add(false);
    // Add space for a possible jump quest later on.
    jumpQuestTracker.add(uniqueID, null);
    enemiesKilledQuestTracker.add(uniqueID, null);
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
  public static ArrayList<JumpQuest> getJumpQuests() {
    return jumpQuestTracker;
  }

  public static ArrayList<EnemiesKilledQuest> getEnemiesKilledQuests() {
    return enemiesKilledQuestTracker;
  }

  public static void incrementGlobalJumps() {
    globalJumps++;
  }

  public static void incrementGlobalEnemiesKilled() {
    globalEnemiesKilled++;
  }

  public static float getGlobalJumps() {
    return globalJumps;
  }

  public static float getGlobalEnemiesKilled() {
    return globalEnemiesKilled;
  }

  public static ArrayList<Integer> getUniqueNPCIDArrayList() {
    return uniqueNPCID;
  }

  public static ArrayList<Boolean> getQuestActiveForNPCID() {
    return questActiveForNPCID;
  }
}
