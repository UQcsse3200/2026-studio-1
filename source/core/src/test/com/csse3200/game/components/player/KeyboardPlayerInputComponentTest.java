package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input.Keys;
import com.csse3200.game.entities.Entity;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KeyboardPlayerInputComponentTest {

  @Test
  void shouldTriggerDropEventWhenQIsPressed() {
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player = new Entity().addComponent(input);
    AtomicInteger drops = new AtomicInteger();
    player.getEvents().addListener("dropItem", drops::incrementAndGet);

    assertTrue(input.keyDown(Keys.Q));
    assertEquals(1, drops.get());
  }

  /** Acceptance criterion: pressing the shield key fires the shield activation event. */
  @Test
  void shouldTriggerActivateShieldEventWhenBIsPressed() {
    KeyboardPlayerInputComponent input = new KeyboardPlayerInputComponent();
    Entity player = new Entity().addComponent(input);
    AtomicInteger activations = new AtomicInteger();
    player.getEvents().addListener("activateShield", activations::incrementAndGet);

    assertTrue(input.keyDown(Keys.B));
    assertEquals(1, activations.get());
  }
}
