package com.csse3200.game.components.difficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Tests keyboard control of the difficulty selection screen. */
@ExtendWith(GameExtension.class)
class DifficultySelectInputComponentTest {
  private DifficultySelectInputComponent input;
  private List<String> events;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerInputService(new InputService());

    input = new DifficultySelectInputComponent();
    Entity entity = new Entity().addComponent(input);
    entity.create();

    events = new ArrayList<>();
    for (String name : List.of("navigateUp", "navigateDown", "confirmSelection", "back")) {
      entity.getEvents().addListener(name, () -> events.add(name));
    }
  }

  @Test
  void upMovesTheSelectionUp() {
    assertTrue(input.keyDown(Input.Keys.UP));
    assertEquals(List.of("navigateUp"), events);
  }

  @Test
  void downMovesTheSelectionDown() {
    assertTrue(input.keyDown(Input.Keys.DOWN));
    assertEquals(List.of("navigateDown"), events);
  }

  @Test
  void enterConfirmsTheSelection() {
    assertTrue(input.keyDown(Input.Keys.ENTER));
    assertEquals(List.of("confirmSelection"), events);
  }

  @Test
  void spaceAlsoConfirmsTheSelection() {
    assertTrue(input.keyDown(Input.Keys.SPACE));
    assertEquals(List.of("confirmSelection"), events);
  }

  @Test
  void escapeGoesBack() {
    assertTrue(input.keyDown(Input.Keys.ESCAPE));
    assertEquals(List.of("back"), events);
  }

  @Test
  void otherKeysAreNotHandled() {
    assertFalse(input.keyDown(Input.Keys.A));
    assertFalse(input.keyDown(Input.Keys.LEFT));
    assertTrue(events.isEmpty());
  }
}
