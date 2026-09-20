package com.csse3200.game.files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSaveData {
  public int health;
  public int gold;
  public List<SavedItem> items = new ArrayList<>();
  public float posX;
  public float posY;
  public Map<String, Long> lootSeedsByRoom = new HashMap<>();
  public List<String> collectedLootIds = new ArrayList<>();
  public String level;
}
