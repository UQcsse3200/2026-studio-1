package com.csse3200.game.ui.terminal.commands;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.events.EventHandler;
import java.util.ArrayList;
import java.util.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A command for spawning NPCs */
public class SpawnCommand implements Command, EventListener {
  private static final String relativeDelimiter = "r";

  /**
   * Gets player position by triggering an event "getPlayerPosition" which is listened to by
   * PlayerActions.java, which then itself sends another event "sendPlayerPosition" with the
   * player's position, which is listened to back here.
   */
  private static Vector2 playerPosition = new Vector2();

  private static final Logger logger = LoggerFactory.getLogger(SpawnCommand.class);
  private static final EventHandler eventHandler = new EventHandler();

  static {
    eventHandler.addListener("sendPlayerPosition", SpawnCommand::updatePlayerPosition);
  }

  /**
   * Spawns an enemy at the players position unless specified coordinates are given. Enemy types
   *
   * @param args command args
   */
  @Override
  public boolean action(ArrayList<String> args) {
    eventHandler.trigger("getPlayerPosition");
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
    eventHandler.trigger("debugSpawnEnemy", args.getFirst(), position);

    return true;
  }

  /**
   * Converts a string into its float value with an optional relative modifier.
   *
   * @param arg string argument
   * @param isHorizontal is x coordinate
   * @exception NumberFormatException on invalid suffix to relative modifier or non number.
   * @return position of arg
   */
  private float getPosition(String arg, boolean isHorizontal) {
    String isolatedValueArg = arg;
    float position = 0;
    if (arg.startsWith(relativeDelimiter)) {
      isolatedValueArg = arg.substring(relativeDelimiter.length());
      position = isHorizontal ? playerPosition.x : playerPosition.y;
    }
    if (!isolatedValueArg.isEmpty()) {
      position += Float.parseFloat(isolatedValueArg);
    }
    System.out.println("argPosition: " + arg + " = " + position);

    return position;
  }

  /**
   * Gets value from event "sendPlayerPosition" called from getPlayerPosition() in
   * PlaterActions.java
   */
  private void updatePlayerPosition(Vector2 position) {
    System.out.println("updatedPlayerPosition: " + position);
    playerPosition.set(position);
  }

  /**
   * Validates the command arguments.
   *
   * @param args command arguments
   * @return is valid
   */
  boolean isValid(ArrayList<String> args) {
    System.out.println("isValid: " + (args.size() == 1 || args.size() == 3));
    return (args.size() == 1 || args.size() == 3);
  }
}
