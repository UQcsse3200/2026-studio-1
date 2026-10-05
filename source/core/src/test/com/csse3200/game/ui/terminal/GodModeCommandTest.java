package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.ui.terminal.commands.GodModeCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GodModeCommandTest {
  @Test
  void enablesAndDisablesGodMode() {
    AtomicReference<Boolean> enabled = new AtomicReference<>();
    GodModeCommand command = new GodModeCommand(enabled::set);

    assertTrue(command.action(args("on")));
    assertEquals(true, enabled.get());

    assertTrue(command.action(args("off")));
    assertEquals(false, enabled.get());
  }

  @Test
  void rejectsUnknownOrIncorrectArguments() {
    AtomicReference<Boolean> enabled = new AtomicReference<>();
    GodModeCommand command = new GodModeCommand(enabled::set);

    assertFalse(command.action(args()));
    assertFalse(command.action(args("toggle")));
    assertFalse(command.action(args("on", "extra")));
    assertNull(enabled.get());
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
