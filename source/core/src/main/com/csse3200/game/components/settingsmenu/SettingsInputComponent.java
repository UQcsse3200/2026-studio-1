package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/**
 * Keyboard input handler for the settings menu. Mirrors MainMenuInputComponent /
 * PauseMenuInputComponent - fires an event on the entity for SettingsMenuDisplay to react to,
 * rather than calling back into it directly.
 */
public class SettingsInputComponent extends InputComponent {

  public SettingsInputComponent() {
    super(10);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode == Input.Keys.ESCAPE) {
      entity.getEvents().trigger("exitSettings");
      return true;
    }

    return false;
  }
}
