package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

public class MainMenuInputComponent extends InputComponent {

  public MainMenuInputComponent() {
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

    return false;
  }
}
