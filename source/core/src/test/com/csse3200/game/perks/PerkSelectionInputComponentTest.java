package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PerkSelectionInputComponentTest {
  private static final String[] EVENTS = {
    "perkSelectNavigateLeft",
    "perkSelectNavigateRight",
    "perkSelectNavigateDown",
    "perkSelectNavigateUp",
    "perkSelectConfirm"
  };

  private PerkSelectionInputComponent input;
  private PerkSelectionDisplay display;
  private Entity entity;
  private AtomicInteger[] counts;

  @BeforeEach
  void setUp() throws Exception {
    entity = new Entity();
    input = new PerkSelectionInputComponent();
    entity.addComponent(input); // sets the entity; create() isn't run, so no InputService needed

    display = mock(PerkSelectionDisplay.class);
    when(display.isVisible()).thenReturn(true);
    setDisplay(display);

    counts = new AtomicInteger[EVENTS.length];
    for (int i = 0; i < EVENTS.length; i++) {
      AtomicInteger counter = new AtomicInteger();
      counts[i] = counter;
      entity.getEvents().addListener(EVENTS[i], counter::incrementAndGet);
    }
  }

  private void setDisplay(PerkSelectionDisplay value) throws Exception {
    Field f = PerkSelectionInputComponent.class.getDeclaredField("perkSelectionDisplay");
    f.setAccessible(true);
    f.set(input, value);
  }

  /** Asserts exactly the event at {@code expectedIndex} fired once (or none, if -1). */
  private void assertOnlyFired(int expectedIndex) {
    for (int i = 0; i < counts.length; i++) {
      assertEquals(i == expectedIndex ? 1 : 0, counts[i].get(), EVENTS[i]);
    }
  }

  @Test
  void leftKeyTriggersNavigateLeft() {
    assertTrue(input.keyDown(Input.Keys.LEFT));
    assertOnlyFired(0);
  }

  @Test
  void rightKeyTriggersNavigateRight() {
    assertTrue(input.keyDown(Input.Keys.RIGHT));
    assertOnlyFired(1);
  }

  @Test
  void downKeyTriggersNavigateDown() {
    assertTrue(input.keyDown(Input.Keys.DOWN));
    assertOnlyFired(2);
  }

  @Test
  void upKeyTriggersNavigateUp() {
    assertTrue(input.keyDown(Input.Keys.UP));
    assertOnlyFired(3);
  }

  @Test
  void enterKeyTriggersConfirm() {
    assertTrue(input.keyDown(Input.Keys.ENTER));
    assertOnlyFired(4);
  }

  @Test
  void spaceKeyTriggersConfirm() {
    assertTrue(input.keyDown(Input.Keys.SPACE));
    assertOnlyFired(4);
  }

  @Test
  void unrelatedKeyIsIgnored() {
    assertFalse(input.keyDown(Input.Keys.A));
    assertOnlyFired(-1);
  }

  @Test
  void keysAreIgnoredWhenDisplayNotVisible() {
    when(display.isVisible()).thenReturn(false);

    assertFalse(input.keyDown(Input.Keys.LEFT));
    assertFalse(input.keyDown(Input.Keys.ENTER));
    assertOnlyFired(-1);
  }

  @Test
  void keysAreIgnoredWhenDisplayIsNull() throws Exception {
    setDisplay(null);

    assertFalse(input.keyDown(Input.Keys.LEFT));
    assertOnlyFired(-1);
  }
}
