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

/** Tests keyboard navigation of the open pause menu. Keys are ignored while it is closed. */
@ExtendWith(GameExtension.class)
class PauseMenuInputComponentTest {
  private Entity ui;
  private PauseMenuComponent pauseMenu;
  private PauseMenuInputComponent input;
  private List<String> events;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    pauseMenu = new PauseMenuComponent();
    input = new PauseMenuInputComponent();
    ui = new Entity().addComponent(pauseMenu).addComponent(input);
    ui.create();

    events = new ArrayList<>();
    for (String name :
        List.of(
            "navigateUp",
            "navigateDown",
            "navigateLeft",
            "navigateRight",
            "leftPressed",
            "rightPressed",
            "leftReleased",
            "rightReleased",
            "confirmSelection")) {
      ui.getEvents().addListener(name, () -> events.add(name));
    }
    ui.getEvents().addListener("keybindCaptured", (Integer key) -> events.add("captured:" + key));
  }

  private void openPauseMenu() {
    pauseMenu.toggleIsPaused();
  }

  @AfterEach
  void tearDown() {
    // The paused flag is static, so clear it or later tests would start paused.
    PauseMenuComponent.isGamePaused().set(false);
  }

  @Test
  void keysAreIgnoredWhileThePauseMenuIsClosed() {
    assertFalse(input.keyDown(Input.Keys.UP));
    assertFalse(input.keyDown(Input.Keys.ENTER));
    assertFalse(input.keyUp(Input.Keys.LEFT));
    assertTrue(events.isEmpty());
  }

  @Test
  void keysAreIgnoredWithoutAPauseMenuComponent() {
    Entity other = new Entity().addComponent(new PauseMenuInputComponent());
    other.create();
    PauseMenuInputComponent orphan = other.getComponent(PauseMenuInputComponent.class);

    assertFalse(orphan.keyDown(Input.Keys.UP));
    assertFalse(orphan.keyUp(Input.Keys.LEFT));
  }

  @Test
  void upAndDownNavigateTheMenu() {
    openPauseMenu();

    assertTrue(input.keyDown(Input.Keys.UP));
    assertTrue(input.keyDown(Input.Keys.DOWN));

    assertEquals(List.of("navigateUp", "navigateDown"), events);
  }

  @Test
  void leftAndRightNavigateAndStartHolding() {
    openPauseMenu();

    assertTrue(input.keyDown(Input.Keys.LEFT));
    assertTrue(input.keyDown(Input.Keys.RIGHT));

    assertEquals(List.of("navigateLeft", "leftPressed", "navigateRight", "rightPressed"), events);
  }

  @Test
  void enterAndSpaceConfirmTheSelection() {
    openPauseMenu();

    assertTrue(input.keyDown(Input.Keys.ENTER));
    assertTrue(input.keyDown(Input.Keys.SPACE));

    assertEquals(List.of("confirmSelection", "confirmSelection"), events);
  }

  @Test
  void otherKeysAreNotHandled() {
    openPauseMenu();

    assertFalse(input.keyDown(Input.Keys.A));
    assertFalse(input.keyUp(Input.Keys.A));
    assertTrue(events.isEmpty());
  }

  @Test
  void releasingLeftAndRightStopsHolding() {
    openPauseMenu();

    assertTrue(input.keyUp(Input.Keys.LEFT));
    assertTrue(input.keyUp(Input.Keys.RIGHT));

    assertEquals(List.of("leftReleased", "rightReleased"), events);
  }

  @Test
  void arrowKeysDoNotNavigateWhileCapturingAKeybind() {
    openPauseMenu();
    pauseMenu.setCapturingKeybind(true);

    assertFalse(input.keyDown(Input.Keys.UP));
    assertFalse(input.keyDown(Input.Keys.DOWN));
    assertFalse(input.keyDown(Input.Keys.LEFT));
    assertFalse(input.keyDown(Input.Keys.RIGHT));
    assertTrue(events.isEmpty());
  }

  @Test
  void enterIsCapturedAsTheNewKeyWhileCapturingAKeybind() {
    openPauseMenu();
    pauseMenu.setCapturingKeybind(true);

    assertTrue(input.keyDown(Input.Keys.ENTER));

    assertEquals(List.of("captured:" + Input.Keys.ENTER), events);
  }
}
