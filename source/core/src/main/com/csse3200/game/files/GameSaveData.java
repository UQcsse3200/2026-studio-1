package com.csse3200.game.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {
  public int health;
  public float stamina = 100f;
  public int gold;
  public List<SavedItem> items = new ArrayList<>();
  public int activeSlot = 1;
  public float posX;
  public float posY;
  public Map<String, Long> lootSeedsByRoom = new HashMap<>();
  public List<String> collectedLootIds = new ArrayList<>();
  public String level;
  public List<String> killedEnemyIds = new ArrayList<>();
  public List<String> ownedPetNames = new ArrayList<>();
  public String activePetName;
  public String difficulty;
  public List<SavedUpgrade> upgrades = new ArrayList<>();
  public Integer shieldHits;
  public List<SavedBuff> buffs = new ArrayList<>();
  public int regenHealPerTick;
  public float regenRemainingSeconds;
  public List<SavedLoot> droppedLoot = new ArrayList<>();

  /** How many quests of each kind have been completed, for the win screen. */
  public Map<String, Integer> completedQuestsByKind = new HashMap<>();

  /** The ids of the hidden tortoises found so far, for the win screen. */
  public List<String> foundTortoiseIds = new ArrayList<>();

  /** Quest state of every friendly NPC met so far. */
  public List<SavedNpc> npcs = new ArrayList<>();

  /** Ids of friendly NPCs that were killed. */
  public List<String> killedNpcIds = new ArrayList<>();

  /** Quest counters, so quests in progress carry on from the right point. */
  public int questJumps;

  public int questEnemiesKilled;
  public int questGoldSpent;
  public int questShieldsCollected;

  /** Whether a shield was picked up but not used yet. */
  public boolean shieldHeld;

  /** Time left on an active shield, in milliseconds. Zero means not active. */
  public long shieldRemainingMillis;

  /** Whether a Ballistic Shield was picked up but not used yet. */
  public boolean ballisticShieldHeld;

  /** Time left on an active Ballistic Shield, in milliseconds. Zero means not active. */
  public long ballisticShieldRemainingMillis;
}
