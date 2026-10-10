package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.player.InventoryDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(GameExtension.class)
class InventoryCommandTest {
  private Entity player;
  private InventoryDisplay display;
  private InventoryCommand command;

  @BeforeEach
  void setUp() {
    display = mock(InventoryDisplay.class);
    player = mock(Entity.class);
    when(player.getComponent(InventoryDisplay.class)).thenReturn(display);
    command = new InventoryCommand(() -> player);
  }

  @Test
  void hidesTheInventoryWhenToldOn() {
    assertTrue(command.action(args("inventory", "on")));

    verify(display).setHidden(true);
  }

  @Test
  void showsTheInventoryWhenToldOff() {
    assertTrue(command.action(args("inventory", "off")));

    verify(display).setHidden(false);
  }

  @Test
  void rejectsNoWordsAtAll() {
    assertFalse(command.action(args()));

    verify(display, never()).setHidden(anyBoolean());
  }

  @Test
  void rejectsOneWord() {
    assertFalse(command.action(args("inventory")));

    verify(display, never()).setHidden(anyBoolean());
  }

  @Test
  void rejectsThreeWords() {
    assertFalse(command.action(args("inventory", "on", "now")));

    verify(display, never()).setHidden(anyBoolean());
  }

  @Test
  void rejectsAFirstWordThatIsNotInventory() {
    assertFalse(command.action(args("shop", "on")));

    verify(display, never()).setHidden(anyBoolean());
  }

  @ParameterizedTest(name = "hide inventory {0}")
  @ValueSource(strings = {"On", "OFF", "yes", "true", ""})
  void rejectsASecondWordThatIsNotOnOrOff(String word) {
    assertFalse(command.action(args("inventory", word)));

    verify(display, never()).setHidden(anyBoolean());
  }

  @Test
  void doesNotLookForThePlayerWhenTheWordsAreWrong() {
    AtomicInteger asked = new AtomicInteger();
    InventoryCommand counting =
        new InventoryCommand(
            () -> {
              asked.incrementAndGet();
              return player;
            });

    counting.action(args("inventory"));
    counting.action(args("inventory", "maybe"));

    assertEquals(0, asked.get());
  }

  @Test
  void failsWhenThereIsNoPlayer() {
    InventoryCommand noPlayer = new InventoryCommand(() -> null);

    assertFalse(noPlayer.action(args("inventory", "on")));
  }

  @Test
  void failsWhenThePlayerHasNoInventoryDisplay() {
    when(player.getComponent(InventoryDisplay.class)).thenReturn(null);

    assertFalse(command.action(args("inventory", "on")));

    verify(display, never()).setHidden(anyBoolean());
  }

  @Test
  void usesTheCurrentPlayerEachTimeItRuns() {
    // After a respawn the game has a new player with a new display.
    InventoryDisplay secondDisplay = mock(InventoryDisplay.class);
    Entity secondPlayer = mock(Entity.class);
    when(secondPlayer.getComponent(InventoryDisplay.class)).thenReturn(secondDisplay);
    Entity[] current = {player};
    InventoryCommand following = new InventoryCommand(() -> current[0]);

    following.action(args("inventory", "on"));
    current[0] = secondPlayer;
    following.action(args("inventory", "off"));

    verify(display).setHidden(true);
    verify(secondDisplay).setHidden(false);
  }

  @Test
  void isValidNeedsTwoWordsStartingWithInventory() {
    assertTrue(command.isValid(args("inventory", "on")));
    assertTrue(command.isValid(args("inventory", "anything")));
    assertFalse(command.isValid(args()));
    assertFalse(command.isValid(args("inventory")));
    assertFalse(command.isValid(args("on", "inventory")));
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
