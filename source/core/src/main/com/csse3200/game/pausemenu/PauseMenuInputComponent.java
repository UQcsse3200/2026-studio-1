package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

public class PauseMenuInputComponent extends InputComponent {
  private PauseMenuComponent pauseMenu;

  public PauseMenuInputComponent() {
    super(10);
  }

  public PauseMenuInputComponent(PauseMenuComponent pauseMenu) {
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
    if (pauseMenu == null || !pauseMenu.isPaused()) {
      return false;
    }

    if (keycode == Input.Keys.UP && !pauseMenu.isCapturingKeybind()) {
      entity.getEvents().trigger("navigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN && !pauseMenu.isCapturingKeybind()) {
      entity.getEvents().trigger("navigateDown");
      return true;
    }

    if (keycode == Input.Keys.LEFT && !pauseMenu.isCapturingKeybind()) {
      entity.getEvents().trigger("navigateLeft");
      entity.getEvents().trigger("leftPressed");
      return true;
    }

    if (keycode == Input.Keys.RIGHT && !pauseMenu.isCapturingKeybind()) {
      entity.getEvents().trigger("navigateRight");
      entity.getEvents().trigger("rightPressed");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      if (!pauseMenu.isCapturingKeybind()) {
        entity.getEvents().trigger("confirmSelection");
        return true;
      } else {
        entity.getEvents().trigger("keybindCaptured", keycode);
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (pauseMenu == null || !pauseMenu.isPaused()) {
      return false;
    }

    if (keycode == Input.Keys.LEFT) {
      entity.getEvents().trigger("leftReleased");
      return true;
    }

    if (keycode == Input.Keys.RIGHT) {
      entity.getEvents().trigger("rightReleased");
      return true;
    }

    return false;
  }
}
