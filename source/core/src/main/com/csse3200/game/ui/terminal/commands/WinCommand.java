package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.maingame.WinScreenDisplay;
import java.util.ArrayList;

/** A debug command for displaying the win screen. */
public class WinCommand implements Command {
  private final WinScreenDisplay winScreenDisplay;

  public WinCommand(WinScreenDisplay winScreenDisplay) {
    this.winScreenDisplay = winScreenDisplay;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      return false;
    }

    winScreenDisplay.showWinScreen();
    return true;
  }
}
