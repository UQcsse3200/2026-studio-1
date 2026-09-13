package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.perks.UpgradesMenuComponent;
import java.util.ArrayList;

/**
 * Debug terminal command to open/close the Upgrades screen for testing,
 * without needing the Pause Menu button wired up yet.
 *
 * Usage in the debug terminal: "upgrades" (toggles open/closed)
 */
public class UpgradesCommand implements Command {
  private final UpgradesMenuComponent upgradesMenu;

  public UpgradesCommand(UpgradesMenuComponent upgradesMenu) {
    this.upgradesMenu = upgradesMenu;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    upgradesMenu.toggleIsOpen();
    return true;
  }
}
