package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.function.Consumer;

/** Debug command for enabling or disabling unlimited player stamina. */
public class StaminaCommand implements Command {
  private final Consumer<Boolean> setUnlimitedStamina;

  public StaminaCommand(Consumer<Boolean> setUnlimitedStamina) {
    this.setUnlimitedStamina = setUnlimitedStamina;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      return false;
    }

    return switch (args.getFirst()) {
      case "on" -> {
        setUnlimitedStamina.accept(true);
        yield true;
      }
      case "off" -> {
        setUnlimitedStamina.accept(false);
        yield true;
      }
      default -> false;
    };
  }
}
