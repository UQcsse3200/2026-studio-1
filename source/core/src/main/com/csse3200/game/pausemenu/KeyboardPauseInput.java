package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

public class KeyboardPauseInput extends InputComponent {
  private PauseMenuComponent pauseMenu;

  public KeyboardPauseInput() {
    super(10);
  }

  public KeyboardPauseInput(PauseMenuComponent pauseMenu) {
    this();
    this.pauseMenu = pauseMenu;
  }

  @Override
  public void create() {
    super.create();
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
  }

  @Override
  public boolean keyDown(int keycode) {
    // While waiting for a key to bind to an action, forward the raw key instead of handling it
    // normally. Escape cancels the capture rather than being bound.
    if (pauseMenu.isCapturingKeybind()) {
      if (keycode == Input.Keys.ESCAPE) {
        entity.getEvents().trigger("keybindCaptureCancelled");
      } else {
        entity.getEvents().trigger("keybindCaptured", keycode);
      }
      return true;
    }

    if (keycode == KeybindSettings.getKey("pause")) {
      if (!pauseMenu.isPaused()) {
        pauseMenu.toggleIsPaused();
      } else {
        entity.getEvents().trigger("escapePressed");
      }
      return true;
    }
    if (pauseMenu.isPaused()) {
      switch (keycode) {
        case Input.Keys.UP:
        case Input.Keys.DOWN:
        case Input.Keys.LEFT:
        case Input.Keys.RIGHT:
        case Input.Keys.ENTER:
        case Input.Keys.SPACE:
          return false;
        default:
          return true;
      }
    }
    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (pauseMenu.isCapturingKeybind()) {
      return true;
    }
    if (pauseMenu.isPaused()) {
      switch (keycode) {
        case Input.Keys.UP:
        case Input.Keys.DOWN:
        case Input.Keys.LEFT:
        case Input.Keys.RIGHT:
        case Input.Keys.ENTER:
        case Input.Keys.SPACE:
          return false;
        default:
          return true;
      }
    }
    return false;
  }
}
