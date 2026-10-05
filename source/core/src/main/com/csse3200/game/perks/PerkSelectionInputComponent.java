package com.csse3200.game.perks;

import com.badlogic.gdx.Input;
import com.csse3200.game.input.InputComponent;

/**
 * Keyboard navigation for {@link PerkSelectionDisplay}: Left/Right moves between perk cards,
 * Down/Up moves focus to/from the Continue button, Enter/Space selects whatever's focused.
 *
 * <p>Event names are prefixed ("perkSelectNavigateLeft" etc.) rather than reusing "navigateUp" /
 * "confirmSelection" verbatim - this component lives on the same shared entity as
 * PauseMenuDisplay/DeathScreenDisplay in MainGameScreen, and those generic names (and
 * DeathScreenDisplay's "deathNavigate..." ones) are already claimed there. Reusing them would fire
 * those listeners too on every keypress here, e.g. Enter also triggering PauseMenuActions.resume().
 */
public class PerkSelectionInputComponent extends InputComponent {
  private PerkSelectionDisplay perkSelectionDisplay;

  public PerkSelectionInputComponent() {
    super(10);
  }

  @Override
  public void create() {
    super.create();
    perkSelectionDisplay = entity.getComponent(PerkSelectionDisplay.class);
  }

  @Override
  public boolean keyDown(int keycode) {
    if (perkSelectionDisplay == null || !perkSelectionDisplay.isVisible()) {
      return false;
    }

    if (keycode == Input.Keys.LEFT) {
      entity.getEvents().trigger("perkSelectNavigateLeft");
      return true;
    }

    if (keycode == Input.Keys.RIGHT) {
      entity.getEvents().trigger("perkSelectNavigateRight");
      return true;
    }

    if (keycode == Input.Keys.DOWN) {
      entity.getEvents().trigger("perkSelectNavigateDown");
      return true;
    }

    if (keycode == Input.Keys.UP) {
      entity.getEvents().trigger("perkSelectNavigateUp");
      return true;
    }

    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
      entity.getEvents().trigger("perkSelectConfirm");
      return true;
    }

    return false;
  }
}
