package com.csse3200.game.components.maingame;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/**
 * Keyboard navigation for the death screen's two buttons. Mirrors PauseMenuInputComponent - gates
 * on the sibling Display's visibility instead of a separate "paused" flag, since the death screen
 * has no equivalent state component of its own.
 *
 * <p>Event names are prefixed ("deathNavigateUp" etc.) rather than reusing "navigateUp" /
 * "confirmSelection" verbatim - this component lives on the same shared entity as
 * PauseMenuDisplay/PauseMenuInputComponent in MainGameScreen, and those generic names are already
 * claimed there. Reusing them would fire PauseMenuDisplay's listeners too on every keypress here
 * (e.g. Enter would also trigger PauseMenuActions.resume(), toggling pause state and music even
 * though the pause menu was never opened).
 */
public class DeathScreenInputComponent extends InputComponent {
  private DeathScreenDisplay deathScreenDisplay;

  public DeathScreenInputComponent() {
    super(10);
  }

  @Override
  public void create() {
    super.create();
    deathScreenDisplay = entity.getComponent(DeathScreenDisplay.class);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (deathScreenDisplay == null || !deathScreenDisplay.isVisible()) {
      return false;
    }

    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("deathNavigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("deathNavigateDown");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      entity.getEvents().trigger("deathConfirmSelection");
      return true;
    }

    return false;
  }
}
