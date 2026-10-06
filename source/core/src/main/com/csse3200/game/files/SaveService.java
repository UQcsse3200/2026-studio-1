package com.csse3200.game.files;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.files.FileLoader.Location;
import java.io.File;

public class SaveService {
  private static final String ROOT_DIR = "AscentOfTheMortal";
  private static final String SAVE_FILE = "save.json";

  public static void save(GameSaveData data) {
    String path = ROOT_DIR + File.separator + SAVE_FILE;
    FileLoader.writeClass(data, path, Location.EXTERNAL);
  }

  public static boolean hasSave() {
    String path = ROOT_DIR + File.separator + SAVE_FILE;
    return Gdx.files != null && Gdx.files.external(path).exists();
  }

  public static GameSaveData load() {
    String path = ROOT_DIR + File.separator + SAVE_FILE;
    GameSaveData data = FileLoader.readClass(GameSaveData.class, path, Location.EXTERNAL);
    return data != null ? data : new GameSaveData();
  }
}
