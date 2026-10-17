package com.csse3200.game.files;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.files.FileLoader.Location;
import java.io.File;

/**
 * Reads and writes the single save file, {@code AscentOfTheMortal/save.json} in the user's home
 * directory. Building the save data and applying it to the game is done by {@code PauseMenuActions}
 * and {@link LoadService}.
 */
public final class SaveService {
  private static final String ROOT_DIR = "AscentOfTheMortal";
  private static final String SAVE_FILE = "save.json";

  /** Path of the save file, relative to the user's home directory. */
  private static final String SAVE_PATH = ROOT_DIR + File.separator + SAVE_FILE;

  private SaveService() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Writes the given data to the save file as JSON, replacing any previous save.
   *
   * @param data the game state to save
   */
  public static void save(GameSaveData data) {
    FileLoader.writeClass(data, SAVE_PATH, Location.EXTERNAL);
  }

  /**
   * @return true if a save file exists
   */
  public static boolean hasSave() {
    return Gdx.files != null && Gdx.files.external(SAVE_PATH).exists();
  }

  /**
   * Reads the save file.
   *
   * @return the saved data, or an empty {@link GameSaveData} if the file is missing or cannot be
   *     read
   */
  public static GameSaveData load() {
    GameSaveData data = FileLoader.readClass(GameSaveData.class, SAVE_PATH, Location.EXTERNAL);
    return data != null ? data : new GameSaveData();
  }
}
