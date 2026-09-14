package com.csse3200.game.perks;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/** Keyboard handling for the Perk Tracker overlay: Up/Down to navigate, Escape to close. */
public class PerkTrackerInputComponent extends InputComponent {
  private PerkTrackerMenuComponent perkTrackerMenu;

  public PerkTrackerInputComponent() {
    super(10);
  }

  @Override
  public void create() {
    super.create();
    perkTrackerMenu = entity.getComponent(PerkTrackerMenuComponent.class);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (perkTrackerMenu == null || !perkTrackerMenu.isOpen()) {
      return false;
    }

    if (keycode == Input.Keys.ESCAPE) {
      perkTrackerMenu.close();
      return true;
    }

    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("perkNavigateUp");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("perkNavigateDown");
      return true;
    }

    // While open, swallow every other key so it never leaks through to gameplay/menu input
    // underneath (same reasoning as KeyboardPauseInput).
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    return perkTrackerMenu != null && perkTrackerMenu.isOpen();
  }
}
