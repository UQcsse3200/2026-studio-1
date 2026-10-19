package com.csse3200.game.components.gamearea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SubLevelTitleDisplayTest {
  private static final float STAGE_WIDTH = 1280f;
  private static final float TOLERANCE = 1e-4f;

  @Test
  void aShortTitleKeepsTheUsualSize() {
    assertEquals(3.2f, SubLevelTitleDisplay.titleScale(200f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void aLongTitleShrinksToFillMostOfTheScreen() {
    // 512 wide unscaled fits 80% of a 1280 stage at exactly 2x.
    assertEquals(2f, SubLevelTitleDisplay.titleScale(512f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void aTitleWiderThanTheScreenIsScaledBelowItsNaturalSize() {
    assertEquals(0.5f, SubLevelTitleDisplay.titleScale(2048f, STAGE_WIDTH), TOLERANCE);
  }

  @Test
  void transitionDoesNotObscureGameplayForLong() {
    assertTrue(SubLevelTitleDisplay.displaySeconds() < 1f);
    assertEquals(0f, SubLevelTitleDisplay.transitionOpacity(1f), TOLERANCE);
  }

  @Test
  void transitionBackgroundRemainsTransparentEnoughToSeeGameplay() {
    assertTrue(SubLevelTitleDisplay.maximumBackgroundAlpha() <= 0.25f);
    assertTrue(SubLevelTitleDisplay.maximumBlurAlpha() <= 0.3f);
  }

  @Test
  void transitionOpacityIsClampedBeforeAndAfterTheAnimation() {
    assertEquals(0f, SubLevelTitleDisplay.transitionOpacity(-1f), TOLERANCE);
    assertEquals(0f, SubLevelTitleDisplay.transitionOpacity(10f), TOLERANCE);
  }

  @Test
  void aNewDisplayStartsHiddenUntilASubLevelEntryEvent() {
    assertEquals(0f, new SubLevelTitleDisplay(null).currentOpacity(), TOLERANCE);
  }
}
