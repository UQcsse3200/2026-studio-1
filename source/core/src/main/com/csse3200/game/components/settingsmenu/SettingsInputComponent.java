package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.pausemenu.KeybindSettings;

public class SettingsInputComponent extends InputComponent {
  private String capturingAction;

  public SettingsInputComponent() {
    super(10);
  }

  public void startKeyCapture(String action) {
    capturingAction = action;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (capturingAction != null) {
      if (keycode == Input.Keys.ESCAPE) {
        capturingAction = null;
        entity.getEvents().trigger("keybindCaptureCancelled");
        return true;
      }

      KeybindSettings.setKey(capturingAction, keycode);
      capturingAction = null;
      entity.getEvents().trigger("keybindChanged");
      return true;
    }

    if (keycode == Input.Keys.ESCAPE) {
      entity.getEvents().trigger("exitSettings");
      return true;
    }

    return false;
  }
}
