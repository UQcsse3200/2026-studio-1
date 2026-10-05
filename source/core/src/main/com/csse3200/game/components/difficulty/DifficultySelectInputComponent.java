package com.csse3200.game.components.difficulty;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

public class DifficultySelectInputComponent extends InputComponent {

  public DifficultySelectInputComponent() {
    super(10);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("navigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("navigateDown");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      entity.getEvents().trigger("confirmSelection");
      return true;
    }

    if (keycode == Input.Keys.ESCAPE) {
      entity.getEvents().trigger("back");
      return true;
    }

    return false;
  }
}
