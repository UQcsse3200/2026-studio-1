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
}
