package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.ui.terminal.commands.WhereCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WhereCommandTest {
  private static final WhereCommand.Location LOCATION =
      new WhereCommand.Location("Level 1", "maps/level1-greek.json", "nether", 27, 34, -2f, 1f);

  @Test
  void displaysCurrentRoomPositionAndPhysics() {
    AtomicReference<String> output = new AtomicReference<>();
    WhereCommand command = new WhereCommand(() -> LOCATION, output::set);

    assertTrue(command.action(args()));
    assertTrue(output.get().contains("Room: Level 1"));
    assertTrue(output.get().contains("Map: maps/level1-greek.json"));
    assertTrue(output.get().contains("Sublevel: nether"));
    assertTrue(output.get().contains("Tile: (27, 34)"));
    assertFalse(output.get().contains("Center:"));
    assertTrue(output.get().contains("Vertical speed: -2.00  Gravity scale: 1.00"));
  }

  @Test
  void rejectsArgumentsOrMissingPlayerState() {
    AtomicReference<String> output = new AtomicReference<>();
    WhereCommand command = new WhereCommand(() -> LOCATION, output::set);

    assertFalse(command.action(args("extra")));
    assertFalse(new WhereCommand(() -> null, output::set).action(args()));
    assertNull(output.get());
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
