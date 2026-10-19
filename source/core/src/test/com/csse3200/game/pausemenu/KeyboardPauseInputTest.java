package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.Input;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests the pause key, and how keys are passed on or blocked while the game is paused. */
@ExtendWith(GameExtension.class)
class KeyboardPauseInputTest {
  private Entity ui;
  private PauseMenuComponent pauseMenu;
  private KeyboardPauseInput input;
  private List<String> events;
  private int pauseKey;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    pauseKey = KeybindSettings.getKey("pause");
    pauseMenu = new PauseMenuComponent();
    input = new KeyboardPauseInput();
    ui = new Entity().addComponent(pauseMenu).addComponent(input);
    ui.create();

    events = new ArrayList<>();
    ui.getEvents().addListener("escapePressed", () -> events.add("escapePressed"));
    ui.getEvents()
        .addListener("keybindCaptureCancelled", () -> events.add("keybindCaptureCancelled"));
    ui.getEvents().addListener("keybindCaptured", (Integer key) -> events.add("captured:" + key));
  }

  @AfterEach
  void tearDown() {
    // The paused flag is static, so clear it or later tests would start paused.
    PauseMenuComponent.isGamePaused().set(false);
  }

  @Test
  void pauseKeyOpensThePauseMenu() {
    assertTrue(input.keyDown(pauseKey));

    assertTrue(pauseMenu.isPaused());
    assertTrue(PauseMenuComponent.isGamePaused().get());
  }

  @Test
  void pauseKeyWhilePausedSendsEscapeToTheMenu() {
    input.keyDown(pauseKey);

    assertTrue(input.keyDown(pauseKey));

    assertTrue(pauseMenu.isPaused(), "the menu decides what escape does, it is not closed here");
    assertEquals(List.of("escapePressed"), events);
  }

  @Test
  void otherKeysPassThroughDuringNormalPlay() {
    assertFalse(input.keyDown(Input.Keys.A));
    assertFalse(input.keyUp(Input.Keys.A));
    assertFalse(pauseMenu.isPaused());
  }

  @Test
  void menuKeysPassThroughToTheMenuWhilePaused() {
    input.keyDown(pauseKey);

    for (int key :
        new int[] {
          Input.Keys.UP,
          Input.Keys.DOWN,
          Input.Keys.LEFT,
          Input.Keys.RIGHT,
          Input.Keys.ENTER,
          Input.Keys.SPACE
        }) {
      assertFalse(input.keyDown(key));
      assertFalse(input.keyUp(key));
    }
  }

  @Test
  void gameKeysAreBlockedWhilePaused() {
    input.keyDown(pauseKey);

    assertTrue(input.keyDown(Input.Keys.A), "the player must not move while paused");
    assertTrue(input.keyUp(Input.Keys.A));
  }

  @Test
  void anyKeyIsCapturedWhileRebinding() {
    pauseMenu.setCapturingKeybind(true);

    assertTrue(input.keyDown(Input.Keys.K));

    assertEquals(List.of("captured:" + Input.Keys.K), events);
  }

  @Test
  void escapeCancelsRebindingInsteadOfBeingBound() {
    pauseMenu.setCapturingKeybind(true);

    assertTrue(input.keyDown(Input.Keys.ESCAPE));

    assertEquals(List.of("keybindCaptureCancelled"), events);
  }

  @Test
  void keyUpIsSwallowedWhileRebinding() {
    pauseMenu.setCapturingKeybind(true);

    assertTrue(input.keyUp(Input.Keys.K));
  }

  @Test
  void pauseKeyDoesNothingWhileTheDeathOrWinScreenIsShowing() {
    input.setPauseBlocked(() -> true);

    assertFalse(input.keyDown(pauseKey));

    assertFalse(pauseMenu.isPaused());
  }

  @Test
  void pauseWorksAgainOnceTheScreenCloses() {
    boolean[] blocked = {true};
    input.setPauseBlocked(() -> blocked[0]);
    input.keyDown(pauseKey);

    blocked[0] = false;

    assertTrue(input.keyDown(pauseKey));
    assertTrue(pauseMenu.isPaused());
  }

  @Test
  void nullCheckNeverBlocksPause() {
    input.setPauseBlocked(null);

    assertTrue(input.keyDown(pauseKey));
    assertTrue(pauseMenu.isPaused());
  }
}
