package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.Map;

/** Debug command for teleporting to a named map test location. */
public class TeleportCommand implements Command {
  private final Map<String, Runnable> destinations;

  public TeleportCommand(Map<String, Runnable> destinations) {
    this.destinations = Map.copyOf(destinations);
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1) {
      return false;
    }

    Runnable teleport = destinations.get(args.getFirst());
    if (teleport == null) {
      return false;
    }

    teleport.run();
    return true;
  }
}
