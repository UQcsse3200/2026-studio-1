package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Covers {@link UpgradesMenuComponent}'s open/closed toggle. */
class UpgradesMenuComponentTest {
  private UpgradesMenuComponent menu;

  @BeforeEach
  void beforeEach() {
    menu = new UpgradesMenuComponent();
  }

  /** The Upgrades screen starts closed until the player opens it. */
  @Test
  void isClosedByDefault() {
    assertFalse(menu.isOpen());
  }

  /** A single toggle opens the menu. */
  @Test
  void togglingOnceOpensTheMenu() {
    menu.toggleIsOpen();

    assertTrue(menu.isOpen());
  }

  /** Toggling twice closes it again, mirroring PauseMenuComponent's own toggle. */
  @Test
  void togglingTwiceClosesTheMenuAgain() {
    menu.toggleIsOpen();
    menu.toggleIsOpen();

    assertFalse(menu.isOpen());
  }
}
