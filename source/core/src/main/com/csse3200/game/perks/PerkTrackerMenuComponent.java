package com.csse3200.game.perks;

import com.csse3200.game.components.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks whether the Perk Tracker screen is currently open. Same shape as
 * UpgradesMenuComponent/PauseMenuComponent - an overlay toggled on/off, not a separate ScreenType.
 */
public class PerkTrackerMenuComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(PerkTrackerMenuComponent.class);
  private boolean isOpen = false;

  public boolean isOpen() {
    return isOpen;
  }

  public void toggleIsOpen() {
    isOpen = !isOpen;
    logger.info("Perk tracker open: {}", isOpen);
  }

  public void close() {
    if (isOpen) {
      isOpen = false;
      logger.info("Perk tracker open: false");
    }
  }
}
