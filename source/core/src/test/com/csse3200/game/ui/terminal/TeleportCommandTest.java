package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.ui.terminal.commands.TeleportCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class TeleportCommandTest {
  @Test
  void teleportsToARegisteredDestination() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command = new TeleportCommand(Map.of("lvl2", () -> teleported.set(true)));

    assertTrue(command.action(args("lvl2")));
    assertTrue(teleported.get());
  }

  @Test
  void rejectsUnknownOrIncorrectArguments() {
    AtomicBoolean teleported = new AtomicBoolean();
    TeleportCommand command = new TeleportCommand(Map.of("lvl2", () -> teleported.set(true)));

    assertFalse(command.action(args()));
    assertFalse(command.action(args("secret-loot")));
    assertFalse(command.action(args("lvl2", "extra")));
    assertFalse(teleported.get());
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
