package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NoclipInputComponentTest {
  @Test
  void controlsVerticalDirectionOnlyWhileEnabled() {
    AtomicReference<Double> direction = new AtomicReference<>(0d);
    NoclipInputComponent input = new NoclipInputComponent(direction::set);

    assertFalse(input.keyDown(Input.Keys.W));
    assertEquals(0d, direction.get());

    input.setNoclipEnabled(true);

    assertTrue(input.keyDown(Input.Keys.W));
    assertEquals(1d, direction.get());

    assertTrue(input.keyDown(Input.Keys.S));
    assertEquals(0d, direction.get());

    assertTrue(input.keyUp(Input.Keys.W));
    assertEquals(-1d, direction.get());

    assertTrue(input.keyUp(Input.Keys.S));
    assertEquals(0d, direction.get());
  }

  @Test
  void leavesHorizontalMovementForTheNormalPlayerInput() {
    NoclipInputComponent input = new NoclipInputComponent(direction -> {});
    input.setNoclipEnabled(true);

    assertFalse(input.keyDown(Input.Keys.A));
    assertFalse(input.keyDown(Input.Keys.D));
    assertFalse(input.keyUp(Input.Keys.A));
    assertFalse(input.keyUp(Input.Keys.D));
  }

  @Test
  void disablingNoclipStopsVerticalMovementAndRestoresNormalInput() {
    AtomicReference<Double> direction = new AtomicReference<>(0d);
    NoclipInputComponent input = new NoclipInputComponent(direction::set);
    input.setNoclipEnabled(true);
    input.keyDown(Input.Keys.W);

    input.setNoclipEnabled(false);

    assertEquals(0d, direction.get());
    assertFalse(input.keyDown(Input.Keys.W));
    assertFalse(input.keyDown(Input.Keys.S));
  }
}
