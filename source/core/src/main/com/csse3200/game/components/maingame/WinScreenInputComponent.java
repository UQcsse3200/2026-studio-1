package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/**
 * Keyboard navigation for the win screen's two buttons. Mirrors PauseMenuInputComponent - gates
 * on the sibling Display's visibility instead of a separate state component.
 *
 * <p>Event names are prefixed ("winNavigateUp" etc.) rather than reusing "navigateUp" /
 * "confirmSelection" verbatim - this component lives on the same shared entity as
 * PauseMenuDisplay/PauseMenuInputComponent in MainGameScreen, and those generic names are
 * already claimed there. See DeathScreenInputComponent for the full reasoning.
 */
public class WinScreenInputComponent extends InputComponent {
  private WinScreenDisplay winScreenDisplay;

  public WinScreenInputComponent() {
    super(10);
  }

  @Override
  public void create() {
    super.create();
    winScreenDisplay = entity.getComponent(WinScreenDisplay.class);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (winScreenDisplay == null || !winScreenDisplay.isVisible()) {
      return false;
    }

    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("winNavigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("winNavigateDown");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      entity.getEvents().trigger("winConfirmSelection");
      return true;
    }

    return false;
  }
}
