package com.csse3200.game.perks;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks whether the Upgrades screen is currently open. Mirrors
 * PauseMenuComponent's isPaused()/toggleIsPaused() pattern - the Upgrades
 * screen is an overlay toggled on/off, not a separate ScreenType.
 */
public class UpgradesMenuComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(UpgradesMenuComponent.class);
  private boolean isOpen = false;

  public boolean isOpen() {
    return isOpen;
  }

  public void toggleIsOpen() {
    isOpen = !isOpen;
    logger.info("Upgrades screen open: {}", isOpen);
  }
}
