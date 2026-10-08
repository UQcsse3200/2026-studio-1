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
  private static ArrayList<ShieldsCollectedQuest> shieldsCollectedQuestTracker =
      new ArrayList<ShieldsCollectedQuest>();
  // Statistics tracker
  private static int globalJumps = 0;
  private static int globalEnemiesKilled = 0;
  private static int globalGoldSpent = 0;
  private static int globalShieldsCollected = 0;

  public static int giveOutUniqueNPCID() {
    int uniqueID = uniqueNPCID.size();
    uniqueNPCID.add(uniqueID);
    questActiveForNPCID.add(false);
    // Add space for a possible jump quest later on.
    jumpQuestTracker.add(uniqueID, null);
    enemiesKilledQuestTracker.add(uniqueID, null);
    goldSpentQuestTracker.add(uniqueID, null);
    shieldsCollectedQuestTracker.add(uniqueID, null);
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
    // Only clear the quest if a quest has been set.
    if (jumpQuestTracker.get(NPCId) != null) {
      jumpQuestTracker.set(NPCId, null);
      questActiveForNPCID.set(NPCId, false);
    }
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
    // Only clear the quest if there's a quest to clear
    if (enemiesKilledQuestTracker.get(NPCId) != null) {
      enemiesKilledQuestTracker.set(NPCId, null);
      questActiveForNPCID.set(NPCId, false);
    }
  }

  // EnemiesKilledQuest functions end
  // GoldSpentQuest function start
  public static boolean logGoldSpentQuest(int NPCId, int amountOfGoldToSpend) {
    if (questActiveForNPCID.get(NPCId)) {
      return false;
    } else {
      goldSpentQuestTracker.set(NPCId, new GoldSpentQuest(amountOfGoldToSpend));
      questActiveForNPCID.set(NPCId, true);
      return true;
    }
  }

  public static int checkGoldSpentQuest(int NPCId) {
    if (goldSpentQuestTracker.get(NPCId) != null) {
      return goldSpentQuestTracker.get(NPCId).checkQuestProgress();
    } else {
      // There is no quest created for that NPCId
      return -1;
    }
  }

  public static void clearGoldSpentQuest(int NPCId) {
    if (goldSpentQuestTracker.get(NPCId) != null) {
      goldSpentQuestTracker.set(NPCId, null);
      questActiveForNPCID.set(NPCId, false);
    }
  }

  // GoldSpentQuest functions end
  // ShieldsCollectedQuest functions start
  public static boolean logShieldsCollectedQuest(int NPCId, int amountOfShieldsToCollect) {
    if (questActiveForNPCID.get(NPCId)) {
      return false;
    } else {
      shieldsCollectedQuestTracker.set(NPCId, new ShieldsCollectedQuest(amountOfShieldsToCollect));
      questActiveForNPCID.set(NPCId, true);
      return true;
    }
  }

  public static int checkShieldsCollectedQuest(int NPCId) {
    if (shieldsCollectedQuestTracker.get(NPCId) != null) {
      return shieldsCollectedQuestTracker.get(NPCId).checkQuestProgress();
    } else {
      // No shieldCollectedQuest was created for that NPC
      return -1;
    }
  }

  public static void clearShieldsCollectedQuest(int NPCId) {
    if (shieldsCollectedQuestTracker.get(NPCId) != null) {
      shieldsCollectedQuestTracker.set(NPCId, null);
      questActiveForNPCID.set(NPCId, false);
    }
  }

  // ShieldsCollectedQuest functions end
  public static ArrayList<JumpQuest> getJumpQuests() {
    return jumpQuestTracker;
  }

  public static ArrayList<EnemiesKilledQuest> getEnemiesKilledQuests() {
    return enemiesKilledQuestTracker;
  }

  public static ArrayList<GoldSpentQuest> getGoldSpentQuests() {
    return goldSpentQuestTracker;
  }

  public static ArrayList<ShieldsCollectedQuest> getShieldsCollectedQuests() {
    return shieldsCollectedQuestTracker;
  }

  public static void incrementGlobalJumps() {
    globalJumps++;
  }

  public static void incrementGlobalEnemiesKilled() {
    globalEnemiesKilled++;
  }

  public static void addGlobalGoldSpent(int amount) {
    globalGoldSpent += amount;
  }

  public static void incrementGlobalShieldsCollected() {
    globalShieldsCollected++;
  }

  public static float getGlobalJumps() {
    return globalJumps;
  }

  public static float getGlobalEnemiesKilled() {
    return globalEnemiesKilled;
  }

  public static float getGlobalGoldSpent() {
    return globalGoldSpent;
  }

  public static float getGlobalShieldsCollected() {
    return globalShieldsCollected;
  }

  public static ArrayList<Integer> getUniqueNPCIDArrayList() {
    return uniqueNPCID;
  }

  public static ArrayList<Boolean> getQuestActiveForNPCID() {
    return questActiveForNPCID;
  }

  // Save/load support start
  public static final String JUMP_QUEST = "jumpquest";
  public static final String ENEMIES_QUEST = "enemiesquest";
  public static final String GOLD_SPENT_QUEST = "goldspentquest";
  public static final String SHIELDS_COLLECTED_QUEST = "shieldscollectedquest";

  /** A quest that is in progress for one NPC, as stored in a save file. */
  public record ActiveQuest(String type, int amountToDo, float snapshot) {}

  /** Clears every NPC id, quest and counter. Used by save/load on a new game or a load. */
  public static void resetAll() {
    uniqueNPCID.clear();
    questActiveForNPCID.clear();
    jumpQuestTracker.clear();
    enemiesKilledQuestTracker.clear();
    goldSpentQuestTracker.clear();
    shieldsCollectedQuestTracker.clear();
    globalJumps = 0;
    globalEnemiesKilled = 0;
    globalGoldSpent = 0;
    globalShieldsCollected = 0;
  }

  /** Returns the four counters in the order jumps, enemies killed, gold spent, shields. */
  public static int[] exportCounters() {
    return new int[] {globalJumps, globalEnemiesKilled, globalGoldSpent, globalShieldsCollected};
  }

  /** Puts the four counters back from a save file. Negative values become zero. */
  public static void restoreCounters(
      int jumps, int enemiesKilled, int goldSpent, int shieldsCollected) {
    globalJumps = Math.max(0, jumps);
    globalEnemiesKilled = Math.max(0, enemiesKilled);
    globalGoldSpent = Math.max(0, goldSpent);
    globalShieldsCollected = Math.max(0, shieldsCollected);
  }

  /** Returns true if this NPC id was given out since the last reset. */
  public static boolean isValidNPCID(int npcId) {
    return npcId >= 0 && npcId < uniqueNPCID.size();
  }

  /** Returns the quest in progress for this NPC, or null if there is none. */
  public static ActiveQuest exportQuest(int npcId) {
    if (!isValidNPCID(npcId)) {
      return null;
    }
    JumpQuest jump = jumpQuestTracker.get(npcId);
    if (jump != null) {
      return new ActiveQuest(JUMP_QUEST, (int) jump.jumpsToDo, jump.globalJumpsSnapshot);
    }
    EnemiesKilledQuest kills = enemiesKilledQuestTracker.get(npcId);
    if (kills != null) {
      return new ActiveQuest(
          ENEMIES_QUEST, (int) kills.enemiesToKill, kills.globalEnemiesKilledSnapshot);
    }
    GoldSpentQuest gold = goldSpentQuestTracker.get(npcId);
    if (gold != null) {
      return new ActiveQuest(GOLD_SPENT_QUEST, (int) gold.goldToSpend, gold.goldSpentSnapshot);
    }
    ShieldsCollectedQuest shields = shieldsCollectedQuestTracker.get(npcId);
    if (shields != null) {
      return new ActiveQuest(
          SHIELDS_COLLECTED_QUEST,
          (int) shields.shieldsToCollect,
          shields.globalShieldsCollectedSnapshot);
    }
    return null;
  }

  /**
   * Puts a saved quest back for this NPC. Returns false and changes nothing if the id, type or
   * amount is not valid, or the NPC already has a quest.
   */
  public static boolean restoreQuest(int npcId, String type, int amountToDo, float snapshot) {
    if (!isValidNPCID(npcId)
        || type == null
        || amountToDo <= 0
        || Boolean.TRUE.equals(questActiveForNPCID.get(npcId))) {
      return false;
    }
    switch (type) {
      case JUMP_QUEST -> {
        JumpQuest quest = new JumpQuest(amountToDo);
        quest.globalJumpsSnapshot = snapshot;
        jumpQuestTracker.set(npcId, quest);
      }
      case ENEMIES_QUEST -> {
        EnemiesKilledQuest quest = new EnemiesKilledQuest(amountToDo);
        quest.globalEnemiesKilledSnapshot = snapshot;
        enemiesKilledQuestTracker.set(npcId, quest);
      }
      case GOLD_SPENT_QUEST -> {
        GoldSpentQuest quest = new GoldSpentQuest(amountToDo);
        quest.goldSpentSnapshot = snapshot;
        goldSpentQuestTracker.set(npcId, quest);
      }
      case SHIELDS_COLLECTED_QUEST -> {
        ShieldsCollectedQuest quest = new ShieldsCollectedQuest(amountToDo);
        quest.globalShieldsCollectedSnapshot = snapshot;
        shieldsCollectedQuestTracker.set(npcId, quest);
      }
      default -> {
        return false;
      }
    }
    questActiveForNPCID.set(npcId, true);
    return true;
  }
  // Save/load support end
}
