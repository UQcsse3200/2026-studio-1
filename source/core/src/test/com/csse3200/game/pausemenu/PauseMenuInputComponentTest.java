package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PauseMenuInputComponentTest {
  private PauseMenuComponent pauseMenu;
  private PauseMenuInputComponent inputComponent;
  private boolean[] eventFired;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerInputService(new InputService());

    // toggleIsPaused() pauses/resumes background music via the resource service - stub it
    // out so tests don't need a real Music asset.
    ResourceService resourceService = mock(ResourceService.class);
    Music mockMusic = mock(Music.class);
    when(resourceService.getAsset((PauseMenuComponent.BACKGROUND_MUSIC), (Music.class)))
        .thenReturn(mockMusic);
    ServiceLocator.registerResourceService(resourceService);

    pauseMenu = new PauseMenuComponent();
    inputComponent = new PauseMenuInputComponent();

    Entity entity = new Entity().addComponent(pauseMenu).addComponent(inputComponent);
    entity.create();

    eventFired = new boolean[1];
  }

  private void listenFor(String eventName) {
    inputComponent.getEntity().getEvents().addListener(eventName, () -> eventFired[0] = true);
  }

  @Test
  void shouldNotHandleInputWhenNotPaused() {
    listenFor("navigateUp");
    assertFalse(inputComponent.keyDown(Input.Keys.UP));
    assertFalse(eventFired[0]);
  }

  @Test
  void shouldNotHandleAnyNavigationKeyWhenNotPaused() {
    assertFalse(inputComponent.keyDown(Input.Keys.UP));
    assertFalse(inputComponent.keyDown(Input.Keys.DOWN));
    assertFalse(inputComponent.keyDown(Input.Keys.LEFT));
    assertFalse(inputComponent.keyDown(Input.Keys.RIGHT));
    assertFalse(inputComponent.keyDown(Input.Keys.ENTER));
    assertFalse(inputComponent.keyDown(Input.Keys.SPACE));
  }

  @Test
  void shouldTriggerNavigateUpWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("navigateUp");

    assertTrue(inputComponent.keyDown(Input.Keys.UP));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerNavigateDownWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("navigateDown");

    assertTrue(inputComponent.keyDown(Input.Keys.DOWN));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerNavigateLeftWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("navigateLeft");

    assertTrue(inputComponent.keyDown(Input.Keys.LEFT));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerNavigateRightWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("navigateRight");

    assertTrue(inputComponent.keyDown(Input.Keys.RIGHT));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerConfirmSelectionOnEnterWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("confirmSelection");

    assertTrue(inputComponent.keyDown(Input.Keys.ENTER));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerConfirmSelectionOnSpaceWhenPaused() {
    pauseMenu.toggleIsPaused();
    listenFor("confirmSelection");

    assertTrue(inputComponent.keyDown(Input.Keys.SPACE));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldNotHandleUnrelatedKeysWhenPaused() {
    pauseMenu.toggleIsPaused();
    assertFalse(inputComponent.keyDown(Input.Keys.A));
  }

  @Test
  void shouldStopHandlingInputAfterPausingThenUnpausing() {
    pauseMenu.toggleIsPaused(); // pause
    pauseMenu.toggleIsPaused(); // unpause
    listenFor("navigateUp");

    assertFalse(inputComponent.keyDown(Input.Keys.UP));
    assertFalse(eventFired[0]);
  }
}
