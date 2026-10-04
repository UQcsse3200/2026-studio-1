package com.csse3200.game.components.player;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/** Handles direct vertical movement input while the developer-only noclip mode is enabled. */
public class NoclipInputComponent extends InputComponent {
  private final DoubleConsumer setVerticalDirection;
  private boolean enabled;
  private boolean movingUp;
  private boolean movingDown;

  public NoclipInputComponent(DoubleConsumer setVerticalDirection) {
    super(6);
    this.setVerticalDirection = Objects.requireNonNull(setVerticalDirection);
  }

  /** Enables or disables noclip input and clears any movement left over from held keys. */
  public void setNoclipEnabled(boolean enabled) {
    this.enabled = enabled;
    movingUp = false;
    movingDown = false;
    updateVerticalDirection();
  }

  @Override
  public boolean keyDown(int keycode) {
    if (!enabled) {
      return false;
    }

    if (keycode == Input.Keys.W) {
      movingUp = true;
      updateVerticalDirection();
      return true;
    }

    if (keycode == Input.Keys.S) {
      movingDown = true;
      updateVerticalDirection();
      return true;
    }

    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (!enabled) {
      return false;
    }

    if (keycode == Input.Keys.W) {
      movingUp = false;
      updateVerticalDirection();
      return true;
    }

    if (keycode == Input.Keys.S) {
      movingDown = false;
      updateVerticalDirection();
      return true;
    }

    return false;
  }

  private void updateVerticalDirection() {
    double direction = (movingUp ? 1d : 0d) - (movingDown ? 1d : 0d);
    setVerticalDirection.accept(direction);
  }
}
