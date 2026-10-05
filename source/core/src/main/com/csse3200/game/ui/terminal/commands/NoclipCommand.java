package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.function.Consumer;

/** Debug command for enabling or disabling player collision and gravity. */
public class NoclipCommand implements Command {
  private final Consumer<Boolean> setNoclipEnabled;

  public NoclipCommand(Consumer<Boolean> setNoclipEnabled) {
    this.setNoclipEnabled = setNoclipEnabled;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      return false;
    }

    return switch (args.getFirst()) {
      case "on" -> {
        setNoclipEnabled.accept(true);
        yield true;
      }
      case "off" -> {
        setNoclipEnabled.accept(false);
        yield true;
      }
      default -> false;
    };
  }
}
