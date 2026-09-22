package com.csse3200.game.Quests;

import java.util.ArrayList;

public class Quest {
  private static ArrayList<Integer> uniqueNPCID = new ArrayList<Integer>();
  // The unique NPCID lets us know if the NPC has a quest is active.
  private static ArrayList<Boolean> questActiveForNPCID = new ArrayList<Boolean>();

  // Quest trackers
  private static ArrayList<JumpQuest> jumpQuestTracker = new ArrayList<JumpQuest>();
  private static ArrayList<EnemiesKilledQuest> enemiesKilledQuestArrayList = new ArrayList<EnemiesKilledQuest>();
  // Statistics tracker
  private static int globalJumps = 1;
  private static int enemiesKilled = 1;

  public static int giveOutUniqueNPCID() {
    int uniqueID = uniqueNPCID.size();
    uniqueNPCID.add(uniqueID);
    questActiveForNPCID.add(false);
    // Add space for a possible jump quest later on.
    jumpQuestTracker.add(uniqueID, null);
    enemiesKilledQuestArrayList.add(uniqueID,null);
    return uniqueID;
  }
  //Jump quest functions start
  public static boolean logJumpQuest(int NPCId, int jumpsToDo) {
    if (questActiveForNPCID.get(NPCId)) {
      return false;
    } else {
      jumpQuestTracker.set(NPCId, new JumpQuest(globalJumps, jumpsToDo));
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
  //Jump Quest functions end

  public static ArrayList<JumpQuest> getJumpQuests() {
    return jumpQuestTracker;
  }

  public static void incrementGlobalJumps() {
    globalJumps++;
  }
  public static void incrementEnemiesKilled(){enemiesKilled++;}

  public static int getGlobalJumps() {
    return globalJumps;
  }
  public static int getEnemiesKilled(){
    return enemiesKilled;
  }

  public static ArrayList<Integer> getUniqueNPCIDArrayList() {
    return uniqueNPCID;
  }

  public static ArrayList<Boolean> getQuestActiveForNPCID() {
    return questActiveForNPCID;
  }
}
