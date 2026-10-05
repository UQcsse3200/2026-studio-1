package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.function.Consumer;

/** Debug command for enabling or disabling player damage immunity. */
public class GodModeCommand implements Command {
  private final Consumer<Boolean> setGodModeEnabled;

  public GodModeCommand(Consumer<Boolean> setGodModeEnabled) {
    this.setGodModeEnabled = setGodModeEnabled;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      return false;
    }

    return switch (args.getFirst()) {
      case "on" -> {
        setGodModeEnabled.accept(true);
        yield true;
      }
      case "off" -> {
        setGodModeEnabled.accept(false);
        yield true;
      }
      default -> false;
    };
  }
}
