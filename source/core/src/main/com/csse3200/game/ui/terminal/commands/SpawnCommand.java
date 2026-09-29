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

  /**
   * Spawns an enemy at the players position unless specified coordinates are given. Enemy types
   *
   * @param args command args
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (!isValid(args)) {
      logger.debug("Invalid arguments received for 'spawn' command: {}", args);
      return false;
    }
    Vector2 position = new Vector2();
    if (args.size() > 1) {
      try {
        position.x = getPosition(args.get(1), true);
        position.y = getPosition(args.get(2), false);
      } catch (NumberFormatException e) {
        logger.debug("Invalid arguments received for 'spawn' command: {}", args);
        return false;
      }
    } else {
      position.x = getPosition(relativeDelimiter, true);
      position.y = getPosition(relativeDelimiter, false);
    }

    // Should be in each level, so that an enemy can be spawned regardless of level.

    return true;
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
