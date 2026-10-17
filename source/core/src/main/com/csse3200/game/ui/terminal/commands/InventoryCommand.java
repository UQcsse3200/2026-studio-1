package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.player.InventoryDisplay;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A debug command that hides or shows the player's inventory panel. Type {@code hide inventory on}
 * to hide it and {@code hide inventory off} to show it again.
 */
public class InventoryCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(InventoryCommand.class);
  private static final String USAGE = "Usage: hide inventory on | off";

  private final Supplier<Entity> playerSupplier;

  /**
   * Creates the command.
   *
   * @param playerSupplier gives the current player each time the command runs, or null if there is
   *     no player yet
   */
  public InventoryCommand(Supplier<Entity> playerSupplier) {
    this.playerSupplier = playerSupplier;
  }

  /**
   * Hides or shows the inventory panel.
   *
   * @param args the words typed after {@code hide}; must be {@code inventory} then {@code on} or
   *     {@code off}
   * @return true if the panel was changed
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (!isValid(args)) {
      logger.warn("{} (got {})", USAGE, args);
      return false;
    }

    boolean shouldHide;
    if (args.getLast().equals("on")) {
      shouldHide = true;
    } else if (args.getLast().equals("off")) {
      shouldHide = false;
    } else {
      logger.warn("{} (got {})", USAGE, args);
      return false;
    }

    Entity player = playerSupplier.get();
    if (player == null) {
      logger.warn("hide inventory: there is no player yet");
      return false;
    }

    InventoryDisplay display = player.getComponent(InventoryDisplay.class);
    if (display == null) {
      logger.warn("hide inventory: the player has no inventory display");
      return false;
    }

    display.setHidden(shouldHide);
    logger.info("Inventory panel {}", shouldHide ? "hidden" : "shown");
    return true;
  }

  /**
   * Checks the command was typed as {@code hide inventory <something>}.
   *
   * @param args the words typed after {@code hide}
   * @return true if there are two words and the first is {@code inventory}
   */
  boolean isValid(ArrayList<String> args) {
    if (args.size() != 2) {
      return false;
    }
    return args.getFirst().equals("inventory");
  }
}
