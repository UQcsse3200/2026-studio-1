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

  // Current held state of the movement keys. Recomputing walkDirection from these each event keeps
  // it in sync (idempotent), avoiding the drift that an incremental add/sub accumulator suffers when
  // a key event is missed or repeated.
  private boolean leftHeld = false;
  private boolean rightHeld = false;
  private boolean downHeld = false;

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
        leftHeld = true;
        direction = "Left";
        updateWalkDirection();
        return true;
      case Keys.S:
        downHeld = true;
        updateWalkDirection();
        return true;
      case Keys.D:
        rightHeld = true;
        direction = "Right";
        updateWalkDirection();
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
        leftHeld = false;
        updateWalkDirection();
        return true;
      case Keys.S:
        downHeld = false;
        updateWalkDirection();
        return true;
      case Keys.D:
        rightHeld = false;
        updateWalkDirection();
        return true;
      case Keys.CONTROL_LEFT:
        entity.getEvents().trigger("ctrlChanged", false);
        return true;
      default:
        return false;
    }
  }

  /** Rebuild the walk direction from the currently held keys and trigger the walk/stop event. */
  private void updateWalkDirection() {
    walkDirection.setZero();
    if (rightHeld) {
      walkDirection.add(Vector2Utils.RIGHT);
    }
    if (leftHeld) {
      walkDirection.add(Vector2Utils.LEFT);
    }
    if (downHeld) {
      walkDirection.add(Vector2Utils.DOWN);
    }
    triggerWalkEvent();
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
