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

    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("navigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("navigateDown");
      return true;
    }

    if (keycode == Input.Keys.LEFT) {
      entity.getEvents().trigger("navigateLeft");
      return true;
    }

    if (keycode == Input.Keys.RIGHT) {
      entity.getEvents().trigger("navigateRight");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      entity.getEvents().trigger("confirmSelection");
      return true;
    }

    return false;
  }
}
