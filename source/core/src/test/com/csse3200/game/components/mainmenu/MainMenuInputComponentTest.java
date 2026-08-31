package com.csse3200.game.components.mainmenu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class MainMenuInputComponentTest {
  private MainMenuInputComponent inputComponent;
  private boolean[] eventFired;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerInputService(new InputService());

    inputComponent = new MainMenuInputComponent();
    Entity entity = new Entity().addComponent(inputComponent);
    entity.create();

    eventFired = new boolean[1];
  }

  private void listenFor(String eventName) {
    inputComponent.getEntity().getEvents().addListener(eventName, () -> eventFired[0] = true);
  }

  @Test
  void shouldTriggerNavigateUp() {
    listenFor("navigateUp");

    assertTrue(inputComponent.keyDown(Input.Keys.UP));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerNavigateDown() {
    listenFor("navigateDown");

    assertTrue(inputComponent.keyDown(Input.Keys.DOWN));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerConfirmSelectionOnEnter() {
    listenFor("confirmSelection");

    assertTrue(inputComponent.keyDown(Input.Keys.ENTER));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldTriggerConfirmSelectionOnSpace() {
    listenFor("confirmSelection");

    assertTrue(inputComponent.keyDown(Input.Keys.SPACE));
    assertTrue(eventFired[0]);
  }

  @Test
  void shouldNotHandleUnrelatedKeys() {
    assertFalse(inputComponent.keyDown(Input.Keys.A));
  }
}
