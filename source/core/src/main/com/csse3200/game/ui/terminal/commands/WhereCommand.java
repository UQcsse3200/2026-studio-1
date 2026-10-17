package com.csse3200.game.ui.terminal.commands;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Displays the player's current room, position, and vertical physics state. */
public class WhereCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(WhereCommand.class);

  private final Supplier<Location> getLocation;
  private final Consumer<String> showOutput;

  public WhereCommand(Supplier<Location> getLocation, Consumer<String> showOutput) {
    this.getLocation = getLocation;
    this.showOutput = showOutput;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (!args.isEmpty()) {
      return false;
    }

    Location location = getLocation.get();
    if (location == null) {
      return false;
    }

    String output =
        String.format(
            Locale.ROOT,
            "Room: %s%nMap: %s%nSublevel: %s%nTile: (%d, %d)%n"
                + "Vertical speed: %.2f  Gravity scale: %.2f",
            location.roomName(),
            location.mapPath(),
            location.sublevel(),
            location.tileX(),
            location.tileY(),
            location.verticalVelocity(),
            location.gravityScale());
    showOutput.accept(output);
    logger.info("{}", output);
    return true;
  }

  public record Location(
      String roomName,
      String mapPath,
      String sublevel,
      int tileX,
      int tileY,
      float verticalVelocity,
      float gravityScale) {}
}
