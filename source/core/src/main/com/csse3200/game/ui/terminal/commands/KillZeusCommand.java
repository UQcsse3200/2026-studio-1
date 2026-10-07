package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * A debug command that kills Zeus, but only when he is alive in the current room. Takes no
 * arguments; any argument makes the command fail without killing anything
 */
public class KillZeusCommand implements Command {
  private final BooleanSupplier killFinalBoss;

  /**
   * Creates the command.
   *
   * @param killFinalBoss kills the final boss and returns true, or returns false if he is not here.
   */
  public KillZeusCommand(BooleanSupplier killFinalBoss) {
    this.killFinalBoss = killFinalBoss;
  }

  /**
   * Runs the command
   *
   * @param args the word typed after the command name; must be empty.
   * @return true if the final boss was killed.
   */
  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      return false;
    }
    return killFinalBoss.getAsBoolean();
  }
}
