package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KillZeusCommandTest {

  @Test
  void shouldSucceedWhenTheFinalBossWasKilled() {
    KillZeusCommand command = new KillZeusCommand(() -> true);

    assertTrue(command.action(new ArrayList<>()));
  }

  @Test
  void shouldFailWhenTheFinalBossIsNotHere() {
    KillZeusCommand command = new KillZeusCommand(() -> false);

    assertFalse(command.action(new ArrayList<>()));
  }

  @Test
  void shouldRejectArgumentsWithoutKillingAnything() {
    AtomicInteger kills = new AtomicInteger();
    KillZeusCommand command =
        new KillZeusCommand(
            () -> {
              kills.incrementAndGet();
              return true;
            });

    assertFalse(command.action(new ArrayList<>(List.of("now"))));
    assertEquals(0, kills.get());
  }

  @Test
  void shouldAskOnceEachTimeItRuns() {
    AtomicInteger kills = new AtomicInteger();
    KillZeusCommand command =
        new KillZeusCommand(
            () -> {
              kills.incrementAndGet();
              return true;
            });

    command.action(new ArrayList<>());
    command.action(new ArrayList<>());

    assertEquals(2, kills.get());
  }
}
