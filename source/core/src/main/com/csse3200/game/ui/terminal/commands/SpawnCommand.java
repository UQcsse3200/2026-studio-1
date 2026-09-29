package com.csse3200.game.ui.terminal.commands;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.LevelGameArea;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A command for spawning NPCs */
public class SpawnCommand implements Command {
  private static LevelGameArea levelGameArea;
  private static final Logger logger = LoggerFactory.getLogger(SpawnCommand.class);

  public SpawnCommand(LevelGameArea levelGameArea) {
    SpawnCommand.levelGameArea = levelGameArea;
  }

  /**
   * Spawns an enemy of type defined in map at the players position.
   *
   * @param args command args, valid argument is entity type.
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (!isValid(args)) {
      logger.debug("Invalid arguments received for 'spawn' command: {}", args);
      return false;
    }
    if (levelGameArea.spawnEnemy(args.getFirst(), getPlayerPosition())) {
      logger.info(
          "'spawn' command {}, successfully spawned '{}' at {}",
          args,
          args.getFirst(),
          getPlayerPosition());
      return true;
    }
    logger.debug("Invalid arguments received for 'spawn' command: {}", args);
    return false;
  }

  private Vector2 getPlayerPosition() {
    return levelGameArea != null ? levelGameArea.getPlayer().getPosition() : null;
  }

  /**
   * A method to update the level game area on transition and loading.
   *
   * @param levelGameArea new level game area to spawn enemies in
   */
  public static void updateLevelGameArea(LevelGameArea levelGameArea) {
    SpawnCommand.levelGameArea = levelGameArea;
  }

  /**
   * Validates the command arguments.
   *
   * @param args command arguments
   * @return is valid
   */
  boolean isValid(ArrayList<String> args) {
    if (levelGameArea == null) {
      logger.warn("Level Game Area is null");
      return false;
    }
    return (args.size() == 1);
  }
}
