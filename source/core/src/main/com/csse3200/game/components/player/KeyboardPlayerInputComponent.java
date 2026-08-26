package com.csse3200.game.components.player;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.utils.math.Vector2Utils;

/**
 * Input handler for the player for keyboard and touch (mouse) input. This input handler only uses
 * keyboard input.
 */
public class KeyboardPlayerInputComponent extends InputComponent {
  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private final Vector2 jumpDirection = Vector2.Zero.cpy();
  private final Vector2 dashDirection = Vector2.Zero.cpy();

  public KeyboardPlayerInputComponent() {
    super(5);
  }

  private boolean jumped = false;
  private boolean dashed = false;
  private boolean crouch = false;
  private String direction = "Right";

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyDown(int)
   */
  @Override
  public boolean keyDown(int keycode) {
    switch (keycode) {
      case Keys.W:
        jumpDirection.add(Vector2Utils.UP); // Adds to the y vector
        triggerJumpEvent();
        jumped = true;
        return true;
      case Keys.L:
        if (direction.equals("Left")) {
          dashDirection.add(Vector2Utils.LEFT); // Adds to the x vector to the left
        } else {
          dashDirection.add(Vector2Utils.RIGHT); // Adds to the x vector to the right
        }
        triggerDashEvent();
        dashed = true;
        return true;
      case Keys.A:
        walkDirection.add(Vector2Utils.LEFT);
        direction = "Left";
        triggerWalkEvent();
        return true;
      case Keys.S:
        walkDirection.add(Vector2Utils.DOWN);
        triggerWalkEvent();
        return true;
      case Keys.D:
        walkDirection.add(Vector2Utils.RIGHT);
        direction = "Right";
        triggerWalkEvent();
        return true;
      case Keys.SPACE:
        entity.getEvents().trigger("attack");
        return true;
      case Keys.CONTROL_LEFT:
        entity.getEvents().trigger("ctrlChanged", true);
        return true;
      default:
        return false;
    }
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyUp(int)
   */
  @Override
  public boolean keyUp(int keycode) {
    switch (keycode) {
      // No need for a W case since gravity cancels out the jump
      case Keys.A:
        walkDirection.sub(Vector2Utils.LEFT);
        triggerWalkEvent();
        return true;
      case Keys.S:
        walkDirection.sub(Vector2Utils.DOWN);
        triggerWalkEvent();
        return true;
      case Keys.D:
        walkDirection.sub(Vector2Utils.RIGHT);
        triggerWalkEvent();
        return true;
      case Keys.CONTROL_LEFT:
        entity.getEvents().trigger("ctrlChanged", false);
        return true;
      default:
        return false;
    }
  }

  private void triggerWalkEvent() {
    if (walkDirection.epsilonEquals(Vector2.Zero)) {
      entity.getEvents().trigger("walkStop");
    } else {
      entity.getEvents().trigger("walk", walkDirection);
    }
  }

  private void triggerJumpEvent() {
    // Player has upwards y velocity
    entity.getEvents().trigger("jump", jumpDirection);
    jumpDirection.y = 0;
    jumped = false;
  }

  private void triggerDashEvent() {
    // Player has an x velocity in the direction they last went or are going
    entity.getEvents().trigger("dash", dashDirection);
    dashDirection.x = 0;
    dashed = false;
  }
}
