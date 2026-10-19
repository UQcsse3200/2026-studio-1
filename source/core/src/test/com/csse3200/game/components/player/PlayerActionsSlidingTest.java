package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerActionsSlidingTest {

  @Test
  void shouldNotConsumeStaminaWhenSlidingWhileStationary() {
    Entity player = new Entity();
    StaminaComponent stamina = new StaminaComponent();
    PlayerActions actions = new PlayerActions();

    player.addComponent(stamina);
    player.addComponent(actions);
    player.create();

    float initialStamina = stamina.getStamina();

    player.getEvents().trigger("slide", true);

    assertEquals(initialStamina, stamina.getStamina(), 0.001f);
  }

  @Test
  void shouldConsumeSlideCostWhenSlidingWhileMoving() {
    Entity player = new Entity();
    StaminaComponent stamina = new StaminaComponent();
    PlayerActions actions = new PlayerActions();
    PhysicsComponent physics = mock(PhysicsComponent.class);
    Body body = mock(Body.class);

    when(physics.getBody()).thenReturn(body);
    when(body.getWorldCenter()).thenReturn(new Vector2(0f, 0f));

    player.addComponent(stamina);
    player.addComponent(physics);
    player.addComponent(actions);
    player.create();

    float initialStamina = stamina.getStamina();

    // Move vertically so the slide does not need keyboard-direction input.
    player.getEvents().trigger("walk", new Vector2(0f, 1f));
    player.getEvents().trigger("slide", true);

    assertEquals(initialStamina - stamina.getSlideCost(), stamina.getStamina(), 0.001f);
  }
}
